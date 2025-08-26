package com.origami.mybatis.pool;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * 数据库连接池
 */
public class ConnectionPool {

    private final String url;
    private final String username;
    private final String password;
    private final int initialSize;      // 初始连接数
    private final int maxSize;          // 最大连接数
    private final int maxIdleTime;      // 最大空闲时间(秒)
    private final int connectionTimeout; // 获取连接超时时间(秒)

    private final LinkedBlockingQueue<PooledConnection> idleConnections;
    private final AtomicInteger activeConnections = new AtomicInteger(0);
    private final AtomicInteger totalConnections = new AtomicInteger(0);

    private final ScheduledExecutorService cleanupExecutor;
    private volatile boolean shutdown = false;

    public ConnectionPool(String url, String username, String password, int initialSize) {
        this(url, username, password, initialSize, initialSize * 2, 300, 30);
    }

    public ConnectionPool(String url, String username, String password, int initialSize,
                         int maxSize, int maxIdleTime, int connectionTimeout) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.initialSize = initialSize;
        this.maxSize = maxSize;
        this.maxIdleTime = maxIdleTime;
        this.connectionTimeout = connectionTimeout;
        this.idleConnections = new LinkedBlockingQueue<>();

        // 启动清理线程
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "清理线程");
            t.setDaemon(true);
            return t;
        });

        // 创建初始连接
        initPool();

        // 定期清理过期连接
        startCleanupTask();
    }

    /**
     * 初始化连接池
     */
    private void initPool() {
        try {
            for (int i = 0; i < initialSize; i++) {
                Connection rawConn = DriverManager.getConnection(url, username, password);
                PooledConnection pooledConn = new PooledConnection(rawConn, this);
                idleConnections.offer(pooledConn);
                totalConnections.incrementAndGet();
            }
            System.out.println("连接池初始化完成，初始连接数：" + initialSize + "，最大连接数：" + maxSize);
        } catch (SQLException e) {
            throw new RuntimeException("连接池初始化失败", e);
        }
    }

    /**
     * 获取连接（支持超时）
     */
    public Connection getConnection() throws SQLException {
        if (shutdown) {
            throw new SQLException("连接池已关闭");
        }

        try {
            // 先尝试从空闲连接中获取
            PooledConnection conn = getValidConnection();
            if (conn != null) {
                activeConnections.incrementAndGet();
                conn.setLastUsedTime(System.currentTimeMillis());
                return conn;
            }

            // 如果没有空闲连接且未达到最大连接数，创建新连接
            if (totalConnections.get() < maxSize) {
                synchronized (this) {
                    if (totalConnections.get() < maxSize) {
                        Connection rawConn = DriverManager.getConnection(url, username, password);
                        PooledConnection newConn = new PooledConnection(rawConn, this);
                        totalConnections.incrementAndGet();
                        activeConnections.incrementAndGet();
                        newConn.setLastUsedTime(System.currentTimeMillis());
                        System.out.println("创建新连接，当前总连接数：" + totalConnections.get());
                        return newConn;
                    }
                }
            }

            // 等待空闲连接
            conn = idleConnections.poll(connectionTimeout, TimeUnit.SECONDS);
            if (conn != null && isConnectionValid(conn)) {
                activeConnections.incrementAndGet();
                conn.setLastUsedTime(System.currentTimeMillis());
                return conn;
            }

            throw new SQLException("获取连接超时，当前活跃连接数：" + activeConnections.get());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("获取连接被中断", e);
        }
    }

    /**
     * 获取有效的空闲连接
     */
    private PooledConnection getValidConnection() {
        PooledConnection conn;
        while ((conn = idleConnections.poll()) != null) {
            if (isConnectionValid(conn)) {
                return conn;
            } else {
                // 连接无效，关闭并减少总连接数
                closeConnection(conn);
                totalConnections.decrementAndGet();
            }
        }
        return null;
    }

    /**
     * 检查连接是否有效
     */
    private boolean isConnectionValid(PooledConnection conn) {
        try {
            return conn != null &&
                   !conn.getRealConnection().isClosed() &&
                   conn.getRealConnection().isValid(3);
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * 归还连接
     */
    public void returnConnection(PooledConnection connection) {
        if (connection == null || shutdown) {
            return;
        }

        try {
            if (isConnectionValid(connection)) {
                connection.setLastUsedTime(System.currentTimeMillis());
                idleConnections.offer(connection);
                activeConnections.decrementAndGet();
            } else {
                // 连接无效，关闭并减少总连接数
                closeConnection(connection);
                totalConnections.decrementAndGet();
                activeConnections.decrementAndGet();
            }
        } catch (Exception e) {
            System.err.println("归还连接失败: " + e.getMessage());
        }
    }

    /**
     * 启动清理任务
     */
    private void startCleanupTask() {
        cleanupExecutor.scheduleWithFixedDelay(this::cleanupIdleConnections, 60, 60, TimeUnit.SECONDS);
    }

    /**
     * 清理过期的空闲连接
     */
    private void cleanupIdleConnections() {
        if (shutdown) return;

        long currentTime = System.currentTimeMillis();
        int cleaned = 0;

        // 保留至少initialSize个连接
        while (totalConnections.get() > initialSize) {
            PooledConnection conn = idleConnections.peek();
            if (conn == null) break;

            long idleTime = (currentTime - conn.getLastUsedTime()) / 1000;
            if (idleTime > maxIdleTime) {
                idleConnections.poll();
                closeConnection(conn);
                totalConnections.decrementAndGet();
                cleaned++;
            } else {
                break; // 队列是按时间排序的，后面的连接更新
            }
        }

        if (cleaned > 0) {
            System.out.println("清理了 " + cleaned + " 个过期连接，当前总连接数：" + totalConnections.get());
        }
    }

    /**
     * 关闭连接
     */
    private void closeConnection(PooledConnection conn) {
        try {
            if (conn != null) {
                conn.getRealConnection().close();
            }
        } catch (SQLException e) {
            System.err.println("关闭连接失败: " + e.getMessage());
        }
    }

    /**
     * 获取连接池状态
     */
    public String getStatus() {
        return String.format("连接池状态 - 总连接数: %d, 活跃连接数: %d, 空闲连接数: %d",
                           totalConnections.get(), activeConnections.get(), idleConnections.size());
    }

    /**
     * 关闭连接池
     */
    public void shutdown() {
        shutdown = true;
        cleanupExecutor.shutdown();

        // 关闭所有连接
        PooledConnection conn;
        while ((conn = idleConnections.poll()) != null) {
            closeConnection(conn);
        }

        System.out.println("连接池已关闭");
    }
}
