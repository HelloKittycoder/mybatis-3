/**
 *    Copyright 2009-2015 the original author or authors.
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
package org.apache.ibatis.exceptions;

/**
 * @author Clinton Begin
 *
 * reflection 包：ReflectionException
 * logging 包：LogException
 * builder 包：BuilderException、IncompleteElementException
 * scripting 包：ScriptingException
 * binding 包：BindingException
 * type 包：TypeException
 * session 包：SqlSessionException
 * cache 包：CacheException
 * transaction 包：TransactionException
 * datasource 包：DataSourceException
 * executor 包：ResultMapException、ExecutorException、BatchExecutorException
 * plugin 包：PluginException
 */
@SuppressWarnings("deprecation")
public class PersistenceException extends IbatisException {

  private static final long serialVersionUID = -7537395265357977271L;

  public PersistenceException() {
    super();
  }

  public PersistenceException(String message) {
    super(message);
  }

  public PersistenceException(String message, Throwable cause) {
    super(message, cause);
  }

  public PersistenceException(Throwable cause) {
    super(cause);
  }
}
