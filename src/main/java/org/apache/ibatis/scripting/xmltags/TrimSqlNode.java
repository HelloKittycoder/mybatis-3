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
package org.apache.ibatis.scripting.xmltags;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.ibatis.session.Configuration;

/**
 * <trim/>标签的SqlNode实现类（<trim/>标签是<where/>和<set/>标签的基础）
 * @author Clinton Begin
 */
public class TrimSqlNode implements SqlNode {

  /**
   * 内含的SqlNode节点
   */
  private final SqlNode contents;
  /**
   * 前缀
   */
  private final String prefix;
  /**
   * 后缀
   */
  private final String suffix;
  /**
   * 需要被删除的前缀
   */
  private final List<String> prefixesToOverride;
  /**
   * 需要被删除的后缀
   */
  private final List<String> suffixesToOverride;
  private final Configuration configuration;

  public TrimSqlNode(Configuration configuration, SqlNode contents, String prefix, String prefixesToOverride, String suffix, String suffixesToOverride) {
    this(configuration, contents, prefix, parseOverrides(prefixesToOverride), suffix, parseOverrides(suffixesToOverride));
  }

  // <trim/>标签的SqlNode实现类
  protected TrimSqlNode(Configuration configuration, SqlNode contents, String prefix, List<String> prefixesToOverride, String suffix, List<String> suffixesToOverride) {
    this.contents = contents;
    this.prefix = prefix;
    this.prefixesToOverride = prefixesToOverride;
    this.suffix = suffix;
    this.suffixesToOverride = suffixesToOverride;
    this.configuration = configuration;
  }

  /**
   * 测试方法见 org.apache.ibatis.submitted.ognlstatic.OgnlStaticTest#shouldGetAUserStatic
   */
  @Override
  public boolean apply(DynamicContext context) {
    // <1> 创建FilteredDynamicContext对象
    FilteredDynamicContext filteredDynamicContext = new FilteredDynamicContext(context);
    // <2> 执行contents的应用
    boolean result = contents.apply(filteredDynamicContext);
    // <3> 执行FilteredDynamicContext的应用
    filteredDynamicContext.applyAll();
    return result;
  }

  // 测试方法见 org.apache.ibatis.scripting.xmltags.TrimSqlNodeTest#testParseOverrides
  // 使用|分隔符，将字符串分隔成字符串数组，并都转换成大写
  private static List<String> parseOverrides(String overrides) {
    if (overrides != null) {
      final StringTokenizer parser = new StringTokenizer(overrides, "|", false);
      final List<String> list = new ArrayList<>(parser.countTokens());
      while (parser.hasMoreTokens()) {
        list.add(parser.nextToken().toUpperCase(Locale.ENGLISH));
      }
      return list;
    }
    return Collections.emptyList();
  }

  // 支持trim逻辑的DynamicContext实现类
  private class FilteredDynamicContext extends DynamicContext {
    /**
     * 委托的DynamicContext对象
     */
    private DynamicContext delegate;
    /**
     * prefix是否已经被应用
     */
    private boolean prefixApplied;
    /**
     * suffix是否已经被应用
     */
    private boolean suffixApplied;
    /**
     * StringBuilder对象
     *
     * @see #appendSql(String)
     */
    private StringBuilder sqlBuffer;

    public FilteredDynamicContext(DynamicContext delegate) {
      super(configuration, null);
      this.delegate = delegate;
      this.prefixApplied = false;
      this.suffixApplied = false;
      this.sqlBuffer = new StringBuilder();
    }

    // 将sqlBuffer处理完后，添加回delegate.sqlBuffer中
    public void applyAll() {
      // <1> 去掉多余的空格，生成新的sqlBuffer对象
      sqlBuffer = new StringBuilder(sqlBuffer.toString().trim());
      // <2> 将sqlBuffer大写，生成新的trimmedUppercaseSql对象
      String trimmedUppercaseSql = sqlBuffer.toString().toUpperCase(Locale.ENGLISH);
      // <3> 应用TrimSqlNode的trim逻辑
      if (trimmedUppercaseSql.length() > 0) {
        applyPrefix(sqlBuffer, trimmedUppercaseSql);
        applySuffix(sqlBuffer, trimmedUppercaseSql);
      }
      // <4> 将结果添加到delegate中
      delegate.appendSql(sqlBuffer.toString());
    }

    @Override
    public Map<String, Object> getBindings() {
      return delegate.getBindings();
    }

    @Override
    public void bind(String name, Object value) {
      delegate.bind(name, value);
    }

    @Override
    public int getUniqueNumber() {
      return delegate.getUniqueNumber();
    }

    // 该方法，将拼接的sql，暂时存储到sqlBuffer中
    @Override
    public void appendSql(String sql) {
      sqlBuffer.append(sql);
    }

    @Override
    public String getSql() {
      return delegate.getSql();
    }

    private void applyPrefix(StringBuilder sql, String trimmedUppercaseSql) {
      if (!prefixApplied) {
        prefixApplied = true;
        // prefixesToOverride非空，先删除
        if (prefixesToOverride != null) {
          for (String toRemove : prefixesToOverride) {
            if (trimmedUppercaseSql.startsWith(toRemove)) {
              sql.delete(0, toRemove.trim().length());
              break;
            }
          }
        }
        // prefix非空，再添加
        if (prefix != null) {
          sql.insert(0, " ");
          sql.insert(0, prefix);
        }
      }
    }

    private void applySuffix(StringBuilder sql, String trimmedUppercaseSql) {
      if (!suffixApplied) {
        suffixApplied = true;
        // suffixesToOverride非空，先删除
        if (suffixesToOverride != null) {
          for (String toRemove : suffixesToOverride) {
            if (trimmedUppercaseSql.endsWith(toRemove) || trimmedUppercaseSql.endsWith(toRemove.trim())) {
              int start = sql.length() - toRemove.trim().length();
              int end = sql.length();
              sql.delete(start, end);
              break;
            }
          }
        }
        // suffix非空，再添加
        if (suffix != null) {
          sql.append(" ");
          sql.append(suffix);
        }
      }
    }

  }

}
