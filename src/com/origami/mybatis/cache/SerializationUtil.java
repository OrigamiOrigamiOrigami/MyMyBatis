package com.origami.mybatis.cache;

import java.io.*;

/**
 * 序列化工具类，用于二级缓存对象的序列化和反序列化
 */
public class SerializationUtil {
    
    /**
     * 序列化对象为字节数组
     */
    public static void serialize(Object obj) {
        if (obj == null) {
            return;
        }
        
        // 检查对象是否实现了Serializable接口
        if (!(obj instanceof Serializable)) {
            throw new IllegalArgumentException("对象必须实现Serializable接口才能进行二级缓存: " + obj.getClass().getName());
        }
        
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(obj);
        } catch (IOException e) {
            throw new RuntimeException("序列化失败", e);
        }
    }
    
    /**
     * 反序列化字节数组为对象
     */
    public static Object deserialize(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            return ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("反序列化失败", e);
        }
    }
}
