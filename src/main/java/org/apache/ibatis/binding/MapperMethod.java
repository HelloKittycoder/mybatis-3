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
package org.apache.ibatis.binding;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.ibatis.annotations.Flush;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.StatementType;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.ParamNameResolver;
import org.apache.ibatis.reflection.TypeParameterResolver;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;

/**
 * @author Clinton Begin
 * @author Eduardo Macarron
 * @author Lasse Voss
 * @author Kazuki Shimizu
 */
public class MapperMethod {

  /**
   * SqlCommand对象
   */
  private final SqlCommand command;
  /**
   * MethodSignature对象
   */
  private final MethodSignature method;

  public MapperMethod(Class<?> mapperInterface, Method method, Configuration config) {
    this.command = new SqlCommand(config, mapperInterface, method);
    this.method = new MethodSignature(config, mapperInterface, method);
  }

  public Object execute(SqlSession sqlSession, Object[] args) {
    Object result;
    switch (command.getType()) {
      case INSERT: {
        // 转换参数
        Object param = method.convertArgsToSqlCommandParam(args);
        // 执行INSERT操作
        // 转换rowCount
        result = rowCountResult(sqlSession.insert(command.getName(), param));
        break;
      }
      case UPDATE: {
        // <1.1> 转换参数
        Object param = method.convertArgsToSqlCommandParam(args);
        // <1.2> 执行更新
        // <1.3> 转换rowCount
        result = rowCountResult(sqlSession.update(command.getName(), param));
        break;
      }
      case DELETE: {
        // 转换参数
        Object param = method.convertArgsToSqlCommandParam(args);
        // 转换rowCount
        result = rowCountResult(sqlSession.delete(command.getName(), param));
        break;
      }
      case SELECT:
        // <2.1> 无返回，并且有ResultHandler方法参数，则将查询的结果，提交给ResultHandler进行处理
        if (method.returnsVoid() && method.hasResultHandler()) {
          executeWithResultHandler(sqlSession, args);
          result = null;
        // <2.2> 执行查询，返回列表
        } else if (method.returnsMany()) {
          result = executeForMany(sqlSession, args);
        // <2.3> 执行查询，返回Map
        } else if (method.returnsMap()) {
          result = executeForMap(sqlSession, args);
        // <2.4> 执行查询，返回Cursor
        } else if (method.returnsCursor()) {
          result = executeForCursor(sqlSession, args);
        // <2.5> 执行查询，返回单个对象
        } else {
          // 转换参数
          Object param = method.convertArgsToSqlCommandParam(args);
          // 查询单条
          result = sqlSession.selectOne(command.getName(), param);
          if (method.returnsOptional()
              && (result == null || !method.getReturnType().equals(result.getClass()))) {
            result = Optional.ofNullable(result);
          }
        }
        break;
      case FLUSH:
        result = sqlSession.flushStatements();
        break;
      default:
        throw new BindingException("Unknown execution method for: " + command.getName());
    }
    // 返回结果为null，并且返回类型为基本类型，则抛出BindingException异常
    if (result == null && method.getReturnType().isPrimitive() && !method.returnsVoid()) {
      throw new BindingException("Mapper method '" + command.getName()
          + " attempted to return null from a method with a primitive return type (" + method.getReturnType() + ").");
    }
    // 返回结果
    return result;
  }

  private Object rowCountResult(int rowCount) {
    final Object result;
    if (method.returnsVoid()) { // Void情况，不用返回
      result = null;
    } else if (Integer.class.equals(method.getReturnType()) || Integer.TYPE.equals(method.getReturnType())) { // Int
      result = rowCount;
    } else if (Long.class.equals(method.getReturnType()) || Long.TYPE.equals(method.getReturnType())) { // Long
      result = (long)rowCount;
    } else if (Boolean.class.equals(method.getReturnType()) || Boolean.TYPE.equals(method.getReturnType())) { // Boolean
      result = rowCount > 0;
    } else {
      throw new BindingException("Mapper method '" + command.getName() + "' has an unsupported return type: " + method.getReturnType());
    }
    return result;
  }

  // 对SqlSession的select方法的封装
  private void executeWithResultHandler(SqlSession sqlSession, Object[] args) {
    // 获得MappedStatement对象
    MappedStatement ms = sqlSession.getConfiguration().getMappedStatement(command.getName());
    if (!StatementType.CALLABLE.equals(ms.getStatementType()) // 校验存储过程的情况。不符合，抛出BindingException异常
        && void.class.equals(ms.getResultMaps().get(0).getType())) {
      throw new BindingException("method " + command.getName()
          + " needs either a @ResultMap annotation, a @ResultType annotation,"
          + " or a resultType attribute in XML so a ResultHandler can be used as a parameter.");
    }
    // 转换参数
    Object param = method.convertArgsToSqlCommandParam(args);
    // 执行SELECT操作
    if (method.hasRowBounds()) {
      RowBounds rowBounds = method.extractRowBounds(args);
      sqlSession.select(command.getName(), param, rowBounds, method.extractResultHandler(args));
    } else {
      sqlSession.select(command.getName(), param, method.extractResultHandler(args));
    }
  }

  // 对SqlSession的selectList方法的封装
  private <E> Object executeForMany(SqlSession sqlSession, Object[] args) {
    List<E> result;
    // 转换参数
    Object param = method.convertArgsToSqlCommandParam(args);
    // 执行SELECT操作
    if (method.hasRowBounds()) {
      RowBounds rowBounds = method.extractRowBounds(args);
      result = sqlSession.selectList(command.getName(), param, rowBounds);
    } else {
      result = sqlSession.selectList(command.getName(), param);
    }
    // issue #510 Collections & arrays support
    // 封装Array或Collection结果
    if (!method.getReturnType().isAssignableFrom(result.getClass())) {
      if (method.getReturnType().isArray()) { // 情况一：Array
        return convertToArray(result);
      } else {
        return convertToDeclaredCollection(sqlSession.getConfiguration(), result); // 情况二：Collection
      }
    }
    // 直接返回的结果
    return result; // 情况三：默认
  }

  private <T> Cursor<T> executeForCursor(SqlSession sqlSession, Object[] args) {
    Cursor<T> result;
    // 转换参数
    Object param = method.convertArgsToSqlCommandParam(args);
    // 执行SELECT操作
    if (method.hasRowBounds()) {
      RowBounds rowBounds = method.extractRowBounds(args);
      result = sqlSession.selectCursor(command.getName(), param, rowBounds);
    } else {
      result = sqlSession.selectCursor(command.getName(), param);
    }
    return result;
  }

  private <E> Object convertToDeclaredCollection(Configuration config, List<E> list) {
    Object collection = config.getObjectFactory().create(method.getReturnType());
    MetaObject metaObject = config.newMetaObject(collection);
    metaObject.addAll(list);
    return collection;
  }

  // 将list转换为对应的数组
  @SuppressWarnings("unchecked")
  private <E> Object convertToArray(List<E> list) {
    Class<?> arrayComponentType = method.getReturnType().getComponentType();
    Object array = Array.newInstance(arrayComponentType, list.size());
    if (arrayComponentType.isPrimitive()) {
      for (int i = 0; i < list.size(); i++) {
        Array.set(array, i, list.get(i));
      }
      return array;
    } else {
      return list.toArray((E[])array);
    }
  }

  // 对SqlSession的selectMap方法的封装
  private <K, V> Map<K, V> executeForMap(SqlSession sqlSession, Object[] args) {
    Map<K, V> result;
    // 转换参数
    Object param = method.convertArgsToSqlCommandParam(args);
    // 执行SELECT操作
    if (method.hasRowBounds()) {
      RowBounds rowBounds = method.extractRowBounds(args);
      result = sqlSession.selectMap(command.getName(), param, method.getMapKey(), rowBounds);
    } else {
      result = sqlSession.selectMap(command.getName(), param, method.getMapKey());
    }
    return result;
  }

  public static class ParamMap<V> extends HashMap<String, V> {

    private static final long serialVersionUID = -2212268410512043556L;

    @Override
    public V get(Object key) {
      if (!super.containsKey(key)) {
        throw new BindingException("Parameter '" + key + "' not found. Available parameters are " + keySet());
      }
      return super.get(key);
    }

  }

  /**
   * SQL命令
   * MapperMethod的内部静态类
   */
  public static class SqlCommand {

    /**
     * {@link MappedStatement#getId()}
     */
    private final String name;
    /**
     * SQL命令类型
     */
    private final SqlCommandType type;

    public SqlCommand(Configuration configuration, Class<?> mapperInterface, Method method) {
      final String methodName = method.getName();
      final Class<?> declaringClass = method.getDeclaringClass();
      // <1> 获得MappedStatement对象
      MappedStatement ms = resolveMappedStatement(mapperInterface, methodName, declaringClass,
          configuration);
      // <2> 找不到MappedStatement
      if (ms == null) {
        // 如果有@Flush注解，则标记为FLUSH类型
        if (method.getAnnotation(Flush.class) != null) {
          name = null;
          type = SqlCommandType.FLUSH;
        // 如果找不到MappedStatement，则抛出BindingException异常
        } else {
          throw new BindingException("Invalid bound statement (not found): "
              + mapperInterface.getName() + "." + methodName);
        }
      // <3> 找到MappedStatement
      } else {
        // 获得name
        name = ms.getId();
        // 获得type
        type = ms.getSqlCommandType();
        // 如果是UNKNOWN类型，则抛出BindingException异常
        if (type == SqlCommandType.UNKNOWN) {
          throw new BindingException("Unknown execution method for: " + name);
        }
      }
    }

    public String getName() {
      return name;
    }

    public SqlCommandType getType() {
      return type;
    }

    // 获得MappedStatement对象
    private MappedStatement resolveMappedStatement(Class<?> mapperInterface, String methodName,
        Class<?> declaringClass, Configuration configuration) {
      // <1> 获得编号
      String statementId = mapperInterface.getName() + "." + methodName;
      // <2> 如果有，则获得MappedStatement对象，并返回
      if (configuration.hasStatement(statementId)) {
        return configuration.getMappedStatement(statementId);
      // <3> 如果没有，并且当前方法就是declaringClass声明的，则说明真的找不到
      } else if (mapperInterface.equals(declaringClass)) {
        return null;
      }
      // <4> 遍历父接口，继续获得MappedStatement对象
      for (Class<?> superInterface : mapperInterface.getInterfaces()) {
        if (declaringClass.isAssignableFrom(superInterface)) {
          MappedStatement ms = resolveMappedStatement(superInterface, methodName,
              declaringClass, configuration);
          if (ms != null) {
            return ms;
          }
        }
      }
      // 真的找不到，返回null
      return null;
    }
  }

  /**
   * 方法签名
   * 是MapperMethod的内部静态类
   */
  public static class MethodSignature {

    /**
     * 返回类型是否为集合
     */
    private final boolean returnsMany;
    /**
     * 返回类型是否为Map
     */
    private final boolean returnsMap;
    /**
     * 返回类型是否为void
     */
    private final boolean returnsVoid;
    /**
     * 返回类型是否为 {@link org.apache.ibatis.cursor.Cursor}
     */
    private final boolean returnsCursor;
    /**
     * 返回类型是否为 {@link java.util.Optional}
     */
    private final boolean returnsOptional;
    /**
     * 返回类型
     */
    private final Class<?> returnType;
    /**
     * 返回方法上的 {@link MapKey#value()}，前提是返回类型为Map
     */
    private final String mapKey;
    /**
     * 获得 {@link ResultHandler} 在方法参数中的位置
     * 如果为null，说明不存在这个类型
     */
    private final Integer resultHandlerIndex;
    /**
     * 获得 {@link RowBounds} 在方法参数中的位置
     * 如果为null，说明不存在这个类型
     */
    private final Integer rowBoundsIndex;
    /**
     * ParamNameResolver对象
     */
    private final ParamNameResolver paramNameResolver;

    public MethodSignature(Configuration configuration, Class<?> mapperInterface, Method method) {
      // 初始化returnType属性
      Type resolvedReturnType = TypeParameterResolver.resolveReturnType(method, mapperInterface);
      if (resolvedReturnType instanceof Class<?>) { // 普通类
        this.returnType = (Class<?>) resolvedReturnType;
      } else if (resolvedReturnType instanceof ParameterizedType) { // 泛型
        this.returnType = (Class<?>) ((ParameterizedType) resolvedReturnType).getRawType();
      } else { // 内部类等等
        this.returnType = method.getReturnType();
      }
      // 初始化returnsVoid属性
      this.returnsVoid = void.class.equals(this.returnType);
      // 初始化returnsMany属性
      this.returnsMany = configuration.getObjectFactory().isCollection(this.returnType) || this.returnType.isArray();
      // 初始化returnsCursor属性
      this.returnsCursor = Cursor.class.equals(this.returnType);
      // 初始化returnsOptional属性
      this.returnsOptional = Optional.class.equals(this.returnType);
      // <1> 初始化mapKey
      this.mapKey = getMapKey(method);
      // 初始化returnsMap
      this.returnsMap = this.mapKey != null;
      // <2> 初始化rowBoundsIndex、resultHandlerIndex
      this.rowBoundsIndex = getUniqueParamIndex(method, RowBounds.class);
      this.resultHandlerIndex = getUniqueParamIndex(method, ResultHandler.class);
      // 初始化paramNameResolver
      this.paramNameResolver = new ParamNameResolver(configuration, method);
    }

    // 获得SQL通用参数
    public Object convertArgsToSqlCommandParam(Object[] args) {
      return paramNameResolver.getNamedParams(args);
    }

    public boolean hasRowBounds() {
      return rowBoundsIndex != null;
    }

    public RowBounds extractRowBounds(Object[] args) {
      return hasRowBounds() ? (RowBounds) args[rowBoundsIndex] : null;
    }

    public boolean hasResultHandler() {
      return resultHandlerIndex != null;
    }

    public ResultHandler extractResultHandler(Object[] args) {
      return hasResultHandler() ? (ResultHandler) args[resultHandlerIndex] : null;
    }

    public String getMapKey() {
      return mapKey;
    }

    public Class<?> getReturnType() {
      return returnType;
    }

    public boolean returnsMany() {
      return returnsMany;
    }

    public boolean returnsMap() {
      return returnsMap;
    }

    public boolean returnsVoid() {
      return returnsVoid;
    }

    public boolean returnsCursor() {
      return returnsCursor;
    }

    /**
     * return whether return type is {@code java.util.Optional}.
     * @return return {@code true}, if return type is {@code java.util.Optional}
     * @since 3.5.0
     */
    public boolean returnsOptional() {
      return returnsOptional;
    }

    // 获得指定参数类型在方法参数中的位置
    private Integer getUniqueParamIndex(Method method, Class<?> paramType) {
      Integer index = null;
      // 遍历方法参数
      final Class<?>[] argTypes = method.getParameterTypes();
      for (int i = 0; i < argTypes.length; i++) {
        if (paramType.isAssignableFrom(argTypes[i])) { // 类型符合
          // 获得第一次的位置
          if (index == null) {
            index = i;
          // 如果类型重复了，则抛出BindingException异常
          } else {
            throw new BindingException(method.getName() + " cannot have multiple " + paramType.getSimpleName() + " parameters");
          }
        }
      }
      return index;
    }

    /**
     * 获取注解的 {@link MapKey#value()}
     * @param method
     * @return
     */
    private String getMapKey(Method method) {
      String mapKey = null;
      // 返回类型为Map
      if (Map.class.isAssignableFrom(method.getReturnType())) {
        // 使用@MapKey注解
        final MapKey mapKeyAnnotation = method.getAnnotation(MapKey.class);
        // 获得@MapKey注解的键
        if (mapKeyAnnotation != null) {
          mapKey = mapKeyAnnotation.value();
        }
      }
      return mapKey;
    }
  }

}
