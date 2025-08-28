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

            int updateResult = mapper.updateAccount(BigDecimal.valueOf(5000), 1);
            System.out.println("更新记录数: " + updateResult);

            int deleteResult = mapper.deleteAccount(1);
            System.out.println("删除记录数: " + deleteResult);

            // 测试查询操作
            try {
                Account account = mapper.selectAccount(3);
                System.out.println("查询单条记录: " + account);
            }catch (Exception ignored){}

            try {
                List<Account> accounts = mapper.selectAccountsByNameAndMoney("测试用户", BigDecimal.valueOf(4999));
                System.out.println("查询结果: " + accounts);
            } catch (Exception ignored) {}

            List<Account> accounts = mapper.selectAccounts();
            for (Account account : accounts) {
                System.out.println("查询结果: " + account);
            }

            Map<String, Object> accountMap = mapper.selectAccountAsMap(3);
            System.out.println("查询结果为Map: " + accountMap);

            // 提交事务
            sqlSession.commit();
            System.out.println("\n基础功能测试完成");

        } catch (Exception e) {
            System.err.println("测试失败，事务回滚");
            sqlSession.rollback();
            e.printStackTrace();
        } finally {
            sqlSession.close();
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
                Account account1 = mapper1.selectAccount(3);
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
                Account account2 = mapper2.selectAccount(3);
                System.out.println("Session2查询结果: " + account2);
                session2.commit();
            } finally {
                session2.close();
            }
            
            System.out.println("内存二级缓存测试完成");
            
        } catch (Exception e) {
            System.err.println("内存缓存测试失败: " + e.getMessage());
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
            
            //模拟异常情况
            int i = 1 / 0;

            session.commit();
            System.out.println("事务提交成功");
            
        } catch (Exception e) {
            System.err.println("发生异常，事务回滚");
            session.rollback();
        } finally {
            session.close();
        }
    }
}
