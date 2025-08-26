package com.origami.mybatis.mapper;

import com.origami.mybatis.annotation.Delete;
import com.origami.mybatis.annotation.Insert;
import com.origami.mybatis.annotation.Select;
import com.origami.mybatis.annotation.Update;
import com.origami.mybatis.pojo.Account;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface AccountMapper {

    @Insert("insert into account(name,money,create_time) values(?,?,?)")
    int insertAccount(String name, BigDecimal money, String createTime);

    @Delete("delete from account where id=?")
    int deleteAccount(int id);

    @Update("update account set money=? where id=?")
    int updateAccount(BigDecimal money, int id);

    @Select("select * from account where id = ?")
    Account selectAccount(int id);

    @Select("select * from account where id = ?")
    Map<String, Object> selectAccountAsMap(int id);

    @Select("select * from account")
    List<Account> selectAccounts();

    @Select("select * from account where name = ? and money > ?")
    List<Account> selectAccountsByNameAndMoney(String name, BigDecimal minMoney);

    @Select("select count(*) from account where money > ?")
    Integer countAccountsByMoney(BigDecimal minMoney);
}
