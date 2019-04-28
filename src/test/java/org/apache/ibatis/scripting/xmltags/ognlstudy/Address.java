package org.apache.ibatis.scripting.xmltags.ognlstudy;

/**
 * Created by shucheng on 2019-4-28 下午 21:02
 */
public class Address {

    private String port; // 邮编
    private String name; // 地址名称

    public Address() {
    }

    public Address(String port, String name) {
        this.port = port;
        this.name = name;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
