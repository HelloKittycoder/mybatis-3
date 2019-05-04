/**
 *    Copyright 2009-2019 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.executor;

import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.transaction.Transaction;

/**
 * @author Jeff Butler
 */
public class BatchExecutor extends BaseExecutor {

  public static final int BATCH_UPDATE_RETURN_VALUE = Integer.MIN_VALUE + 1002;

  /**
   * Statement数组
   */
  private final List<Statement> statementList = new ArrayList<>();
  /**
   * BatchResult数组
   * 每一个BatchResult元素，对应一个 {@link #statementList} 的Statement元素
   */
  private final List<BatchResult> batchResultList = new ArrayList<>();
  /**
   * 当前SQL
   */
  private String currentSql;
  /**
   * 当前MappedStatement对象
   */
  private MappedStatement currentStatement;

  public BatchExecutor(Configuration configuration, Transaction transaction) {
    super(configuration, transaction);
  }

  @Override
  public int doUpdate(MappedStatement ms, Object parameterObject) throws SQLException {
    final Configuration configuration = ms.getConfiguration();
    // <1> 创建StatementHandler对象
    final StatementHandler handler = configuration.newStatementHandler(this, ms, parameterObject, RowBounds.DEFAULT, null, null);
    final BoundSql boundSql = handler.getBoundSql();
    final String sql = boundSql.getSql();
    final Statement stmt;
    // <2> 如果匹配最后一次currentSql和currentStatement，则聚合到BatchResult中
    if (sql.equals(currentSql) && ms.equals(currentStatement)) {
      // <2.1> 获得最后一次的Statement对象
      int last = statementList.size() - 1;
      stmt = statementList.get(last);
      // <2.2> 设置事务超时时间
      applyTransactionTimeout(stmt);
      // <2.3> 设置SQL上的参数，例如PrepareStatement对象上的占位符
      handler.parameterize(stmt);//fix Issues 322
      // <2.4> 获得最后一次的BatchResult对象，并添加参数到其中
      BatchResult batchResult = batchResultList.get(last);
      batchResult.addParameterObject(parameterObject);
    // <3> 如果不匹配最后一次currentSql和currentStatement，则新建BatchResult对象
    } else {
      // <3.1> 获得Connection
      Connection connection = getConnection(ms.getStatementLog());
      // <3.2> 创建Statement或PrepareStatement对象
      stmt = handler.prepare(connection, transaction.getTimeout());
      // <3.3> 设置SQL上的参数，例如PrepareStatement对象上的占位符
      handler.parameterize(stmt);    //fix Issues 322
      // <3.4> 重新设置currentSql和currentStatement
      currentSql = sql;
      currentStatement = ms;
      // <3.5> 添加Statement到statementList中
      statementList.add(stmt);
      // <3.6> 创建BatchResult对象，并添加到batchResultList中
      batchResultList.add(new BatchResult(ms, sql, parameterObject));
    }
    // <4> 批处理
    handler.batch(stmt);
    return BATCH_UPDATE_RETURN_VALUE;
  }

  @Override
  public <E> List<E> doQuery(MappedStatement ms, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql)
      throws SQLException {
    Statement stmt = null;
    try {
      // <1> 刷入批处理语句
      flushStatements();
      Configuration configuration = ms.getConfiguration();
      // 创建StatementHandler对象
      StatementHandler handler = configuration.newStatementHandler(wrapper, ms, parameterObject, rowBounds, resultHandler, boundSql);
      // 获得Connection对象
      Connection connection = getConnection(ms.getStatementLog());
      // 创建Statement或PrepareStatement对象
      stmt = handler.prepare(connection, transaction.getTimeout());
      // 设置SQL上的参数，例如PrepareStatement对象上的占位符
      handler.parameterize(stmt);
      // 执行StatementHandler，进行读操作
      return handler.query(stmt, resultHandler);
    } finally {
      // 关闭StatementHandler对象
      closeStatement(stmt);
    }
  }

  @Override
  protected <E> Cursor<E> doQueryCursor(MappedStatement ms, Object parameter, RowBounds rowBounds, BoundSql boundSql) throws SQLException {
    // <1> 刷入批处理语句
    flushStatements();
    Configuration configuration = ms.getConfiguration();
    // 创建StatementHandler对象
    StatementHandler handler = configuration.newStatementHandler(wrapper, ms, parameter, rowBounds, null, boundSql);
    // 获得Connection对象
    Connection connection = getConnection(ms.getStatementLog());
    // 创建Statement或PrepareStatement对象
    Statement stmt = handler.prepare(connection, transaction.getTimeout());
    // 设置Statement，如果执行完成，则进行自动关闭
    stmt.closeOnCompletion();
    // 设置SQL上的参数，例如PrepareStatement对象上的占位符
    handler.parameterize(stmt);
    // 执行StatementHandler，进行读操作
    return handler.queryCursor(stmt);
  }

  @Override
  public List<BatchResult> doFlushStatements(boolean isRollback) throws SQLException {
    try {
      List<BatchResult> results = new ArrayList<>();
      // <1> 如果isRollback为true，返回空数组
      if (isRollback) {
        return Collections.emptyList();
      }
      // <2> 遍历statementList和batchResultList数组，逐个提交批处理
      for (int i = 0, n = statementList.size(); i < n; i++) {
        // <2.1> 获得Statement和BatchResult对象
        Statement stmt = statementList.get(i);
        applyTransactionTimeout(stmt);
        BatchResult batchResult = batchResultList.get(i);
        try {
          // <2.2> 批量执行
          batchResult.setUpdateCounts(stmt.executeBatch());
          // <2.3> 处理主键生成
          MappedStatement ms = batchResult.getMappedStatement();
          List<Object> parameterObjects = batchResult.getParameterObjects();
          KeyGenerator keyGenerator = ms.getKeyGenerator();
          if (Jdbc3KeyGenerator.class.equals(keyGenerator.getClass())) {
            Jdbc3KeyGenerator jdbc3KeyGenerator = (Jdbc3KeyGenerator) keyGenerator;
            jdbc3KeyGenerator.processBatch(ms, stmt, parameterObjects);
          } else if (!NoKeyGenerator.class.equals(keyGenerator.getClass())) { //issue #141
            for (Object parameter : parameterObjects) {
              keyGenerator.processAfter(this, ms, stmt, parameter);
            }
          }
          // Close statement to close cursor #1109
          // <2.4> 关闭Statement对象
          closeStatement(stmt);
        } catch (BatchUpdateException e) {
          // 如果发生异常，则抛出BatchUpdateException异常
          StringBuilder message = new StringBuilder();
          message.append(batchResult.getMappedStatement().getId())
              .append(" (batch index #")
              .append(i + 1)
              .append(")")
              .append(" failed.");
          if (i > 0) {
            message.append(" ")
                .append(i)
                .append(" prior sub executor(s) completed successfully, but will be rolled back.");
          }
          throw new BatchExecutorException(message.toString(), e, results, batchResult);
        }
        // <2.5> 添加到结果集
        results.add(batchResult);
      }
      return results;
    } finally {
      // <3.1> 关闭Statement
      for (Statement stmt : statementList) {
        closeStatement(stmt);
      }
      // <3.2> 置空currentSql、statementList、batchResultList属性
      currentSql = null;
      statementList.clear();
      batchResultList.clear();
    }
  }

}
