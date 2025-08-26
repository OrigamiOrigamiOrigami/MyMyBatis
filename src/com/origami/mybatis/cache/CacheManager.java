package com.origami.mybatis.cache;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 缓存管理器，支持一级缓存和二级缓存
 * 一级缓存：SqlSession级别，会话结束即清空
 * 二级缓存：跨SqlSession，使用Redis进行缓存
 */
public class CacheManager implements Cache {

    // 一级缓存（SqlSession级别）
    private final ConcurrentHashMap<String, Object> localCache = new ConcurrentHashMap<>();
    
    // 二级缓存（SqlSessionFactory级别）
    private Cache secondLevelCache;
    
    public CacheManager() {
        // 默认不启用二级缓存
    }
    
    public CacheManager(Cache secondLevelCache) {
        this.secondLevelCache = secondLevelCache;
        System.out.println("缓存管理器初始化 - 启用二级缓存");
    }

    @Override
    public void put(String key, Object value) {
        // 存储到一级缓存
        localCache.put(key, value);
        
        // 如果启用了二级缓存，也存储到二级缓存
        if (secondLevelCache != null) {
            secondLevelCache.put(key, value);
        }
    }

    @Override
    public Object get(String key) {
        
        // 先查二级缓存（跨SqlSession共享）
        if (secondLevelCache != null) {
            Object value = secondLevelCache.get(key);
            if (value != null) {
                // 回填到一级缓存
                localCache.put(key, value);
                System.out.println("二级缓存命中，回填一级缓存");
                return value;
            }
        }
        
        // 再查一级缓存（当前SqlSession）
        Object value = localCache.get(key);
        if (value != null) {
            System.out.println("一级缓存命中");
            return value;
        }
        
        // 都没命中，返回null，由调用方查询数据库
        return null;
    }

    @Override
    public boolean containsKey(String key) {
        return localCache.containsKey(key) || 
               (secondLevelCache != null && secondLevelCache.containsKey(key));
    }

    /**
     * 删除缓存项
     */
    @Override
    public void remove(String key) {
        localCache.remove(key);
        if (secondLevelCache != null) {
            secondLevelCache.remove(key);
        }
    }

    /**
     * 清空一级缓存（当前SqlSession）
     */
    @Override
    public void clear() {
        localCache.clear();
        System.out.println("一级缓存已清空");
        
    }
    
    /**
     * 清空所有缓存（包括二级缓存）
     * 仅在写操作时调用
     */
    public void clearAll() {
        localCache.clear();
        System.out.println("一级缓存已清空");
        
        if (secondLevelCache != null) {
            secondLevelCache.clear();
            System.out.println("二级缓存已清空");
        }
    }

    @Override
    public int size() {
        return localCache.size();
    }

    /**
     * 生成缓存key (MD5)
     */
    public String generateCacheKey(String sql, Object[] args, Class<?> returnType) {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(sql).append("|").append(returnType.getName());

        if (args != null) {
            for (Object arg : args) {
                keyBuilder.append("|").append(arg != null ? arg.toString() : "null");
            }
        }

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(keyBuilder.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            // 如果MD5失败，回退到简单的hashCode
            return String.valueOf(keyBuilder.toString().hashCode());
        }
    }
    
    /**
     * 设置二级缓存
     */
    public void setSecondLevelCache(Cache secondLevelCache) {
        this.secondLevelCache = secondLevelCache;
    }

}

