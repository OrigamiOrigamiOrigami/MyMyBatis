package com.origami.mybatis.session;

import com.origami.mybatis.config.Configuration;

/**
 * SqlSession工厂接口
 * 简单工厂模式(Simple Factory Pattern): 根据参数创建不同配置的SqlSession实例
 */
public interface SqlSessionFactory {

    /**
     * 打开一个新的SqlSession
     * @param autoCommit 是否自动提交事务
     */
    SqlSession openSession(boolean autoCommit);
    
    /**
     * 获取配置信息
     */
    Configuration getConfiguration();
    
    /**
     * 关闭工厂
     */
    void shutdown();
}
