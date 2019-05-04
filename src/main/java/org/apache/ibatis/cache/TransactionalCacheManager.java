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
package org.apache.ibatis.cache;

import java.util.HashMap;
import java.util.Map;

import org.apache.ibatis.cache.decorators.TransactionalCache;

/**
 * TransactionalCache管理器
 * @author Clinton Begin
 */
public class TransactionalCacheManager {

  /**
   * Cache和TransactionalCache的映射
   */
  private final Map<Cache, TransactionalCache> transactionalCaches = new HashMap<>();

  // 清空缓存
  public void clear(Cache cache) {
    getTransactionalCache(cache).clear();
  }

  public Object getObject(Cache cache, CacheKey key) {
    return getTransactionalCache(cache).getObject(key);
  }

  // 添加Cache + KV，到缓存中
  public void putObject(Cache cache, CacheKey key, Object value) {
    // 首先，获得Cache对应的TransactionalCache对象
    // 然后，添加KV到TransactionalCache对象中
    getTransactionalCache(cache).putObject(key, value);
  }

  // 提交所有TransactionalCache
  // 通过调用该方法，TransactionalCache存储的当前事务的缓存，会同步到其对应的Cache对象
  public void commit() {
    for (TransactionalCache txCache : transactionalCaches.values()) {
      txCache.commit();
    }
  }

  // 回滚所有TransactionalCache
  public void rollback() {
    for (TransactionalCache txCache : transactionalCaches.values()) {
      txCache.rollback();
    }
  }

  private TransactionalCache getTransactionalCache(Cache cache) {
    return transactionalCaches.computeIfAbsent(cache, TransactionalCache::new);
  }

}
