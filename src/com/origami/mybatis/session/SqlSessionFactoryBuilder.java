package com.origami.mybatis.session;

import com.origami.mybatis.config.Configuration;

import java.io.InputStream;
import java.util.Properties;

/**
 * SqlSessionFactory建造者
 * 体现了建造者模式(Builder Pattern)，提供了多种构建SqlSessionFactory的方式
 */
public class SqlSessionFactoryBuilder {
    
    /**
     * 通过配置对象构建SqlSessionFactory
     */
    public SqlSessionFactory build(Configuration configuration) {
        return new DefaultSqlSessionFactory(configuration);
    }
    
    /**
     * 通过配置文件流构建SqlSessionFactory
     */
    public SqlSessionFactory build(InputStream inputStream) {
        try {
            Properties props = new Properties();
            props.load(inputStream);
            
            // 创建配置对象
            Configuration configuration = Configuration.builder()
                .database(props.getProperty("jdbc.url"), 
                         props.getProperty("jdbc.username"), 
                         props.getProperty("jdbc.password"))
                .build();
            
            return new DefaultSqlSessionFactory(configuration);
        } catch (Exception e) {
            throw new RuntimeException("构建SqlSessionFactory失败", e);
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (Exception e) {
                System.err.println("关闭输入流失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 通过配置文件路径构建SqlSessionFactory
     */
    public SqlSessionFactory build(String resource) {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resource);
        if (inputStream == null) {
            throw new RuntimeException("找不到配置文件: " + resource);
        }
        return build(inputStream);
    }
    
    /**
     * 使用默认配置构建SqlSessionFactory
     */
    public SqlSessionFactory build() {
        Configuration configuration = Configuration.builder()
            .database("jdbc.properties")
            .build();
        return new DefaultSqlSessionFactory(configuration);
    }
}
