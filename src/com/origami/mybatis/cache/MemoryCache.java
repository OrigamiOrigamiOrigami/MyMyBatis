package com.origami.mybatis.cache;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 内存二级缓存实现
 * 支持LRU淘汰策略和过期时间
 */
public class MemoryCache implements Cache {
    
    private final String namespace;
    private final int maxSize; // 最大缓存条目数
    private final long expireTimeMs; // 过期时间(毫秒)
    
    // 缓存数据存储
    private final ConcurrentHashMap<String, CacheEntry> cache;
    // LRU访问顺序队列
    private final ConcurrentLinkedQueue<String> accessQueue;
    // 读写锁
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    /**
     * 缓存条目，包含数据和时间戳
     */
    private static class CacheEntry {
        final Object value;
        final long createTime;
        
        CacheEntry(Object value) {
            this.value = value;
            this.createTime = System.currentTimeMillis();
        }
        
        boolean isExpired(long expireTimeMs) {
            return expireTimeMs > 0 && 
                   (System.currentTimeMillis() - createTime) > expireTimeMs;
        }
    }

    public MemoryCache(String namespace, int maxSize, long expireTimeMs) {
        this.namespace = namespace;
        this.maxSize = maxSize;
        this.expireTimeMs = expireTimeMs;
        this.cache = new ConcurrentHashMap<>(maxSize);
        this.accessQueue = new ConcurrentLinkedQueue<>();
        
        System.out.println("内存二级缓存初始化 - 命名空间: " + namespace + 
                          ", 最大条目: " + maxSize + ", 过期时间: " + (expireTimeMs/1000) + "秒");
    }
    
    @Override
    public void put(String key, Object value) {
        // 检查对象是否可序列化
        if (!(value instanceof Serializable)) {
            throw new IllegalArgumentException("缓存对象必须实现Serializable接口: " + value.getClass().getName());
        }
        
        String fullKey = namespace + ":" + key;
        
        lock.writeLock().lock();
        try {
            // 检查是否需要淘汰旧数据
            if (cache.size() >= maxSize) {
                evictLRU();
            }
            
            // 存储新数据
            CacheEntry entry = new CacheEntry(value);
            cache.put(fullKey, entry);
            accessQueue.offer(fullKey);
            
            System.out.println("内存缓存存储: " + fullKey + " (当前大小: " + cache.size() + ")");
            
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public Object get(String key) {
        String fullKey = namespace + ":" + key;
        
        lock.readLock().lock();
        try {
            CacheEntry entry = cache.get(fullKey);
            
            if (entry == null) {
                return null;
            }
            
            // 检查是否过期
            if (entry.isExpired(expireTimeMs)) {
                // 需要写锁来删除过期数据
                lock.readLock().unlock();
                lock.writeLock().lock();
                try {
                    // 双重检查
                    entry = cache.get(fullKey);
                    if (entry != null && entry.isExpired(expireTimeMs)) {
                        cache.remove(fullKey);
                        accessQueue.remove(fullKey);
                        System.out.println("内存缓存过期移除: " + fullKey);
                        return null;
                    }
                } finally {
                    lock.readLock().lock();
                    lock.writeLock().unlock();
                }
            }
            
            if (entry != null) {
                // 更新LRU顺序
                accessQueue.remove(fullKey);
                accessQueue.offer(fullKey);
                
                System.out.println("内存缓存命中: " + fullKey);
                return entry.value;
            }
            
            return null;
            
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public boolean containsKey(String key) {
        String fullKey = namespace + ":" + key;
        
        lock.readLock().lock();
        try {
            CacheEntry entry = cache.get(fullKey);
            return entry != null && !entry.isExpired(expireTimeMs);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public void remove(String key) {
        String fullKey = namespace + ":" + key;
        
        lock.writeLock().lock();
        try {
            if (cache.remove(fullKey) != null) {
                accessQueue.remove(fullKey);
                System.out.println("内存缓存移除: " + fullKey);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public void clear() {
        lock.writeLock().lock();
        try {
            cache.clear();
            accessQueue.clear();
            System.out.println("内存缓存清空 - 命名空间: " + namespace);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public int size() {
        lock.readLock().lock();
        try {
            // 清理过期数据后返回实际大小
            cleanExpired();
            return cache.size();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * LRU淘汰策略：移除最久未访问的数据
     */
    private void evictLRU() {
        String oldestKey = accessQueue.poll();
        if (oldestKey != null) {
            cache.remove(oldestKey);
            System.out.println("LRU淘汰: " + oldestKey);
        }
    }
    
    /**
     * 清理过期数据
     */
    private void cleanExpired() {
        if (expireTimeMs <= 0) return;
        
        lock.writeLock().lock();
        try {
            cache.entrySet().removeIf(entry -> {
                if (entry.getValue().isExpired(expireTimeMs)) {
                    accessQueue.remove(entry.getKey());
                    System.out.println("清理过期缓存: " + entry.getKey());
                    return true;
                }
                return false;
            });
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 获取缓存统计信息
     */
    public String getStats() {
        lock.readLock().lock();
        try {
            return String.format("命名空间: %s, 当前大小: %d/%d, 过期时间: %d秒", 
                            namespace, cache.size(), maxSize, expireTimeMs/1000);
        } finally {
            lock.readLock().unlock();
        }
    }
}
