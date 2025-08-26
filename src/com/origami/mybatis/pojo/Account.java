package com.origami.mybatis.pojo;

import java.io.Serializable;
import java.math.BigDecimal;

public class Account implements Serializable {
    private Integer id;
    private String name;
    private BigDecimal money;
    private String createTime;

    public Account() {
    }

    public Account(Integer id, String name, BigDecimal money, String createTime) {
        this.id = id;
        this.name = name;
        this.money = money;
        this.createTime = createTime;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getMoney() {
        return money;
    }

    public void setMoney(BigDecimal money) {
        this.money = money;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return "Account{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", money=" + money +
                ", createTime='" + createTime + '\'' +
                '}';
    }
}
