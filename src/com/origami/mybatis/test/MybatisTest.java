package com.origami.mybatis.test;

import com.origami.mybatis.config.Configuration;
import com.origami.mybatis.mapper.AccountMapper;
import com.origami.mybatis.pojo.Account;
import com.origami.mybatis.session.SqlSession;
import com.origami.mybatis.session.SqlSessionFactory;
import com.origami.mybatis.session.SqlSessionFactoryBuilder;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class MybatisTest {

    @Test
    public void testBasicOperations() {
        System.out.println("=== 基础功能测试 ===");
        
        // 使用Builder模式创建SqlSessionFactory
        SqlSessionFactory factory = new SqlSessionFactoryBuilder().build();
        
        SqlSession sqlSession = factory.openSession(false); // 手动提交模式
        try {
            sqlSession.beginTransaction();
            AccountMapper mapper = sqlSession.getMapper(AccountMapper.class);

            // 测试一级缓存
            System.out.println("\n1. 测试一级缓存:");
            Integer count1 = mapper.countAccountsByMoney(BigDecimal.valueOf(1000));
            System.out.println("第一次查询数量: " + count1);

            Integer count2 = mapper.countAccountsByMoney(BigDecimal.valueOf(1000));
            System.out.println("第二次查询数量: " + count2);

            // 测试CRUD操作
            System.out.println("\n2. 测试CRUD操作:");
            int insertResult = mapper.insertAccount("测试用户", BigDecimal.valueOf(5000), "2025-08-26 18:00:00");
            System.out.println("插入记录数: " + insertResult);

            Account account = mapper.selectAccount(1);
            System.out.println("查询单条记录: " + account);

            List<Account> accounts = mapper.selectAccounts();
            System.out.println("查询所有记录数量: " + accounts.size());

            Map<String, Object> accountMap = mapper.selectAccountAsMap(1);
            System.out.println("查询结果为Map: " + accountMap);

            // 提交事务
            sqlSession.commit();
            System.out.println("\n✅ 基础功能测试完成");

        } catch (Exception e) {
            System.err.println("❌ 测试失败，事务回滚");
            sqlSession.rollback();
            e.printStackTrace();
        } finally {
            sqlSession.close();
        }
    }

    /**
     * 测试Builder模式配置
     */
    @Test
    public void testBuilderConfiguration() {
        System.out.println("=== Builder模式配置测试 ===");
        
        // 使用Builder模式创建自定义配置
        Configuration config = Configuration.builder()
            .database("jdbc.properties")
            .connectionPool(3, 8)  // 自定义连接池大小
            .build();
        
        SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(config);
        
        SqlSession session = factory.openSession(false);
        try {
            session.beginTransaction();
            AccountMapper mapper = session.getMapper(AccountMapper.class);
            
            Account account = mapper.selectAccount(1);
            System.out.println("使用自定义配置查询: " + account);
            
            session.commit();
            System.out.println("✅ Builder模式配置测试完成");
            
        } catch (Exception e) {
            System.err.println("❌ 配置测试失败");
            session.rollback();
            e.printStackTrace();
        } finally {
            session.close();
        }
    }

    /**
     * 测试内存二级缓存
     */
    @Test
    public void testMemorySecondLevelCache() {
        System.out.println("=== 内存二级缓存测试 ===");
        
        try {
            // 创建带内存二级缓存的配置
            Configuration config = Configuration.builder()
                .database("jdbc.properties")
                .enableSecondLevelCache()
                .build();
            
            SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(config);
            
            // 第一个Session查询
            System.out.println("\n1. 第一个Session查询:");
            SqlSession session1 = factory.openSession(false);
            try {
                session1.beginTransaction();
                AccountMapper mapper1 = session1.getMapper(AccountMapper.class);
                Account account1 = mapper1.selectAccount(1);
                System.out.println("Session1查询结果: " + account1);
                session1.commit();
            } finally {
                session1.close();
            }
            
            // 第二个Session查询（应该命中二级缓存）
            System.out.println("\n2. 第二个Session查询（测试二级缓存）:");
            SqlSession session2 = factory.openSession(false);
            try {
                session2.beginTransaction();
                AccountMapper mapper2 = session2.getMapper(AccountMapper.class);
                Account account2 = mapper2.selectAccount(1);
                System.out.println("Session2查询结果: " + account2);
                session2.commit();
            } finally {
                session2.close();
            }
            
            System.out.println("✅ Redis二级缓存测试完成");
            
        } catch (Exception e) {
            System.err.println("❌ Redis缓存测试失败: " + e.getMessage());
            System.err.println("请确保Redis服务已启动");
        }
    }

    /**
     * 测试事务管理
     */
    @Test
    public void testTransactionManagement() {
        System.out.println("=== 事务管理测试 ===");
        
        SqlSessionFactory factory = new SqlSessionFactoryBuilder().build();
        SqlSession session = factory.openSession(false);
        
        try {
            session.beginTransaction();
            AccountMapper mapper = session.getMapper(AccountMapper.class);
            
            // 插入测试数据
            int insertResult = mapper.insertAccount("事务测试", BigDecimal.valueOf(1000), "2025-08-26 18:00:00");
            System.out.println("插入记录数: " + insertResult);
            
            // 模拟异常情况
            // throw new RuntimeException("模拟异常");
            
            session.commit();
            System.out.println("✅ 事务提交成功");
            
        } catch (Exception e) {
            System.err.println("❌ 发生异常，事务回滚");
            session.rollback();
        } finally {
            session.close();
        }
    }
}
