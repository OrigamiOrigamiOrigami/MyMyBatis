package com.origami.mybatis.handler;

import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 结果集映射器
 */
public class ResultSetMapper {


    /**
     * 通过反射实现 ResultSet 到 POJO 的自动映射。
     * 1. 遍历 ResultSet 的所有列。
     * 2. 获取列名（如 "create_time"），并将其转换为驼峰式命名（"createTime"）。
     * 3. 构造出对应的 setter 方法名（"setCreateTime"）。
     * 4. 使用 reflection API (obj.getClass().getMethod) 查找并调用该 setter 方法，将列值注入到 POJO 实例中。
     * 这种方式避免了硬编码的 `user.setName(rs.getString("name"))` 写法，实现了通用映射。
     */
    public Object mapResultSetToObject(ResultSet resultSet, Class<?> targetClass) throws Exception {
        if (targetClass == null) return null;
        Object obj = targetClass.newInstance();
        for (int i = 1; i <= resultSet.getMetaData().getColumnCount(); i++) {
            String columnName = resultSet.getMetaData().getColumnName(i);
            Object value = resultSet.getObject(columnName);
            if (value != null) {
                String propertyName = convertUnderscoreToCamelCase(columnName);
                String setMethodName = "set" + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
                try {
                    Method setMethod = obj.getClass().getMethod(setMethodName, value.getClass());
                    setMethod.invoke(obj, value);
                } catch (NoSuchMethodException e) {
                    // 如果找不到setter方法，则忽略
                }
            }
        }
        return obj;
    }

    /**
     * 将ResultSet映射为List
     */
    public List<Object> mapResultSetToList(ResultSet resultSet, Class<?> elementType) throws Exception {
        List<Object> list = new ArrayList<>();
        while (resultSet.next()) {
            Object obj = mapResultSetToObject(resultSet, elementType);
            list.add(obj);
        }
        return list;
    }

    /**
     * 将ResultSet映射为Map
     */
    public Map<String, Object> mapResultSetToMap(ResultSet resultSet) throws Exception {
        Map<String, Object> resultMap = new HashMap<>();
        for (int i = 1; i <= resultSet.getMetaData().getColumnCount(); i++) {
            String columnName = resultSet.getMetaData().getColumnName(i);
            Object value = resultSet.getObject(columnName);
            resultMap.put(columnName, value);
        }
        return resultMap;
    }

    /**
     * 将下划线命名转换为驼峰命名
     */
    private String convertUnderscoreToCamelCase(String columnName) {
        if (columnName == null || !columnName.contains("_")) {
            return columnName;
        }

        StringBuilder result = new StringBuilder();
        boolean nextUpperCase = false;

        for (char c : columnName.toCharArray()) {
            if (c == '_') {
                nextUpperCase = true;
            } else if (nextUpperCase) {
                result.append(Character.toUpperCase(c));
                nextUpperCase = false;
            } else {
                result.append(Character.toLowerCase(c));
            }
        }
        return result.toString();
    }
}

