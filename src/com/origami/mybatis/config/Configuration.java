package com.origami.mybatis.config;

import com.origami.mybatis.cache.CacheManager;
import com.origami.mybatis.pool.ConnectionPool;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * MyBatis配置类
 * 存储框架运行时需要的所有配置信息
 */
public class Configuration {

    private Properties properties;
    private ConnectionPool connectionPool;
    private CacheManager cacheManager;
    private boolean initialized = false;

    Configuration() {
        this.properties = new Properties();
    }

    /**
     * 设置配置属性
     */
    void setProperties(Properties properties) {
        this.properties = properties;
        initialize();
    }

    /**
     * 初始化数据库连接池
     */
    private void initialize() {
        if (initialized) return;
        
        try {
            // 加载数据库驱动
            String driverClass = properties.getProperty("jdbc.driverClassName");
            if (driverClass != null) {
                Class.forName(driverClass);
                System.out.println("数据库驱动加载成功：" + driverClass);
            }

            // 初始化连接池
            String url = properties.getProperty("jdbc.url");
            String username = properties.getProperty("jdbc.username");
            String password = properties.getProperty("jdbc.password");
            
            if (url != null && username != null && password != null) {
                connectionPool = new ConnectionPool(
                    url, username, password,
                    getIntProperty("jdbc.initialSize", 5),
                    getIntProperty("jdbc.maxSize", 10),
                    getIntProperty("jdbc.maxIdleTime", 300),
                    getIntProperty("jdbc.connectionTimeout", 30)
                );
            }
            
            initialized = true;
        } catch (Exception e) {
            throw new RuntimeException("配置初始化失败", e);
        }
    }

    /**
     * 获取整型配置属性
     */
    private int getIntProperty(String key, int defaultValue) {
        String value = properties.getProperty(key);
        return value != null ? Integer.parseInt(value) : defaultValue;
    }

    /**
     * 获取数据库连接
     */
    public Connection getConnection() throws SQLException {
        if (!initialized) {
            throw new SQLException("配置未初始化");
        }
        
        if (connectionPool != null) {
            return connectionPool.getConnection();
        } else {
            return java.sql.DriverManager.getConnection(
                properties.getProperty("jdbc.url"),
                properties.getProperty("jdbc.username"),
                properties.getProperty("jdbc.password")
            );
        }
    }

    /**
     * 获取连接池状态
     */
    public String getConnectionPoolStatus() {
        return connectionPool != null ? connectionPool.getStatus() : "连接池未初始化";
    }
    
    /**
     * 设置缓存管理器（内部使用）
     */
    void setCacheManager(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }
    
    /**
     * 获取缓存管理器
     */
    public CacheManager getCacheManager() {
        return cacheManager;
    }
    
    /**
     * 创建Configuration构建器
     */
    public static ConfigurationBuilder builder() {
        return new ConfigurationBuilder();
    }

}
