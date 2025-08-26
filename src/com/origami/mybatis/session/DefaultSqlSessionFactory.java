package com.origami.mybatis.session;

import com.origami.mybatis.config.Configuration;

/**
 * SqlSessionFactory的默认实现
 * 体现了工厂模式，负责创建和管理SqlSession实例
 */
public class DefaultSqlSessionFactory implements SqlSessionFactory {
    
    private final Configuration configuration;
    
    public DefaultSqlSessionFactory(Configuration configuration) {
        this.configuration = configuration;
    }
    
    @Override
    public SqlSession openSession(boolean autoCommit) {
        // 传入完整的配置对象，避免重复初始化
        DefaultSqlSession session = new DefaultSqlSession(configuration);
        if (autoCommit) {
            // 自动提交模式下不需要手动管理事务
            System.out.println("创建SqlSession - 自动提交模式");
        } else {
            System.out.println("创建SqlSession - 手动提交模式");
        }
        return session;
    }
    
    @Override
    public Configuration getConfiguration() {
        return configuration;
    }
    
    @Override
    public void shutdown() {
        System.out.println("SqlSessionFactory正在关闭...");
        
        // 清空二级缓存
        if (configuration.getCacheManager() != null) {
            configuration.getCacheManager().clearAll();
            System.out.println("二级缓存已清空");
        }
        
        // 关闭连接池
        System.out.println(configuration.getConnectionPoolStatus());
        System.out.println("SqlSessionFactory已关闭");
    }
}
