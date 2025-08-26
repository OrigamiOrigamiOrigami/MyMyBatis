package com.origami.mybatis.config;

import com.origami.mybatis.cache.CacheManager;
import com.origami.mybatis.cache.MemoryCache;

import java.io.InputStream;
import java.util.Properties;

/**
 * Configuration构建器
 * 支持链式调用和流畅的API设计
 */
public class ConfigurationBuilder {
    
    private Properties properties = new Properties();
    private boolean cacheEnabled = true;
    private int cacheMaxSize = 1000;
    private long cacheExpireTimeMs = 30 * 60 * 1000L; // 30分钟
    private boolean secondLevelCacheEnabled = false;
    
    /**
     * 从配置文件加载数据库配置
     */
    public ConfigurationBuilder database(String propertiesFile) {
        try {
            InputStream in = ConfigurationBuilder.class.getClassLoader().getResourceAsStream(propertiesFile);
            if (in == null) {
                throw new RuntimeException("找不到配置文件: " + propertiesFile);
            }
            properties.load(in);
            in.close();
        } catch (Exception e) {
            throw new RuntimeException("配置文件加载失败: " + propertiesFile, e);
        }
        return this;
    }
    
    /**
     * 设置数据库连接参数
     */
    public ConfigurationBuilder database(String url, String username, String password) {
        properties.setProperty("jdbc.url", url);
        properties.setProperty("jdbc.username", username);
        properties.setProperty("jdbc.password", password);
        properties.setProperty("jdbc.driverClassName", "com.mysql.cj.jdbc.Driver");
        return this;
    }
    
    /**
     * 配置连接池参数
     */
    public ConfigurationBuilder connectionPool(int initialSize, int maxSize) {
        properties.setProperty("jdbc.initialSize", String.valueOf(initialSize));
        properties.setProperty("jdbc.maxSize", String.valueOf(maxSize));
        return this;
    }
    
    /**
     * 启用内存二级缓存
     */
    public ConfigurationBuilder enableSecondLevelCache() {
        this.secondLevelCacheEnabled = true;
        return this;
    }
    
    /**
     * 启用内存二级缓存并配置参数
     */
    public ConfigurationBuilder enableSecondLevelCache(int maxSize, long expireTimeMs) {
        this.cacheMaxSize = maxSize;
        this.cacheExpireTimeMs = expireTimeMs;
        this.secondLevelCacheEnabled = true;
        return this;
    }
    
    /**
     * 禁用缓存
     */
    public ConfigurationBuilder disableCache() {
        this.cacheEnabled = false;
        return this;
    }
    
    /**
     * 构建Configuration对象
     */
    public Configuration build() {
        Configuration config = new Configuration();
        
        // 设置数据库配置
        config.setProperties(properties);
        
        // 初始化缓存管理器
        if (cacheEnabled) {
            CacheManager cacheManager = new CacheManager();
            
            // 如果启用了二级缓存，配置内存缓存
            if (secondLevelCacheEnabled) {
                try {
                    MemoryCache memoryCache = new MemoryCache("mybatis_cache", cacheMaxSize, cacheExpireTimeMs);
                    cacheManager.setSecondLevelCache(memoryCache);
                } catch (Exception e) {
                    System.err.println("内存二级缓存初始化失败: " + e.getMessage());
                }
            }
            
            config.setCacheManager(cacheManager);
            System.out.println("缓存管理器初始化 - 启用" + (secondLevelCacheEnabled ? "二级缓存" : "一级缓存"));
        }
        
        return config;
    }
}
