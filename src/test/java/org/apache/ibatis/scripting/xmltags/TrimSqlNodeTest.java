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

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Created by shucheng on 2019-4-28 下午 13:29
 */
public class TrimSqlNodeTest {

    @Test
    public void testParseOverrides() throws Exception {
        Method method = TrimSqlNode.class.getDeclaredMethod("parseOverrides", String.class);
        method.setAccessible(true); // 将私有方法设置为可访问的
        List<String> list = (List<String>) method.invoke(null, "aa|bb");
        System.out.println(list);
    }
}
