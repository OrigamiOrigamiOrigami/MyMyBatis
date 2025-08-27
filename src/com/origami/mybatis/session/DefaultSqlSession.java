package com.origami.mybatis.session;

import com.origami.mybatis.annotation.Delete;
import com.origami.mybatis.annotation.Insert;
import com.origami.mybatis.annotation.Select;
import com.origami.mybatis.annotation.Update;
import com.origami.mybatis.config.Configuration;
import com.origami.mybatis.cache.CacheManager;
import com.origami.mybatis.executor.SqlExecutor;
import com.origami.mybatis.handler.ResultSetMapper;
import com.origami.mybatis.exception.SqlExecutionException;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class DefaultSqlSession implements SqlSession {
/**
 * SqlSession 的核心实现类，体现了多种设计模式：
 * 1. 外观模式 (Facade Pattern): 作为整个 MyMyBatis 框架的统一入口，隐藏了内部复杂的组件（如 SqlExecutor, CacheManager, Configuration等）的交互细节。
 * 2. AOP (面向切面编程) 核心: 通过实现 InvocationHandler 接口的 invoke 方法，充当了所有 Mapper 接口方法的切面，织入了缓存、事务等通用逻辑。
 * 3. 工厂模式 (Factory Pattern): 内部的 getMapper 方法扮演了 Mapper 实例的工厂，负责创建代理对象。
 */

    // 缓存管理器
    private final CacheManager cacheManager;

    // SQL执行器
    private static final SqlExecutor sqlExecutor = new SqlExecutor();

    // 结果集映射器
    private static final ResultSetMapper resultSetMapper = new ResultSetMapper();

    // 事务管理
    protected Connection transactionConnection;
    protected boolean inTransaction = false;
    
    // 配置对象
    private Configuration configuration;
    
    // 构造函数
    public DefaultSqlSession(CacheManager cacheManager) {
        this.cacheManager = cacheManager != null ? cacheManager : new CacheManager();
    }
    
    public DefaultSqlSession(Configuration configuration) {
        this.configuration = configuration;
        this.cacheManager = configuration.getCacheManager();
    }

    /**
     * 工厂方法：创建 Mapper 接口的动态代理对象。
     * 这是 AOP "织入" 过程的核心体现。
     * @param clazz Mapper 接口的 Class 对象
     * @return 代理对象
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMapper(Class<T> clazz) {
        System.out.println("创建Mapper代理对象：" + clazz.getSimpleName());
        Class<?>[] interfaces = new Class[]{clazz};
        Object mapper = Proxy.newProxyInstance(clazz.getClassLoader(), interfaces, this::invoke);
        return (T) mapper;
    }

    @Override
    public void beginTransaction() {
        if (inTransaction) {
            throw new RuntimeException("事务已经开启，不能重复开启");
        }
        try {
            if (configuration == null) {
                throw new RuntimeException("Configuration未初始化，无法开启事务");
            }
            transactionConnection = configuration.getConnection();
            transactionConnection.setAutoCommit(false);
            inTransaction = true;
            System.out.println("事务已开启");
        } catch (Exception e) {
            throw new RuntimeException("开启事务失败", e);
        }
    }

    @Override
    public void commit() {
        if (!inTransaction || transactionConnection == null) {
            throw new RuntimeException("没有事务可以提交");
        }
        try {
            transactionConnection.commit();
            System.out.println("\n事务已提交");
        } catch (Exception e) {
            throw new RuntimeException("提交事务失败", e);
        } finally {
            closeTransaction();
        }
    }

    @Override
    public void rollback() {
        if (!inTransaction || transactionConnection == null) {
            throw new RuntimeException("没有事务可以回滚");
        }
        try {
            transactionConnection.rollback();
            System.out.println("事务已回滚");
        } catch (Exception e) {
            throw new RuntimeException("回滚事务失败", e);
        } finally {
            closeTransaction();
        }
    }

    @Override
    public void close() {
        if (inTransaction) {
            rollback();
        }
        cacheManager.clear();
        System.out.println("SqlSession已关闭");
    }
    /**
     * AOP核心："切面"与"通知"的实现。
     * 这是一个环绕通知 (Around Advice)，它拦截了 Mapper 接口的所有方法调用。
     * @param proxy 代理对象
     * @param method 被调用的方法
     * @param args 方法参数
     * @return 方法执行结果
     */
    private Object invoke(Object proxy, Method method, Object[] args) {
        System.out.println("\n调用的方法：" + method.getName());

        // 处理Object基本方法
        if (method.getDeclaringClass() == Object.class) {
            return handleObjectMethod(proxy, method, args);
        }

        // 检查是否为增删改操作
        String modificationSql = getModificationSql(method);
        if (modificationSql != null) {
            return handleModification(modificationSql, args);
        }

        // 检查是否为查询操作
        if (method.isAnnotationPresent(Select.class)) {
            return handleQuery(method, args);
        }

        return null;
    }

    /**
     * 处理Object基本方法
     */
    private Object handleObjectMethod(Object proxy, Method method, Object[] args) {
        String methodName = method.getName();
        
        switch (methodName) {
            case "toString":
                return proxy.getClass().getSimpleName() + "@" + Integer.toHexString(proxy.hashCode());
            case "equals":
                return proxy == args[0];
            default:
                throw new UnsupportedOperationException("不支持的Object方法: " + methodName);
        }
    }

    /**
     * 处理增删改操作
     */
    private int handleModification(String sql, Object[] args) {
        // 提取表名并清理缓存
        String tableName = cacheManager.extractTableNameFromSQL(sql);
        if (tableName != null) {
            cacheManager.clearByTable(tableName);
        } else {
            cacheManager.clearAll(); // 无法解析表名时回退到全清
        }
        
        Connection connection = null;
        try {
            connection = getConnection();
            return sqlExecutor.executeUpdate(connection, sql, args);
        } catch (SQLException e) {
            throw new RuntimeException("执行更新操作时出错", e);
        } finally {
            closeResources(connection, null, null);
        }
    }

    /**
     * 处理查询操作
     */
    private Object handleQuery(Method method, Object[] args) {
        Select annotation = method.getAnnotation(Select.class);
        String selectSql = annotation.value();
        String cacheKey = cacheManager.generateCacheKey(selectSql, args, method.getReturnType());

        if (cacheManager.containsKey(cacheKey)) {
            System.out.println("缓存命中");
            return cacheManager.get(cacheKey);
        }
        Connection connection = null;
        try {
            connection = getConnection();
            Object result = sqlExecutor.executeQuery(connection, selectSql, args, rs -> {
                Class<?> returnType = method.getReturnType();
                if (returnType == Integer.class) {
                    return rs.next() ? rs.getInt(1) : 0;
                }
                if (returnType == List.class) {
                    // 处理泛型返回类型，绕过类型擦除
                    // Java 的类型擦除 (Type Erasure) 会在运行时将 List<Account> 变为 List，无法直接获取泛型类型 Account。
                    // 但通过 Method.getGenericReturnType() 可以获取到带有泛型参数的 Type 对象 (ParameterizedType)，
                    // 从而在运行时动态地拿到泛型的实际类型 (Account.class)，实现精准的结果集映射。
                    Type genericReturnType = method.getGenericReturnType();
                    if (genericReturnType instanceof ParameterizedType) {
                        Class<?> elementType = (Class<?>) ((ParameterizedType) genericReturnType).getActualTypeArguments()[0];
                        return resultSetMapper.mapResultSetToList(rs, elementType);
                    }
                }
                if (returnType == Map.class) {
                    return rs.next() ? resultSetMapper.mapResultSetToMap(rs) : new HashMap<>();
                }
                return rs.next() ? resultSetMapper.mapResultSetToObject(rs, returnType) : null;
            });
            cacheManager.putWithTable(cacheKey, result, selectSql);
            return result;
        } catch (Exception e) {
            throw new SqlExecutionException(selectSql, args, e);
        } finally {
            closeResources(connection, null, null);
        }
    }


    /**
     * 获取增删改操作的SQL
     */
    private String getModificationSql(Method method) {
        if (method.isAnnotationPresent(Insert.class)) {
            return method.getAnnotation(Insert.class).value();
        }
        if (method.isAnnotationPresent(Delete.class)) {
            return method.getAnnotation(Delete.class).value();
        }
        if (method.isAnnotationPresent(Update.class)) {
            return method.getAnnotation(Update.class).value();
        }
        return null;
    }

    /**
     * 获取连接（事务感知）
     */
    private Connection getConnection() throws SQLException {
        if (inTransaction && transactionConnection != null) {
            return transactionConnection;
        }
        if (configuration == null) {
            throw new SQLException("Configuration未初始化，无法获取数据库连接");
        }
        return configuration.getConnection();
    }

    /**
     * 关闭事务连接
     */
    private void closeTransaction() {
        if (transactionConnection != null) {
            try {
                transactionConnection.setAutoCommit(true);
            } catch (Exception e) {
                System.err.println("恢复自动提交模式失败: " + e.getMessage());
            }
            
            try {
                transactionConnection.close();
            } catch (Exception e) {
                System.err.println("关闭事务连接失败: " + e.getMessage());
            }
        }
        
        transactionConnection = null;
        inTransaction = false;
    }

    /**
     * 关闭资源（事务感知）
     */
    private void closeResources(Connection connection, PreparedStatement statement, ResultSet resultSet) {
        try {
            if (resultSet != null) resultSet.close();
            if (statement != null) statement.close();
            // 只有在非事务模式下才关闭连接
            if (connection != null && !inTransaction) {
                connection.close();
            }
        } catch (Exception e) {
            System.err.println("关闭资源失败: " + e.getMessage());
        }
    }
}

