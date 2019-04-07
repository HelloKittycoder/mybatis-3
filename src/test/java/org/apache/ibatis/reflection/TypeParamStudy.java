package org.apache.ibatis.reflection;

import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.lang.reflect.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Create by Administrator on 2019/4/7
 * 学习java.lang.reflect.Type类，主要为了看懂TypeParameterResolver
 * 参考链接：https://www.jianshu.com/p/e8eeff12c306
 */
public class TypeParamStudy {

    /**
     * {@link ParameterizedType} 表示参数化类型，也就是泛型，例如List<T>、Set<T>等
     *
     * 举例：List<String>
     * rawType为List（在哪个类上定义泛型）
     * actualTypeArguments为String（实际传入的泛型参数是什么）
     * ownerType为null（List类是属于哪个类的内部类，而List是单独的一个类，不是作为哪个类的内部类，所以为null）
     * @throws NoSuchFieldException
     */
    @Test
    public void testParameterizedType() throws NoSuchFieldException {
        Field fieldList = ParameterizedTypeTest.class.getDeclaredField("list");
        // 获取该属性的泛型类型
        Type typeList = fieldList.getGenericType();
        // 获取实际的Type类型：sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl
        System.out.println(typeList.getClass().getName());

        Field fieldSet = ParameterizedTypeTest.class.getDeclaredField("set");
        Type typeSet = fieldSet.getGenericType();
        System.out.println(typeSet.getClass().getName());
    }

    @Test
    public void testGetActualTypeArguments() throws NoSuchFieldException {
        Field fieldMap = ParameterizedTypeTest.class.getDeclaredField("map");
        Type typeMap = fieldMap.getGenericType();
        ParameterizedType parameterizedTypeMap = (ParameterizedType) typeMap;
        // 获取泛型中的实际类型
        Type[] types = parameterizedTypeMap.getActualTypeArguments();
        System.out.println(types[0]);
        System.out.println(types[1]);
    }

    @Test
    public void testGetRawType() throws NoSuchFieldException {
        Field fieldMap = ParameterizedTypeTest.class.getDeclaredField("map");
        Type typeMap = fieldMap.getGenericType();
        ParameterizedType parameterizedTypeMap = (ParameterizedType) typeMap;
        // 获取声明泛型的类/接口
        Type type = parameterizedTypeMap.getRawType();
        System.out.println(type);
    }

    @Test
    public void testGetOwnerType() throws NoSuchFieldException {
        Field fieldMapEntry = ParameterizedTypeTest.class.getDeclaredField("mapEntry");
        Type typeMapEntry = fieldMapEntry.getGenericType();
        ParameterizedType parameterizedTypeMapEntry = (ParameterizedType) typeMapEntry;
        // 获取泛型的拥有者
        Type type = parameterizedTypeMapEntry.getOwnerType();
        System.out.println(type);
    }

    /**
     * {@link GenericArrayType} 泛型数组类型，例如List<String>[]、T[]等
     * @throws NoSuchFieldException
     */
    @Test
    public void testGenericArrayType() throws NoSuchFieldException {
        Field fieldListArray = GenericArrayTypeTest.class.getDeclaredField("listArray");
        Type typeListArray = fieldListArray.getGenericType();
        // 获取实际Type类型：sun.reflect.generics.reflectiveObjects.GenericArrayTypeImpl
        System.out.println(typeListArray.getClass().getName());

        Field fieldT = GenericArrayTypeTest.class.getDeclaredField("t");
        Type typeT = fieldT.getGenericType();
        System.out.println(typeT.getClass().getName());
    }

    // 返回泛型数组元素中的Type类型
    // 即List<String>[]中的List<String>（ParameterizedTypeImpl），T[]中的T（TypeVariableImpl）
    @Test
    public void testGetGenericComponentType() throws NoSuchFieldException {
        Field fieldListArray = GenericArrayTypeTest.class.getDeclaredField("listArray");
        Type typeListArray = fieldListArray.getGenericType();
        GenericArrayType genericArrayType1 = (GenericArrayType) typeListArray;
        // 获取泛型数组的Type对象
        // List<String>的原始类型为泛型，所以type最终的类型为ParameterizedTypeImpl
        Type type1 = genericArrayType1.getGenericComponentType();
        System.out.println(type1);

        Field fieldT = GenericArrayTypeTest.class.getDeclaredField("t");
        Type typeT = fieldT.getGenericType();
        GenericArrayType genericArrayType2 = (GenericArrayType) typeT;
        Type type2 = genericArrayType2.getGenericComponentType();
        System.out.println(type2);
    }

    /**
     * {@link TypeVariable} 泛型的类型变量，指的是List<T>、Map<K,V>中的T、K、V等值，
     * 实际的Java类型是TypeVariableImpl（TypeVariable的子类）；此外，还可以对类型变量加上extend限定，
     * 这样会有类型变量对应的上限
     * @throws NoSuchFieldException
     */
    /**
     * 补充：Java-Type体系中的一个重要接口，GenericDeclaration
     * 含义为声明类型变量的所有实体的公共接口，也就是说该接口定义了哪些地方可以定义类型变量（泛型）
     * 查看源码发现，GenericDeclaration下有三个子类（Class、Method、Constructor），
     * 也就是说，我们定义泛型只能在一个类中这3个地方自定义泛型
     */
    @Test
    public void testTypeVariable() throws NoSuchFieldException {
        Field fieldList = TypeVariableTest.class.getDeclaredField("list");
        // 获取该属性的实际类型
        Type typeList = fieldList.getGenericType();
        ParameterizedType parameterizedTypeList = (ParameterizedType) typeList;
        // 获取泛型中的实际类型
        Type[] type = parameterizedTypeList.getActualTypeArguments();
        // 类型变量：sun.reflect.generics.reflectiveObjects.TypeVariableImpl
        System.out.println(type[0].getClass().getName());
    }

    // 获得该类型变量的上限，也就是泛型中extend右边的值
    // 例如：List<T extends Number>，Number就是类型变量T的上限
    // 如果我们只是简单的声明了List<T>（无显式定义extends），那么默认为Object
    @Test
    public void testGetBounds() throws NoSuchFieldException {
        Field fieldT = TypeVariableTest1.class.getDeclaredField("t");
        TypeVariable typeVariable = (TypeVariable) fieldT.getGenericType();
        // 获取类型变量t的上边界
        Type[] types = typeVariable.getBounds();
        for (Type type : types) {
            System.out.println(type);
        }
    }

    // 获取声明该类型变量实体，也就是TypeVariableTest<T>中的TypeVariableTest
    @Test
    public void testGetGenericDeclaration() throws NoSuchFieldException {
        Field fieldT = TypeVariableTest2.class.getDeclaredField("t");
        TypeVariable typeVariable = (TypeVariable) fieldT.getGenericType();
        // 获取声明该类型变量（T）的实体
        GenericDeclaration genericDeclaration = typeVariable.getGenericDeclaration();
        // class org.apache.ibatis.reflection.TypeVariableTest2
        System.out.println(genericDeclaration);
    }

    // 获取类型变量在源码中定义的名称
    @Test
    public void testGetName() throws NoSuchFieldException {
        Field fieldT = TypeVariableTest2.class.getDeclaredField("t");
        TypeVariable typeVariable = (TypeVariable) fieldT.getGenericType();
        // 获取类型变量在源码中定义的名称
        String name = typeVariable.getName();
        System.out.println(name);
    }

    /**
     * {@link Class} Type接口的实现类，是我们工作中常用到的一个对象；在java中，每个class文件在程序运行期间，
     * 都对应着一个Class对象，这个对象保存有这个类的全部信息；因此，Class对象也被称为Java反射的基础
     * @throws NoSuchFieldException
     */
    @Test
    public void testClass() throws NoSuchFieldException {
        Field field = ClassTest.class.getDeclaredField("classTest");
        Type type = field.getGenericType();
        System.out.println(type);
    }

    /**
     * {@link WildcardType} ?--通配符表达式，表示通配符泛型，但是WildcardType并不属于Java-Type中的一种
     * 例如：List<? extends Number>和List<? super Integer>
     * @throws NoSuchFieldException
     */
    @Test
    public void testWildcardType() throws NoSuchFieldException {
        Field fieldNum = WildcardTypeTest.class.getDeclaredField("listNum");
        Type typeNum = fieldNum.getGenericType();
        ParameterizedType parameterizedTypeNum = (ParameterizedType) typeNum;
        // 获取泛型中的实际类型
        Type[] typesNum = parameterizedTypeNum.getActualTypeArguments();
        // 通配符类型：class sun.reflect.generics.reflectiveObjects.WildcardTypeImpl
        System.out.println(typesNum[0].getClass());
    }

    // 获取泛型变量的上边界（extends）
    @Test
    public void testGetUpperBounds() throws NoSuchFieldException {
        Field fieldNum = WildcardTypeTest.class.getDeclaredField("listNum");
        Type typeNum = fieldNum.getGenericType();
        ParameterizedType parameterizedTypeNum = (ParameterizedType) typeNum;
        // 获取泛型中的实际类型
        Type[] typesNum = parameterizedTypeNum.getActualTypeArguments();
        // 通配符类型：class sun.reflect.generics.reflectiveObjects.WildcardTypeImpl
        WildcardType wildcardType = (WildcardType) typesNum[0];
        // 获取泛型变量的上边界（extends）
        Type[] types = wildcardType.getUpperBounds();
        // class java.lang.Number
        System.out.println(types[0]);
    }

    // 获取泛型变量的下边界（super）
    @Test
    public void testGetLowerBounds() throws NoSuchFieldException {
        Field fieldStr = WildcardTypeTest.class.getDeclaredField("listStr");
        Type typeStr = fieldStr.getGenericType();
        ParameterizedType parameterizedTypeStr = (ParameterizedType) typeStr;
        // 获取泛型中的实际类型
        Type[] typesStr = parameterizedTypeStr.getActualTypeArguments();
        // 通配符类型：class sun.reflect.generics.reflectiveObjects.WildcardTypeImpl
        WildcardType wildcardType = (WildcardType) typesStr[0];
        // 获取泛型变量的下边界（super）
        Type[] types = wildcardType.getLowerBounds();
        // class java.lang.String
        System.out.println(types[0]);
    }
}

class ParameterizedTypeTest<T> {
    private List<T> list = null;
    private Set<T> set = null;
    private Map<String, Integer> map = null;
    private Map.Entry<String, Integer> mapEntry;
}

class GenericArrayTypeTest<T> {
    private T[] t;
    private List<String>[] listArray;
}

class TypeVariableTest<T> {
    private List<T> list;
}

class TypeVariableTest1<T extends Number & Serializable & Comparable> {
    private T t;
}

class TypeVariableTest2<T> {
    private T t;
}

class ClassTest {
    private ClassTest classTest;
}

class WildcardTypeTest {
    private List<? extends Number> listNum;
    private List<? super String> listStr;
}