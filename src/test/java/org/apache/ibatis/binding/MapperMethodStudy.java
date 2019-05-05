package org.apache.ibatis.binding;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.FieldSetter;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;

/**
 * Created by shucheng on 2019-5-5 下午 20:34
 * 测试MapperMethod中的方法
 */
public class MapperMethodStudy {

    // 将list转换为数组
    @Test
    public void convertToArray() throws Exception {

        List<String> list = new ArrayList<String>(){{
            add("aa");
            add("bb");
            add("cc");
        }};
        // 模拟对象行为
        // https://yanbin.blog/mockito-modify-private-field/
        MapperMethod mockMapperMethod = mock(MapperMethod.class);
        Field apiField = MapperMethod.class.getDeclaredField("method");
        MapperMethod.MethodSignature mockMethodSignature = mock(MapperMethod.MethodSignature.class);
        FieldSetter.setField(mockMapperMethod, apiField, mockMethodSignature);

        /* 这种写法有问题
        参考：https://stackoverflow.com/questions/15942880/mocking-a-method-that-return-generics-with-wildcard-using-mockito
        when(mockMethodSignature.getReturnType().getComponentType())
                .thenReturn(new String[]{}.getClass());*/

        Mockito.<Class<?>>when(mockMethodSignature.getReturnType()).thenReturn(new String[]{}.getClass());

        // 获取convertToArray方法
        Method method = MapperMethod.class.getDeclaredMethod("convertToArray", List.class);
        method.setAccessible(true);
        Object result = method.invoke(mockMapperMethod, list);
        System.out.println(result);
    }

    @Test
    public void convertToArray1() throws Exception {

        List<Integer> list = new ArrayList<Integer>(){{
            add(11);
            add(22);
            add(33);
        }};
        // 模拟对象行为
        // https://yanbin.blog/mockito-modify-private-field/
        MapperMethod mockMapperMethod = mock(MapperMethod.class);
        Field apiField = MapperMethod.class.getDeclaredField("method");
        MapperMethod.MethodSignature mockMethodSignature = mock(MapperMethod.MethodSignature.class);
        FieldSetter.setField(mockMapperMethod, apiField, mockMethodSignature);

        /* 这种写法有问题
        参考：https://stackoverflow.com/questions/15942880/mocking-a-method-that-return-generics-with-wildcard-using-mockito
        when(mockMethodSignature.getReturnType().getComponentType())
                .thenReturn(new String[]{}.getClass());*/

        Mockito.<Class<?>>when(mockMethodSignature.getReturnType()).thenReturn(new int[]{}.getClass());

        // 获取convertToArray方法
        Method method = MapperMethod.class.getDeclaredMethod("convertToArray", List.class);
        method.setAccessible(true);
        Object result = method.invoke(mockMapperMethod, list);
        System.out.println(result);
    }

    // 获取ComponentType（数组中存放的对象的类型）
    // https://zhidao.baidu.com/question/196068518.html
    @Test
    public void testComponentType() {
        String[] aa = new String[]{};
        Class<?> returnType = aa.getClass();
        // System.out.println(new String[]{}.getClass() == returnType);
        Class<?> componentType = returnType.getComponentType();
        System.out.println(componentType);
    }
}
