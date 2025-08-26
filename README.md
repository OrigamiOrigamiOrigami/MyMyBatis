# MyMyBatis：企业级ORM框架

## 🚀 项目简介

MyMyBatis 是一个功能完整的企业级 ORM 框架，采用现代化的设计模式和架构。它不仅实现了 MyBatis 的核心功能，还提供了更加优雅的 API 设计和零依赖的高性能内存缓存机制。

### ✨ 核心特性
- **🏗️ Builder模式配置**: 流畅的链式API，告别繁琐的XML配置
- **🔄 JDK动态代理**: 无侵入式的Mapper接口代理实现
- **📝 注解驱动**: 支持 `@Select`, `@Insert`, `@Update`, `@Delete` 注解
- **🎯 智能映射**: 自动映射到 Object、List、Map 多种返回类型
- **💾 双级缓存**: 一级缓存(SqlSession级) + 内存二级缓存(跨Session)
- **🔗 连接池管理**: 高性能数据库连接池
- **⚡ 事务管理**: 完整的事务生命周期管理
- **🔍 反射映射**: 智能的ResultSet到对象映射

### 🛠️ 技术栈
- **核心语言**: Java 8+
- **数据库**: MySQL 8.0+
- **缓存**: 内存缓存(LRU+TTL)
- **连接池**: 自研高性能连接池
- **测试**: JUnit 4
- **序列化**: 自定义序列化工具

## 🚀 快速开始

### 📋 环境要求
- Java 8+
- MySQL 8.0+
- 无外部依赖，内置内存缓存

### ⚡ 5分钟上手

#### 1️⃣ 基础使用
```java
// 创建SqlSessionFactory
SqlSessionFactory factory = new SqlSessionFactoryBuilder().build();

// 获取SqlSession
SqlSession session = factory.openSession(false);
try {
    session.beginTransaction();
    
    // 获取Mapper代理
    AccountMapper mapper = session.getMapper(AccountMapper.class);
    
    // 执行查询
    Account account = mapper.selectAccount(1);
    System.out.println(account);
    
    session.commit();
} finally {
    session.close();
}
```

#### 2️⃣ Builder模式配置
```java
// 自定义配置
Configuration config = Configuration.builder()
    .database("jdbc.properties")
    .connectionPool(5, 20)  // 初始5个，最大20个连接
    .enableSecondLevelCache()  // 启用内存缓存
    .build();

SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(config);
```

#### 3️⃣ Mapper接口定义
```java
public interface AccountMapper {
    @Select("SELECT * FROM account WHERE id = ?")
    Account selectAccount(Integer id);
    
    @Insert("INSERT INTO account(name, money, create_time) VALUES(?, ?, ?)")
    int insertAccount(String name, BigDecimal money, String createTime);
    
    @Update("UPDATE account SET money = ? WHERE id = ?")
    int updateAccount(BigDecimal money, Integer id);
    
    @Delete("DELETE FROM account WHERE id = ?")
    int deleteAccount(Integer id);
}
```

## 5. 项目结构

为了实现“高内聚、低耦合”，我们将框架的不同职责拆分到了不同的包中：

-   `com.origami.mybatis.annotation`: 存放所有自定义的SQL注解，如 `@Select`。
-   `com.origami.mybatis.cache`: **缓存模块**。`CacheManager` 负责管理一级缓存。
-   `com.origami.mybatis.config`: **配置模块**。`Configuration` 负责加载 `jdbc.properties` 和初始化连接池。
-   `com.origami.mybatis.exception`: 存放自定义的异常类。
-   `com.origami.mybatis.executor`: **执行模块**。`SqlExecutor` 负责所有底层的JDBC操作。
-   `com.origami.mybatis.handler`: **处理模块**。`ResultSetMapper` 负责将 `ResultSet` 映射成 Java 对象。
-   `com.origami.mybatis.mapper`: 存放用户编写的 Mapper 接口。
-   `com.origami.mybatis.pojo`: 存放与数据库表对应的实体类 (POJO)。
-   `com.origami.mybatis.pool`: **连接池模块**。`ConnectionPool` 负责管理数据库连接。
-   `com.origami.mybatis.session`: **核心会话模块**。`SqlSession` 是用户与框架交互的顶层接口，它负责协调其他所有模块来完成一次数据库操作。

## 💾 缓存机制

### 🥇 一级缓存 (SqlSession级别)
- **生命周期**: 与SqlSession相同
- **作用域**: 单个SqlSession内
- **清理时机**: commit/rollback/close时清空

### 🥈 二级缓存 (内存)
- **生命周期**: 跨SqlSession持久化
- **作用域**: 全局共享，进程内
- **淘汰策略**: LRU + TTL过期机制
- **清理时机**: 写操作时清空，自动过期清理

```java
// 启用内存二级缓存
Configuration config = Configuration.builder()
    .database("jdbc.properties")
    .enableSecondLevelCache()  // 默认1000条目，30分钟过期
    // 或自定义参数
    .enableSecondLevelCache(2000, 60 * 60 * 1000L)  // 2000条目，1小时过期
    .build();
```

## ⚙️ 配置选项

### 📄 jdbc.properties
```properties
jdbc.driverClassName=com.mysql.cj.jdbc.Driver
jdbc.url=jdbc:mysql://localhost:3306/mybatis
jdbc.username=root
jdbc.password=123456

# 连接池配置
jdbc.initialSize=5
jdbc.maxSize=20
jdbc.maxIdleTime=300
jdbc.connectionTimeout=30
```

### 🔧 Builder配置
```java
Configuration config = Configuration.builder()
    // 数据库配置
    .database("jdbc.properties")
    // 或直接配置
    .database("jdbc:mysql://localhost:3306/mybatis", "root", "123456")
    
    // 连接池配置
    .connectionPool(5, 20)
    
    // 内存缓存配置
    .enableSecondLevelCache()  // 使用默认配置
    // 或自定义配置
    .enableSecondLevelCache(1000, 30 * 60 * 1000L)  // 最大1000条目，30分钟过期
    
    // 禁用缓存
    .disableCache()
    
    .build();
```

## 🧪 测试用例

项目提供了完整的测试套件，涵盖所有核心功能：

### 📋 测试列表
- `testBasicOperations()` - 基础CRUD操作
- `testBuilderConfiguration()` - Builder模式配置
- `testMemorySecondLevelCache()` - 内存二级缓存
- `testTransactionManagement()` - 事务管理

### 🗄️ 数据库准备
```sql
CREATE DATABASE mybatis;
USE mybatis;

CREATE TABLE `account` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `money` decimal(10,2) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB;

-- 插入测试数据
INSERT INTO account(name, money, create_time) VALUES
('张三', 1000.00, '2025-01-01 10:00:00'),
('李四', 2000.00, '2025-01-02 11:00:00'),
('王五', 3000.00, '2025-01-03 12:00:00');
```

### ▶️ 运行测试
```bash
# 编译项目
javac -cp "lib/*" -d out src/com/origami/mybatis/**/*.java

# 运行测试
java -cp "lib/*;out" org.junit.runner.JUnitCore com.origami.mybatis.test.MybatisTest
```

## 📚 最佳实践

### ✅ 推荐做法
- 使用Builder模式创建配置
- 及时关闭SqlSession资源
- 合理使用事务管理
- 启用内存二级缓存提升性能

### ❌ 避免事项
- 不要忘记提交事务
- 不要在finally块外关闭session
- 不要在高并发场景下禁用缓存

## 🤝 贡献指南

欢迎提交Issue和Pull Request！

## 📄 许可证

MIT License
