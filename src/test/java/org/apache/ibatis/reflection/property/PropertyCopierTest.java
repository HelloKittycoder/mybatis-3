package org.apache.ibatis.reflection.property;

import org.apache.ibatis.reflection.factory.DefaultObjectFactory;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

class PropertyCopierTest {

    @Test
    void copyBeanProperties() {
        ObjectFactory objectFactory = new DefaultObjectFactory();
        Student student = objectFactory.create(Student.class, Arrays.asList(String.class, String.class), Arrays.asList("1", "张三"));
        student.setHairColor("black");
        Student student1 = objectFactory.create(Student.class);
        PropertyCopier.copyBeanProperties(Student.class, student, student1);
        System.out.println(student1);
    }
}

class Student extends People {
    private String id;
    private String name;

    public Student() {
    }

    public Student(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

class People {
    private String hairColor;

    public String getHairColor() {
        return hairColor;
    }

    public void setHairColor(String hairColor) {
        this.hairColor = hairColor;
    }
}