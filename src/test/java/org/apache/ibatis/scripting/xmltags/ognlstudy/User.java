package org.apache.ibatis.scripting.xmltags.ognlstudy;

/**
 * Created by shucheng on 2019-4-28 下午 21:01
 */
public class User {
    private String name; // 用户名
    private String userCode; // 用户code
    private Address address; // 地址

    public User() {
    }

    public User(String name, String userCode) {
        this.name = name;
        this.userCode = userCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUserCode() {
        return userCode;
    }

    public void setUserCode(String userCode) {
        this.userCode = userCode;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }
}
