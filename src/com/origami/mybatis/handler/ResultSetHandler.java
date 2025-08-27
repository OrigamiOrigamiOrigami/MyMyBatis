package com.origami.mybatis.handler;

import java.sql.ResultSet;

/**
 * 函数式接口，用于处理ResultSet
 * 
 * 设计思想：
 *  函数式编程：只有一个抽象方法，支持Lambda表达式
 *  策略模式：不同的调用场景可以传入不同的处理策略
 *  泛型支持：返回类型安全，避免强制类型转换
 * 
 * 使用场景：
 *  Integer查询：rs -> rs.next() ? rs.getInt(1) : 0
 *  List查询：rs -> resultSetMapper.mapResultSetToList(rs, elementType)
 *  Map查询：rs -> resultSetMapper.mapResultSetToMap(rs)
 *  对象查询：rs -> resultSetMapper.mapResultSetToObject(rs, returnType)
 * 
 * @param <T> 处理结果的返回类型
 */
@FunctionalInterface
public interface ResultSetHandler<T> {

    T handle(ResultSet rs) throws Exception;
}

