package org.apache.ibatis.scripting.xmltags.ognlstudy;

import org.junit.jupiter.api.Test;

/**
 * Created by shucheng on 2019-4-28 下午 22:13
 */
public class OgnlCacheTest {

    // 判断创建的对象属性是否为单例
    @Test
    public void testIsSingleton() {
        TestClass t1 = new TestClass();
        TestClass t2 = new TestClass();
        System.out.println(t1.getPerson() == t2.getPerson());
    }
}

class TestClass {
    private static final Person p = new Person(1, "张三");

    public TestClass() {}

    public Person getPerson() {
        return p;
    }
}
