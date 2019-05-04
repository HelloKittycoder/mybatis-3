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
package org.apache.ibatis.executor.keygen;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.Map.Entry;

import org.apache.ibatis.binding.MapperMethod.ParamMap;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.ExecutorException;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.reflection.ArrayUtil;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.defaults.DefaultSqlSession.StrictMap;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;

/**
 * 实现KeyGenerator接口，基于Statement#getGeneratedKeys()方法的KeyGenerator实现类，
 * 适用于MySQL、H2主键生成
 * @author Clinton Begin
 * @author Kazuki Shimizu
 */
public class Jdbc3KeyGenerator implements KeyGenerator {

  /**
   * A shared instance.
   *
   * 共享的单例
   *
   * @since 3.4.3
   */
  public static final Jdbc3KeyGenerator INSTANCE = new Jdbc3KeyGenerator();

  // 空实现。因为对于Jdbc3KeyGenerator类的主键，是在SQl执行后，才生成
  @Override
  public void processBefore(Executor executor, MappedStatement ms, Statement stmt, Object parameter) {
    // do nothing
  }

  // 处理返回的自增主键。单个parameter参数，可以认为是批量的一个特例
  @Override
  public void processAfter(Executor executor, MappedStatement ms, Statement stmt, Object parameter) {
    processBatch(ms, stmt, parameter);
  }

  public void processBatch(MappedStatement ms, Statement stmt, Object parameter) {
    // <1> 获得主键属性的配置。如果为空，则直接返回，说明不需要主键
    final String[] keyProperties = ms.getKeyProperties();
    if (keyProperties == null || keyProperties.length == 0) {
      return;
    }
    // <2> 获得返回的自增主键
    try (ResultSet rs = stmt.getGeneratedKeys()) {
      final ResultSetMetaData rsmd = rs.getMetaData();
      final Configuration configuration = ms.getConfiguration();
      if (rsmd.getColumnCount() < keyProperties.length) {
        // Error?
      } else {
        assignKeys(configuration, rs, rsmd, keyProperties, parameter);
      }
    } catch (Exception e) {
      throw new ExecutorException("Error getting generated key or setting result to parameter object. Cause: " + e, e);
    }
  }

  @SuppressWarnings("unchecked")
  private void assignKeys(Configuration configuration, ResultSet rs, ResultSetMetaData rsmd, String[] keyProperties,
      Object parameter) throws SQLException {
    if (parameter instanceof ParamMap || parameter instanceof StrictMap) {
      // Multi-param or single param with @Param
      // 单个或多个带有@Param的参数
      assignKeysToParamMap(configuration, rs, rsmd, keyProperties, (Map<String, ?>) parameter);
    } else if (parameter instanceof ArrayList && !((ArrayList<?>) parameter).isEmpty()
        && ((ArrayList<?>) parameter).get(0) instanceof ParamMap) {
      // Multi-param or single param with @Param in batch operation
      // 批量操作中单个或多个带有@Param的参数
      assignKeysToParamMapList(configuration, rs, rsmd, keyProperties, ((ArrayList<ParamMap<?>>) parameter));
    } else {
      // Single param without @Param
      // 单个不带@Param的参数
      assignKeysToParam(configuration, rs, rsmd, keyProperties, parameter);
    }
  }

  private void assignKeysToParam(Configuration configuration, ResultSet rs, ResultSetMetaData rsmd,
      String[] keyProperties, Object parameter) throws SQLException {
    // 包装成Collection对象
    Collection<?> params = collectionize(parameter);
    if (params.isEmpty()) {
      return;
    }
    List<KeyAssigner> assignerList = new ArrayList<>();
    for (int i = 0; i < keyProperties.length; i++) {
      assignerList.add(new KeyAssigner(configuration, rsmd, i + 1, null, keyProperties[i]));
    }
    // 遍历params数组
    Iterator<?> iterator = params.iterator();
    while (rs.next()) {
      Object param = iterator.next();
      // 挨个给对象的主键赋值
      assignerList.forEach(x -> x.assign(rs, param));
    }
  }

  private void assignKeysToParamMapList(Configuration configuration, ResultSet rs, ResultSetMetaData rsmd,
      String[] keyProperties, ArrayList<ParamMap<?>> paramMapList) throws SQLException {
    Iterator<ParamMap<?>> iterator = paramMapList.iterator();
    List<KeyAssigner> assignerList = new ArrayList<>();
    while (rs.next()) {
      ParamMap<?> paramMap = iterator.next();
      if (assignerList.isEmpty()) {
        for (int i = 0; i < keyProperties.length; i++) {
          assignerList
              .add(getAssignerForParamMap(configuration, rsmd, i + 1, paramMap, keyProperties[i], keyProperties, false)
                  .getValue());
        }
      }
      assignerList.forEach(x -> x.assign(rs, paramMap));
    }
  }

  private void assignKeysToParamMap(Configuration configuration, ResultSet rs, ResultSetMetaData rsmd,
      String[] keyProperties, Map<String, ?> paramMap) throws SQLException {
    if (paramMap.isEmpty()) {
      return;
    }
    Map<String, Entry<Iterator<?>, List<KeyAssigner>>> assignerMap = new HashMap<>();
    for (int i = 0; i < keyProperties.length; i++) {
      Entry<String, KeyAssigner> entry = getAssignerForParamMap(configuration, rsmd, i + 1, paramMap, keyProperties[i],
          keyProperties, true);
      Entry<Iterator<?>, List<KeyAssigner>> iteratorPair = assignerMap.computeIfAbsent(entry.getKey(),
          k -> entry(collectionize(paramMap.get(k)).iterator(), new ArrayList<>()));
      iteratorPair.getValue().add(entry.getValue());
    }
    while (rs.next()) {
      for (Entry<Iterator<?>, List<KeyAssigner>> pair : assignerMap.values()) {
        Object param = pair.getKey().next();
        pair.getValue().forEach(x -> x.assign(rs, param));
      }
    }
  }

  private Entry<String, KeyAssigner> getAssignerForParamMap(Configuration config, ResultSetMetaData rsmd,
      int columnPosition, Map<String, ?> paramMap, String keyProperty, String[] keyProperties, boolean omitParamName) {
    boolean singleParam = paramMap.values().stream().distinct().count() == 1;
    int firstDot = keyProperty.indexOf('.');
    if (firstDot == -1) {
      if (singleParam) {
        return getAssignerForSingleParam(config, rsmd, columnPosition, paramMap, keyProperty, omitParamName);
      }
      throw new ExecutorException("Could not determine which parameter to assign generated keys to. "
          + "Note that when there are multiple parameters, 'keyProperty' must include the parameter name (e.g. 'param.id'). "
          + "Specified key properties are " + ArrayUtil.toString(keyProperties) + " and available parameters are "
          + paramMap.keySet());
    }
    String paramName = keyProperty.substring(0, firstDot);
    if (paramMap.containsKey(paramName)) {
      String argParamName = omitParamName ? null : paramName;
      String argKeyProperty = keyProperty.substring(firstDot + 1);
      return entry(paramName, new KeyAssigner(config, rsmd, columnPosition, argParamName, argKeyProperty));
    } else if (singleParam) {
      return getAssignerForSingleParam(config, rsmd, columnPosition, paramMap, keyProperty, omitParamName);
    } else {
      throw new ExecutorException("Could not find parameter '" + paramName + "'. "
          + "Note that when there are multiple parameters, 'keyProperty' must include the parameter name (e.g. 'param.id'). "
          + "Specified key properties are " + ArrayUtil.toString(keyProperties) + " and available parameters are "
          + paramMap.keySet());
    }
  }

  private Entry<String, KeyAssigner> getAssignerForSingleParam(Configuration config, ResultSetMetaData rsmd,
      int columnPosition, Map<String, ?> paramMap, String keyProperty, boolean omitParamName) {
    // Assume 'keyProperty' to be a property of the single param.
    String singleParamName = nameOfSingleParam(paramMap);
    String argParamName = omitParamName ? null : singleParamName;
    return entry(singleParamName, new KeyAssigner(config, rsmd, columnPosition, argParamName, keyProperty));
  }

  private static String nameOfSingleParam(Map<String, ?> paramMap) {
    // There is virtually one parameter, so any key works.
    return paramMap.keySet().iterator().next();
  }

  private static Collection<?> collectionize(Object param) {
    if (param instanceof Collection) {
      return (Collection<?>) param;
    } else if (param instanceof Object[]) {
      return Arrays.asList((Object[]) param);
    } else {
      return Arrays.asList(param);
    }
  }

  private static <K, V> Entry<K, V> entry(K key, V value) {
    // Replace this with Map.entry(key, value) in Java 9.
    return new AbstractMap.SimpleImmutableEntry<>(key, value);
  }

  private class KeyAssigner {
    private final Configuration configuration;
    private final ResultSetMetaData rsmd;
    private final TypeHandlerRegistry typeHandlerRegistry;
    private final int columnPosition;
    private final String paramName;
    private final String propertyName;
    private TypeHandler<?> typeHandler;

    protected KeyAssigner(Configuration configuration, ResultSetMetaData rsmd, int columnPosition, String paramName,
        String propertyName) {
      super();
      this.configuration = configuration;
      this.rsmd = rsmd;
      this.typeHandlerRegistry = configuration.getTypeHandlerRegistry();
      this.columnPosition = columnPosition;
      this.paramName = paramName;
      this.propertyName = propertyName;
    }

    protected void assign(ResultSet rs, Object param) {
      if (paramName != null) {
        // If paramName is set, param is ParamMap
        param = ((ParamMap<?>) param).get(paramName);
      }
      // 获取参数的MetaObject对象
      MetaObject metaParam = configuration.newMetaObject(param);
      try {
        // 获得对象中属性的typeHandler
        if (typeHandler == null) {
          if (metaParam.hasSetter(propertyName)) {
            Class<?> propertyType = metaParam.getSetterType(propertyName);
            typeHandler = typeHandlerRegistry.getTypeHandler(propertyType,
                JdbcType.forCode(rsmd.getColumnType(columnPosition)));
          } else {
            throw new ExecutorException("No setter found for the keyProperty '" + propertyName + "' in '"
                + metaParam.getOriginalObject().getClass().getName() + "'.");
          }
        }
        if (typeHandler == null) {
          // Error?
        } else {
          // 通过typeHandler获取结果集中指定列的值，得到数据库中主键列的值
          Object value = typeHandler.getResult(rs, columnPosition);
          // 把数据库中的主键列的值赋给java对象的指定属性
          metaParam.setValue(propertyName, value);
        }
      } catch (SQLException e) {
        throw new ExecutorException("Error getting generated key or setting result to parameter object. Cause: " + e,
            e);
      }
    }
  }
}
