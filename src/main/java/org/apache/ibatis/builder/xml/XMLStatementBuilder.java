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
package org.apache.ibatis.builder.xml;

import java.util.List;
import java.util.Locale;

import org.apache.ibatis.builder.BaseBuilder;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.executor.keygen.SelectKeyGenerator;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultSetType;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.mapping.StatementType;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.session.Configuration;

/**
 * @author Clinton Begin
 */
public class XMLStatementBuilder extends BaseBuilder {

  private final MapperBuilderAssistant builderAssistant;
  /**
   * 当前XML系欸但，例如：<select/>、<insert/>、<update/>、<delete/>标签
   */
  private final XNode context;
  /**
   * 要求的databaseId
   */
  private final String requiredDatabaseId;

  public XMLStatementBuilder(Configuration configuration, MapperBuilderAssistant builderAssistant, XNode context) {
    this(configuration, builderAssistant, context, null);
  }

  public XMLStatementBuilder(Configuration configuration, MapperBuilderAssistant builderAssistant, XNode context, String databaseId) {
    super(configuration);
    this.builderAssistant = builderAssistant;
    this.context = context;
    this.requiredDatabaseId = databaseId;
  }

  // 执行Statement解析
  public void parseStatementNode() {
    // <1> 获得id属性，编号
    String id = context.getStringAttribute("id");
    // <2> 获得databaseId，判断databaseId是否匹配
    String databaseId = context.getStringAttribute("databaseId");

    if (!databaseIdMatchesCurrent(id, databaseId, this.requiredDatabaseId)) {
      return;
    }

    // <8> 获得SQL对应的SqlCommandType枚举值
    String nodeName = context.getNode().getNodeName();
    SqlCommandType sqlCommandType = SqlCommandType.valueOf(nodeName.toUpperCase(Locale.ENGLISH));
    // <9> 获得各种属性
    boolean isSelect = sqlCommandType == SqlCommandType.SELECT;
    boolean flushCache = context.getBooleanAttribute("flushCache", !isSelect);
    boolean useCache = context.getBooleanAttribute("useCache", isSelect);
    boolean resultOrdered = context.getBooleanAttribute("resultOrdered", false);

    // Include Fragments before parsing
    // <10> 创建XMLIncludeTransformer对象，并替换<include/>标签相关的内容
    XMLIncludeTransformer includeParser = new XMLIncludeTransformer(configuration, builderAssistant);
    includeParser.applyIncludes(context.getNode());

    String parameterType = context.getStringAttribute("parameterType");
    Class<?> parameterTypeClass = resolveClass(parameterType);

    String lang = context.getStringAttribute("lang");
    // <4> 获得lang对应的LanguageDriver对象
    LanguageDriver langDriver = getLanguageDriver(lang);

    // Parse selectKey after includes and remove them.
    // <11> 解析<selectKey/>标签
    processSelectKeyNodes(id, parameterTypeClass, langDriver);

    // Parse the SQL (pre: <selectKey> and <include> were parsed and removed)
    // <12> 获得KeyGenerator对象
    KeyGenerator keyGenerator;
    // <13.1> 优先，从configuration中获得KeyGenerator对象。如果存在，意味着是<selectKey/>标签配置的
    String keyStatementId = id + SelectKeyGenerator.SELECT_KEY_SUFFIX;
    keyStatementId = builderAssistant.applyCurrentNamespace(keyStatementId, true);
    if (configuration.hasKeyGenerator(keyStatementId)) {
      keyGenerator = configuration.getKeyGenerator(keyStatementId);
    // <13.2> 其次，根据标签属性的情况，判断是否使用对应的Jdbc3KeyGenerator或者NoKeyGenerator对象
    } else {
      keyGenerator = context.getBooleanAttribute("useGeneratedKeys", // 优先，基于useGeneratedKeys
          configuration.isUseGeneratedKeys() && SqlCommandType.INSERT.equals(sqlCommandType)) // 其次，基于全局的useGeneratedKeys配置+是否为插入语句类型
          ? Jdbc3KeyGenerator.INSTANCE : NoKeyGenerator.INSTANCE;
    }

    // <12> 创建SqlSource
    SqlSource sqlSource = langDriver.createSqlSource(configuration, context, parameterTypeClass);
    // <7> 获得statementType对应的枚举值
    StatementType statementType = StatementType.valueOf(context.getStringAttribute("statementType", StatementType.PREPARED.toString()));
    // <3> 获得各种属性
    Integer fetchSize = context.getIntAttribute("fetchSize");
    Integer timeout = context.getIntAttribute("timeout");
    String parameterMap = context.getStringAttribute("parameterMap");
    String resultType = context.getStringAttribute("resultType");
    // <5> 获得resultType对应的类
    Class<?> resultTypeClass = resolveClass(resultType);
    String resultMap = context.getStringAttribute("resultMap");
    // <6> 获得resultSet对应的枚举值
    String resultSetType = context.getStringAttribute("resultSetType");
    ResultSetType resultSetTypeEnum = resolveResultSetType(resultSetType);
    String keyProperty = context.getStringAttribute("keyProperty");
    String keyColumn = context.getStringAttribute("keyColumn");
    String resultSets = context.getStringAttribute("resultSets");

    // 创建MappedStatement对象
    builderAssistant.addMappedStatement(id, sqlSource, statementType, sqlCommandType,
        fetchSize, timeout, parameterMap, parameterTypeClass, resultMap, resultTypeClass,
        resultSetTypeEnum, flushCache, useCache, resultOrdered,
        keyGenerator, keyProperty, keyColumn, databaseId, langDriver, resultSets);
  }

  private void processSelectKeyNodes(String id, Class<?> parameterTypeClass, LanguageDriver langDriver) {
    // <1> 获得<selectKey/>节点
    List<XNode> selectKeyNodes = context.evalNodes("selectKey");
    // <2> 执行解析<selectKey/>节点
    if (configuration.getDatabaseId() != null) {
      parseSelectKeyNodes(id, selectKeyNodes, parameterTypeClass, langDriver, configuration.getDatabaseId());
    }
    parseSelectKeyNodes(id, selectKeyNodes, parameterTypeClass, langDriver, null);
    // <3> 移除<selectKey/>节点
    removeSelectKeyNodes(selectKeyNodes);
  }

  // 解析<selectKey/>子节点
  private void parseSelectKeyNodes(String parentId, List<XNode> list, Class<?> parameterTypeClass, LanguageDriver langDriver, String skRequiredDatabaseId) {
    // <1> 遍历<selectKey/>节点
    for (XNode nodeToHandle : list) {
      // <2> 获得完整id，格式为`${id}!selectKey`
      String id = parentId + SelectKeyGenerator.SELECT_KEY_SUFFIX;
      // <3> 获得databaseId，判断dadtabaseId是否匹配
      String databaseId = nodeToHandle.getStringAttribute("databaseId");
      if (databaseIdMatchesCurrent(id, databaseId, skRequiredDatabaseId)) {
        // <4> 执行解析单个<selectKey/>节点
        parseSelectKeyNode(id, nodeToHandle, parameterTypeClass, langDriver, databaseId);
      }
    }
  }

  // 执行解析单个<selectKey/>节点
  private void parseSelectKeyNode(String id, XNode nodeToHandle, Class<?> parameterTypeClass, LanguageDriver langDriver, String databaseId) {
    // <1.1> 获得各种属性和对应的类
    String resultType = nodeToHandle.getStringAttribute("resultType");
    Class<?> resultTypeClass = resolveClass(resultType);
    StatementType statementType = StatementType.valueOf(nodeToHandle.getStringAttribute("statementType", StatementType.PREPARED.toString()));
    String keyProperty = nodeToHandle.getStringAttribute("keyProperty");
    String keyColumn = nodeToHandle.getStringAttribute("keyColumn");
    boolean executeBefore = "BEFORE".equals(nodeToHandle.getStringAttribute("order", "AFTER"));

    //defaults
    // <1.2> 创建MappedStatement需要用到的默认值
    boolean useCache = false;
    boolean resultOrdered = false;
    KeyGenerator keyGenerator = NoKeyGenerator.INSTANCE;
    Integer fetchSize = null;
    Integer timeout = null;
    boolean flushCache = false;
    String parameterMap = null;
    String resultMap = null;
    ResultSetType resultSetTypeEnum = null;

    // <1.3> 创建SqlSource对象
    SqlSource sqlSource = langDriver.createSqlSource(configuration, nodeToHandle, parameterTypeClass);
    SqlCommandType sqlCommandType = SqlCommandType.SELECT;

    // <1.4> 创建MappedStatement对象
    builderAssistant.addMappedStatement(id, sqlSource, statementType, sqlCommandType,
        fetchSize, timeout, parameterMap, parameterTypeClass, resultMap, resultTypeClass,
        resultSetTypeEnum, flushCache, useCache, resultOrdered,
        keyGenerator, keyProperty, keyColumn, databaseId, langDriver, null);

    // <2.1> 获得SelectKeyGenerator的编号，格式为`${namespace}.${id}`
    id = builderAssistant.applyCurrentNamespace(id, false);

    // <2.2> 获得MappedStatement对象
    MappedStatement keyStatement = configuration.getMappedStatement(id, false);
    // <2.3> 创建SelectKeyGenerator对象，并添加到configuration中
    configuration.addKeyGenerator(id, new SelectKeyGenerator(keyStatement, executeBefore));
  }

  // 移除<selectKey/>节点
  private void removeSelectKeyNodes(List<XNode> selectKeyNodes) {
    for (XNode nodeToHandle : selectKeyNodes) {
      nodeToHandle.getParent().getNode().removeChild(nodeToHandle.getNode());
    }
  }

  private boolean databaseIdMatchesCurrent(String id, String databaseId, String requiredDatabaseId) {
    // 如果不匹配，则返回false
    if (requiredDatabaseId != null) {
      if (!requiredDatabaseId.equals(databaseId)) {
        return false;
      }
      // 上面这块可以简写为 return requiredDatabaseId.equals(databaseId);
    } else {
      // 如果未设置requiredDatabaseId，但是databaseId存在，说明还是不匹配，则返回false
      if (databaseId != null) {
        return false;
      }
      // skip this statement if there is a previous one with a not null databaseId
      // 判断是否已经存在
      id = builderAssistant.applyCurrentNamespace(id, false);
      if (this.configuration.hasStatement(id, false)) {
        MappedStatement previous = this.configuration.getMappedStatement(id, false); // issue #2
        // 若存在，则判断原有的sqlFragment的databaseId是否为空。因为，当前databaseId为空，这样两者才能匹配
        // 下面这块可以简写为 return previous.getDatabaseId() == null;
        if (previous.getDatabaseId() != null) {
          return false;
        }
      }
    }
    return true;
  }

  // 获得lang对应的LanguageDriver对象
  private LanguageDriver getLanguageDriver(String lang) {
    // 解析lang对应的类
    Class<? extends LanguageDriver> langClass = null;
    if (lang != null) {
      langClass = resolveClass(lang);
    }
    // 获得LanguageDriver对象
    return configuration.getLanguageDriver(langClass);
  }

}
