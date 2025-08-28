package com.origami.mybatis.cache;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.util.TablesNamesFinder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

/**
 * 缓存管理器，支持一级缓存和二级缓存
 * 一级缓存：SqlSession级别，会话结束即清空
 * 二级缓存：跨SqlSession，支持内存缓存等多种实现
 */
public class CacheManager implements Cache {

    // 一级缓存（SqlSession级别）
    private final ConcurrentHashMap<String, Object> localCache = new ConcurrentHashMap<>();
    
    // 表名到缓存Key的映射关系
    private final ConcurrentHashMap<String, Set<String>> tableToKeys = new ConcurrentHashMap<>();
    
    // 二级缓存（SqlSessionFactory级别）
    private Cache secondLevelCache;
    
    // 缓存操作读写锁
    private final ReadWriteLock cacheLock = new ReentrantReadWriteLock();
    
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
            // 使用序列化工具检查对象是否可序列化
            try {
                SerializationUtil.serialize(value); // 验证序列化
                secondLevelCache.put(key, value);
            } catch (IllegalArgumentException e) {
                System.out.println("警告: " + e.getMessage() + " - 跳过二级缓存存储");
                // 只存储到一级缓存，不影响正常功能
            }
        }
    }
    
    /**
     * 存储缓存并建立表名映射关系
     */
    public void putWithTable(String key, Object value, String sql) {
        cacheLock.writeLock().lock();
        try {
            put(key, value);
            
            // 建立表名到缓存Key的映射
            String tableName = extractTableNameFromSQL(sql);
            if (tableName != null) {
                tableToKeys.computeIfAbsent(tableName.toLowerCase(), 
                    k -> ConcurrentHashMap.newKeySet()).add(key);
            }
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    @Override
    public Object get(String key) {
        cacheLock.readLock().lock();
        try {
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
        } finally {
            cacheLock.readLock().unlock();
        }
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
    
    /**
     * 按表名清理相关缓存
     * 使用映射表实现精确清理
     */
    public void clearByTable(String tableName) {
        if (tableName == null || tableName.trim().isEmpty()) {
            return;
        }
        
        cacheLock.writeLock().lock();
        try {
            String tableKey = tableName.toLowerCase();
            Set<String> keys = tableToKeys.get(tableKey);
            
            if (keys != null && !keys.isEmpty()) {
                // 从一级缓存中删除相关的缓存项
                keys.forEach(key -> {
                    localCache.remove(key);
                    // 同时从二级缓存中删除
                    if (secondLevelCache != null) {
                        secondLevelCache.remove(key);
                    }
                });
                
                // 清理映射关系
                tableToKeys.remove(tableKey);
                
                System.out.println("已清理表 " + tableName + " 相关缓存，共 " + keys.size() + " 个缓存项");
            } else {
                System.out.println("表 " + tableName + " 无相关缓存需要清理");
            }
        } finally {
            cacheLock.writeLock().unlock();
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
     * 从SQL语句中提取表名
     */
    public String extractTableNameFromSQL(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return null;
        }
        
        try {
            // 使用JSqlParser解析SQL
            Statement statement = CCJSqlParserUtil.parse(sql);
            TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
            Set<String> tableNamesSet = tablesNamesFinder.getTables(statement);
            List<String> tableNames = new ArrayList<>(tableNamesSet);
            
            if (!tableNames.isEmpty()) {
                // 返回第一个表名（主表）
                String tableName = tableNames.get(0);
                // 处理schema.table格式，只取表名部分
                if (tableName.contains(".")) {
                    tableName = tableName.substring(tableName.lastIndexOf(".") + 1);
                }
                // 移除引号
                tableName = tableName.replaceAll("[`\"'\\[\\]]", "");
                
                System.out.println("解析成功 - 主表: " + tableName +
                                 (tableNames.size() > 1 ? ", 涉及表数: " + tableNames.size() : ""));
                return tableName.toLowerCase();
            }
            
        } catch (JSQLParserException e) {
            System.out.println("解析失败: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * 设置二级缓存
     */
    public void setSecondLevelCache(Cache secondLevelCache) {
        this.secondLevelCache = secondLevelCache;
    }

}

