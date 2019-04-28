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
package org.apache.ibatis.scripting.xmltags;

import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

import ognl.OgnlContext;
import ognl.OgnlRuntime;
import ognl.PropertyAccessor;

import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;

/**
 * 动态SQL，用于每次执行SQL操作时，记录动态SQL处理后的最终SQL字符串
 * @author Clinton Begin
 */
public class DynamicContext {

  /**
   * {@link #bindings} _parameter的键，参数
   */
  public static final String PARAMETER_OBJECT_KEY = "_parameter";
  /**
   * {@link #bindings} _databaseId的键，数据库编号
   */
  public static final String DATABASE_ID_KEY = "_databaseId";

  static {
    // <1.2> 设置OGNL的属性访问器
    OgnlRuntime.setPropertyAccessor(ContextMap.class, new ContextAccessor());
  }

  /**
   * 上下文的参数集合
   */
  private final ContextMap bindings;
  /**
   * 生成后的SQL
   */
  private final StringJoiner sqlBuilder = new StringJoiner(" ");
  /**
   * 唯一编号。在{@link ForEachSqlNode}使用
   */
  private int uniqueNumber = 0;

  // 当需要使用到OGNL表达式时，parameterObject非空
  public DynamicContext(Configuration configuration, Object parameterObject) {
    // <1> 初始化bindings参数
    if (parameterObject != null && !(parameterObject instanceof Map)) {
      MetaObject metaObject = configuration.newMetaObject(parameterObject); // <1.1>
      bindings = new ContextMap(metaObject);
    } else {
      bindings = new ContextMap(null);
    }
    // <2> 添加bindings的默认值
    bindings.put(PARAMETER_OBJECT_KEY, parameterObject);
    bindings.put(DATABASE_ID_KEY, configuration.getDatabaseId());
  }

  public Map<String, Object> getBindings() {
    return bindings;
  }

  public void bind(String name, Object value) {
    bindings.put(name, value);
  }

  public void appendSql(String sql) {
    // 该操作会先追加sql，然后追加分隔符" "
    sqlBuilder.add(sql);
  }

  public String getSql() {
    return sqlBuilder.toString().trim();
  }

  // 每次请求，获得新的序号
  public int getUniqueNumber() {
    return uniqueNumber++;
  }

  // 上下文的参数集合
  static class ContextMap extends HashMap<String, Object> {
    private static final long serialVersionUID = 2977601501966151582L;

    /**
     * parameter对应的MetaObject对象
     */
    private MetaObject parameterMetaObject;

    public ContextMap(MetaObject parameterMetaObject) {
      this.parameterMetaObject = parameterMetaObject;
    }

    // ContextMap类在HashMap的基础上，增加对parameterMetaObject属性的访问支持
    @Override
    public Object get(Object key) {
      // 如果有key对应的值，直接获得
      String strKey = (String) key;
      if (super.containsKey(strKey)) {
        return super.get(strKey);
      }

      // 从parameterMetaObject中，获得key对应的属性
      if (parameterMetaObject != null) {
        // issue #61 do not modify the context when reading
        return parameterMetaObject.getValue(strKey);
      }

      return null;
    }
  }

  // 上下文访问器
  static class ContextAccessor implements PropertyAccessor {

    @Override
    public Object getProperty(Map context, Object target, Object name) {
      Map map = (Map) target;

      // 优先从ContextMap中，获得属性
      Object result = map.get(name);
      if (map.containsKey(name) || result != null) {
        return result;
      }

      // <x> 如果没有，则从PARAMETER_OBJECT_KEY对应的Map中，获得属性
      Object parameterObject = map.get(PARAMETER_OBJECT_KEY);
      if (parameterObject instanceof Map) {
        return ((Map)parameterObject).get(name);
      }

      return null;
    }

    @Override
    public void setProperty(Map context, Object target, Object name, Object value) {
      Map<Object, Object> map = (Map<Object, Object>) target;
      map.put(name, value);
    }

    @Override
    public String getSourceAccessor(OgnlContext arg0, Object arg1, Object arg2) {
      return null;
    }

    @Override
    public String getSourceSetter(OgnlContext arg0, Object arg1, Object arg2) {
      return null;
    }
  }
}