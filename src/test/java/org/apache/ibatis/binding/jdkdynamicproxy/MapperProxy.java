package org.apache.ibatis.binding.jdkdynamicproxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Created by shucheng on 2019-4-14 下午 22:35
 * 简单模拟mybatis用到的jdk动态代理
 * https://my.oschina.net/zudajun/blog/666223
 * https://www.cnblogs.com/dreamroute/p/5273888.html
 */
public class MapperProxy implements InvocationHandler {

    public <T> T newInstance(Class<T> clz) {
        return (T) Proxy.newProxyInstance(clz.getClassLoader(), new Class[]{ clz }, this);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (Object.class.equals(method.getDeclaringClass())) {
            try {
                // 诸如hashCode()、toString()、equals()等方法，将target指向当前对象this
                return method.invoke(this, args);
            } catch (Throwable t) {
            }
        }
        return new User((Integer) args[0], "zhangsan", 18);
    }
}
