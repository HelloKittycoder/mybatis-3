package org.apache.ibatis.reflection.wrapper;

import org.apache.ibatis.domain.misc.CustomBeanWrapperFactory;
import org.apache.ibatis.domain.misc.RichType;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BeanWrapperTest {

    @Test
    void getGetterType() {
        RichType object = new RichType();

        object.setRichType(new RichType());
        object.getRichType().setRichMap(new HashMap());
        object.getRichType().getRichMap().put("nihao", "123");

        MetaObject meta = MetaObject.forObject(object, SystemMetaObject.DEFAULT_OBJECT_FACTORY,
                new CustomBeanWrapperFactory(), new DefaultReflectorFactory());
        Class<?> clazz = meta.getObjectWrapper().getGetterType("richType.richMap.nihao");
        System.out.println(clazz);
    }

    @Test
    void hasGetter() {
        RichType object = new RichType();

        MetaObject meta = MetaObject.forObject(object, SystemMetaObject.DEFAULT_OBJECT_FACTORY,
                new CustomBeanWrapperFactory(), new DefaultReflectorFactory());

        assertTrue(meta.getObjectWrapper().hasGetter("richType"));
        assertTrue(meta.getObjectWrapper().hasGetter("richType.richField"));
        assertTrue(meta.getObjectWrapper().hasGetter("richField"));
        assertTrue(meta.getObjectWrapper().hasGetter("richMap"));
    }
}