package com.origami.mybatis.executor;

import com.origami.mybatis.exception.SqlExecutionException;
import com.origami.mybatis.handler.ResultSetHandler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * SQL执行器
 */
public class SqlExecutor {

    public <T> T executeQuery(Connection connection, String sql, Object[] args, ResultSetHandler<T> handler) {
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            st = connection.prepareStatement(sql);
            if (args != null) {
                for (int i = 0; i < args.length; i++) {
                    st.setObject(i + 1, args[i]);
                }
            }
            rs = st.executeQuery();
            return handler.handle(rs);
        } catch (Exception e) {
            throw new SqlExecutionException(sql, args, e);
        } finally {
            try {
                if (rs != null) rs.close();
                if (st != null) st.close();
            } catch (Exception e) {
                System.err.println("关闭资源失败: " + e.getMessage());
            }
        }
    }

    public int executeUpdate(Connection connection, String sql, Object[] args) {
        PreparedStatement st = null;
        try {
            st = connection.prepareStatement(sql);
            if (args != null) {
                for (int i = 0; i < args.length; i++) {
                    st.setObject(i + 1, args[i]);
                }
            }
            return st.executeUpdate();
        } catch (Exception e) {
            throw new SqlExecutionException(sql, args, e);
        } finally {
            try {
                if (st != null) st.close();
            } catch (Exception e) {
                System.err.println("关闭资源失败: " + e.getMessage());
            }
        }
    }
}

