package org.apache.ibatis.binding.jdkdynamicproxy;

/**
 * Created by shucheng on 2019-4-14 下午 22:42
 */
public class MapperProxyTest {

    public static void main(String[] args) {
        MapperProxy proxy = new MapperProxy();

        UserMapper mapper = proxy.newInstance(UserMapper.class);
        User user = mapper.getUserById(1001);

        System.out.println("ID:" + user.getId());
        System.out.println("Name:" + user.getName());
        System.out.println("Age:" + user.getAge());
        System.out.println(mapper.toString());
    }
}
