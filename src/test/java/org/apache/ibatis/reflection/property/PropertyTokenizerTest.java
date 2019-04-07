package org.apache.ibatis.reflection.property;

import org.junit.jupiter.api.Test;

class PropertyTokenizerTest {

    // 测试PropertyTokenizer的点号分隔功能
    @Test
    void PropertyTokenizer() {
        String propertyName = "order[0].item[0].name";

        PropertyTokenizer prop = new PropertyTokenizer(propertyName);
        /*System.out.println(prop.getName());
        System.out.println(prop.next().getName());
        System.out.println(prop.next().next().getName());*/

        while (prop.hasNext()) {
            System.out.println(prop.getName());
            prop = prop.next();
        }
        System.out.println(prop.getName());
    }
}