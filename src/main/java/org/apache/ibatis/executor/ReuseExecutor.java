/**
 *    Copyright 2009-2018 the original author or authors.
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

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.transaction.Transaction;

/**
 * 可重用的Executor实现类
 * 说明：1.每次开始读或写操作，优先从缓存中获取对应的Statement对象。如果不存在，才进行创建
 * 2.执行完成后，不关闭Statement对象
 * 3.其他的，和SimpleExecutor是一致的
 * @author Clinton Begin
 */
public class ReuseExecutor extends BaseExecutor {

  /**
   * Statement的缓存
   *
   * KEY：SQL
   */
  private final Map<String, Statement> statementMap = new HashMap<>();

  public ReuseExecutor(Configuration configuration, Transaction transaction) {
    super(configuration, transaction);
  }

  @Override
  public int doUpdate(MappedStatement ms, Object parameter) throws SQLException {
    Configuration configuration = ms.getConfiguration();
    // 创建StatementHandler对象
    StatementHandler handler = configuration.newStatementHandler(this, ms, parameter, RowBounds.DEFAULT, null, null);
    // 初始化StatementHandler对象
    Statement stmt = prepareStatement(handler, ms.getStatementLog());
    // 执行StatementHandler，进行写操作
    return handler.update(stmt);
  }

  @Override
  public <E> List<E> doQuery(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
    Configuration configuration = ms.getConfiguration();
    // 创建StatementHandler对象
    StatementHandler handler = configuration.newStatementHandler(wrapper, ms, parameter, rowBounds, resultHandler, boundSql);
    // <1> 初始化StatementHandler对象
    Statement stmt = prepareStatement(handler, ms.getStatementLog());
    // 执行StatementHandler，进行读操作
    return handler.query(stmt, resultHandler);
  }

  @Override
  protected <E> Cursor<E> doQueryCursor(MappedStatement ms, Object parameter, RowBounds rowBounds, BoundSql boundSql) throws SQLException {
    Configuration configuration = ms.getConfiguration();
    // 创建StatementHandler对象
    StatementHandler handler = configuration.newStatementHandler(wrapper, ms, parameter, rowBounds, null, boundSql);
    // 初始化StatementHandler对象
    Statement stmt = prepareStatement(handler, ms.getStatementLog());
    // 执行StatementHandler，执行读操作
    return handler.queryCursor(stmt);
  }

  @Override
  public List<BatchResult> doFlushStatements(boolean isRollback) {
    // 关闭缓存的Statement对象
    for (Statement stmt : statementMap.values()) {
      closeStatement(stmt);
    }
    statementMap.clear();
    // 返回空集合
    return Collections.emptyList();
  }

  private Statement prepareStatement(StatementHandler handler, Log statementLog) throws SQLException {
    Statement stmt;
    BoundSql boundSql = handler.getBoundSql();
    String sql = boundSql.getSql();
    // 存在
    if (hasStatementFor(sql)) {
      // <1.1> 从缓存中获得Statement或PrepareStaement对象
      stmt = getStatement(sql);
      // <1.2> 设置事务超时时间
      applyTransactionTimeout(stmt);
    // 不存在
    } else {
      // <2.1> 获得Connection对象
      Connection connection = getConnection(statementLog);
      // <2.2> 创建Statement或PrepareStaement对象
      stmt = handler.prepare(connection, transaction.getTimeout());
      // <2.3> 添加到缓存中
      putStatement(sql, stmt);
    }
    // <2> 设置SQL上的参数，例如PrepareStatement对象上的占位符
    handler.parameterize(stmt);
    return stmt;
  }

  // 判断是否存在对应的Statement对象（并且要求连接未关闭）
  private boolean hasStatementFor(String sql) {
    try {
      return statementMap.keySet().contains(sql) && !statementMap.get(sql).getConnection().isClosed();
    } catch (SQLException e) {
      return false;
    }
  }

  // 获得Statement对象
  private Statement getStatement(String s) {
    return statementMap.get(s);
  }

  private void putStatement(String sql, Statement stmt) {
    statementMap.put(sql, stmt);
  }

}
