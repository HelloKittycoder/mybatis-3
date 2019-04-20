package org.apache.ibatis.builder.xml.testIncludeSql;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.jupiter.api.Test;

import java.io.Reader;

/**
 * Created by shucheng on 2019-4-20 下午 19:46
 */
public class XmlIncludeTansformerTest {

    @Test
    public void testApplyIncludes() throws Exception {
        String resource = "org/apache/ibatis/builder/xml/testIncludeSql/mybatis-config.xml";
        Reader reader = Resources.getResourceAsReader(resource);
        SqlSessionFactoryBuilder builder = new SqlSessionFactoryBuilder();
        SqlSessionFactory sqlSessionFactory = builder.build(reader);
        System.out.println(sqlSessionFactory);
    }
}
