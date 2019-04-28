package org.apache.ibatis.scripting.xmltags.ognlstudy;

import ognl.Ognl;
import ognl.OgnlContext;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by shucheng on 2019-4-28 下午 21:04
 * Ognl表达式学习
 * 参考链接：https://www.cnblogs.com/renchunxiao/p/3423299.html
 */
public class OgnlStudy {

    public static final String ONE = "one";
    public OgnlContext context = new OgnlContext(null,null,
            new DefaultMemberAccess(true));

    public static void get() {}
    public static String getString() {
        return "string";
    }

    // 根据上下文来获取变量
    @Test
    public void testContext() throws Exception {
        User user = new User("张三", "101");
        Address address = new Address("20100", "上海市浦东新区");
        user.setAddress(address);

        System.out.println("用户名====" + Ognl.getValue("name", context, user));
        System.out.println("用户对应的地址类====" + Ognl.getValue("address", context, user));
        System.out.println("用户所在地的邮编====" + Ognl.getValue("address.port", context, user));
    }

    // 获取静态常量、执行静态方法
    @Test
    public void testStaticRelated() throws Exception {
        Object object = Ognl.getValue("@org.apache.ibatis.scripting.xmltags.ognlstudy.OgnlStudy@ONE",
                context, (Object) null);
        Object object1 = Ognl.getValue("@org.apache.ibatis.scripting.xmltags.ognlstudy.OgnlStudy@get()",
                context, (Object) null);
        Object object2 = Ognl.getValue("@org.apache.ibatis.scripting.xmltags.ognlstudy.OgnlStudy@getString()",
                context, (Object) null);
        System.out.println(object);
        System.out.println(object1);
        System.out.println(object2);
    }

    // 对数组和集合的访问
    @Test
    public void testCollection() throws Exception {
        User user = new User();
        String[] strArr = {"aa", "bb", "cc"};
        List<String> list = new ArrayList<>();
        list.add("11");
        list.add("22");
        list.add("33");
        Map<String, String> map = new HashMap<>();
        map.put("key1", "value1");
        map.put("key2", "value2");
        context.put("list", list);
        context.put("strArr", strArr);
        context.put("map", map);

        System.out.println(Ognl.getValue("#strArr[0]", context, user));
        System.out.println(Ognl.getValue("#list[0]", context, user));
        System.out.println(Ognl.getValue("#list[0 + 1]", context, user));
        System.out.println(Ognl.getValue("#map['key1']", context, user));
        System.out.println(Ognl.getValue("#map['key'+'2']", context, user));
    }

    // 投影与选择
    @Test
    public void testSelect() throws Exception {
        Person p1 = new Person(1, "s1");
        Person p2 = new Person(2, "s2");
        Person p3 = new Person(3, "s3");
        Person p4 = new Person(4, "s4");
        Person p5 = new Person(5, "s5");
        List<Person> list = new ArrayList<>();
        list.add(p1);
        list.add(p2);
        list.add(p3);
        list.add(p4);
        list.add(p5);
        context.put("list", list);

        List<Integer> list1 = (List<Integer>) Ognl.getValue("#list.{id}", context, list);
        List<Integer> list2 = (List<Integer>) Ognl.getValue("#list.{id + '-' + name}",
                context, list);
        // ? 满足条件的所有元素
        List<Person> list3 = (List<Person>) Ognl.getValue("#list.{? #this.id > 2}",
                context, list);
        // ^ 满足条件的第一个元素
        List<Person> list4 = (List<Person>) Ognl.getValue("#list.{^ #this.id > 2}",
                context, list);
        // $ 满足条件的最后一个元素
        List<Person> list5 = (List<Person>) Ognl.getValue("#list.{$ #this.id > 2}",
                context, list);
        System.out.println(list1);
        System.out.println(list2);
        System.out.println(list3);
        System.out.println(list4);
        System.out.println(list5);
    }

    // 创建对象
    @Test
    public void createCreatObject() throws Exception {

        List<String> list = (List<String>) Ognl.getValue("{'aa', 'bb', 'cc'}", context, (Object) null);
        // 构造Map对象
        Map<String, String> map = (Map<String, String>) Ognl.getValue("#{'key1':'value1','key2':'value2'}", context, (Object) null);
        // 构造任意对象
        Object object = Ognl.getValue("new java.lang.Object()", context, (Object) null);
        Person person = (Person) Ognl.getValue("new org.apache.ibatis.scripting.xmltags.ognlstudy.Person(1, '张三')", context, (Object) null);
        System.out.println(list);
        System.out.println(map);
        System.out.println(object);
        System.out.println(person);
    }
}
