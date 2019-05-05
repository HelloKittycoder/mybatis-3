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
package org.apache.ibatis.executor.resultset;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.apache.ibatis.cursor.Cursor;

/**
 * java.sql.ResultSet处理器接口
 * @author Clinton Begin
 */
public interface ResultSetHandler {

  /**
   * 将 {@link java.sql.ResultSet} 处理成映射对应的结果
   * @param stmt Statement对象
   * @param <E> 泛型
   * @return 结果数组
   * @throws SQLException
   */
  <E> List<E> handleResultSets(Statement stmt) throws SQLException;

  /**
   * 将 {@link java.sql.ResultSet} 处理成Cursor对象
   * @param stmt Statement对象
   * @param <E> 泛型
   * @return Cursor对象
   * @throws SQLException
   */
  <E> Cursor<E> handleCursorResultSets(Statement stmt) throws SQLException;

  // 暂时忽略，和存储过程相关
  void handleOutputParameters(CallableStatement cs) throws SQLException;

}
