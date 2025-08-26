package com.origami.mybatis.cache;

/**
 * 缓存接口，定义了缓存的基本操作
 * 支持一级缓存和二级缓存的统一抽象
 */
public interface Cache {
    
    /**
     * 存储缓存
     */
    void put(String key, Object value);
    
    /**
     * 获取缓存
     */
    Object get(String key);
    
    /**
     * 检查是否包含指定key
     */
    boolean containsKey(String key);
    
    /**
     * 移除指定key的缓存
     */
    void remove(String key);
    
    /**
     * 清空所有缓存
     */
    void clear();
    
    /**
     * 获取缓存大小
     */
    int size();
}
