package com.origami.mybatis.session;

/**
 * MyMyBatis 框架的核心接口，定义了与外部交互的顶层 API。
 * 这是 "门面模式" (Facade Pattern) 的体现，为使用者提供了一个简洁统一的入口，
 * 隐藏了内部复杂的子系统（如配置加载、SQL执行、事务管理、缓存等）。
 */
public interface SqlSession {
    /**
     * 获取mapper接口的代理对象
     */
    <T> T getMapper(Class<T> clazz) throws InstantiationException, IllegalAccessException;

    /**
     * 开启事务
     */
    void beginTransaction();

    /**
     * 提交事务
     */
    void commit();

    /**
     * 回滚事务
     */
    void rollback();

    /**
     * 关闭会话
     */
    void close();
}
