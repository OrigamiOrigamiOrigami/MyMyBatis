package com.origami.mybatis.handler;

import java.sql.ResultSet;

/**
 * 函数式接口，用于处理ResultSet
 * @param <T> a
 */
@FunctionalInterface
public interface ResultSetHandler<T> {
    T handle(ResultSet rs) throws Exception;
}

