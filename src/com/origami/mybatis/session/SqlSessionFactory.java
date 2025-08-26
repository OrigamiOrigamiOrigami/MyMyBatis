package com.origami.mybatis.session;

import com.origami.mybatis.config.Configuration;

/**
 * SqlSession工厂接口
 * 体现了工厂模式(Factory Pattern)，负责创建SqlSession实例
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
