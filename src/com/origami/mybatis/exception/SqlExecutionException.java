package com.origami.mybatis.exception;

/**
 * SQL执行异常
 */
public class SqlExecutionException extends RuntimeException {
    
    private final String sql;
    private final Object[] parameters;
    
    public SqlExecutionException(String sql, Object[] parameters, Throwable cause) {
        super(buildMessage(sql, parameters), cause);
        this.sql = sql;
        this.parameters = parameters;
    }
    
    private static String buildMessage(String sql, Object[] parameters) {
        StringBuilder sb = new StringBuilder();
        sb.append("SQL执行失败: ").append(sql);
        if (parameters != null && parameters.length > 0) {
            sb.append(", 参数: ");
            for (int i = 0; i < parameters.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(parameters[i]);
            }
        }
        return sb.toString();
    }
    
    public String getSql() {
        return sql;
    }
    
    public Object[] getParameters() {
        return parameters;
    }
}
