# shardingsphere demo演示文档

demo地址：https://github.com/wcrydy/shardingsphere-demo.git

Official Website:：https://github.com/apache/shardingsphere.git

###  一、主从复制

#### 1、配置主

``` shell
docker run -d \
-p 10080:3306 \
-v /qlan/mysql/user/master/conf:/etc/mysql/conf.d \
-v /qlan/mysql/user/master/data:/var/lib/mysql \
-e MYSQL_ROOT_PASSWORD=root \
--name mysql-user \
mysql:8.1.0

vim /qlan/mysql/user/master/conf/my.cnf

# 给予下面的配置
[mysqld]

# 服务器唯一id，默认值1
server-id=1
# 设置日志格式，默认值ROW
binlog_format=STATEMENT
# 二进制日志名，默认binlog
# log-bin=binlog
# 设置需要复制的数据库，默认复制全部数据库
#binlog-do-db=mytestdb
# 设置不需要复制的数据库
#binlog-ignore-db=mysql
#binlog-ignore-db=infomation_schema

```



`binlog格式说明：`

- binlog_format=STATEMENT：日志记录的是主机数据库的`写指令`，性能高，但是now()之类的函数以及获取系统参数的操作会出现主从数据不同步的问题。
- binlog_format=ROW（默认）：日志记录的是主机数据库的`写后的数据`，批量操作时性能较差，解决now()或者  user()或者  @@hostname 等操作在主从机器上不一致的问题。
- binlog_format=MIXED：是以上两种level的混合使用，有函数用ROW，没函数用STATEMENT，但是无法识别系统变量

``` shell
给主创建用户
docker exec -it mysql-user /bin/bash
-- 创建slave用户
CREATE USER 'qlan_user_slave'@'%';
-- 设置密码
ALTER USER 'qlan_user_slave'@'%' IDENTIFIED WITH mysql_native_password BY 'root';
-- 授予复制权限
GRANT REPLICATION SLAVE ON *.* TO 'qlan_user_slave'@'%';
-- 刷新权限
FLUSH PRIVILEGES;

-- 查看主的状态
SHOW MASTER STATUS;
```

#### 2、配置从

``` shell
docker run -d \
-p 10090:3306 \
-v /qlan/mysql/user/slave0/conf:/etc/mysql/conf.d \
-v /qlan/mysql/user/slave0/data:/var/lib/mysql \
-e MYSQL_ROOT_PASSWORD=root \
--name mysql-user-slave0 \
mysql:8.1.0

vim /qlan/mysql/user/slave0/conf/my.cnf

# 给予下面的配置
[mysqld]
# 服务器唯一id，每台id不同，注意修改
server-id=2
# 中继日志名，默认xxxxxxxxxxxx-relay-bin
#relay-log=relay-bin

-- 进入从机 执行slq配置
CHANGE MASTER TO MASTER_HOST='192.168.200.25', 
MASTER_USER='qlan_user_slave',MASTER_PASSWORD='root', MASTER_PORT=10080,
MASTER_LOG_FILE='binlog.000003',MASTER_LOG_POS=1081; 

-- 启动主从复制
START SLAVE;
-- 查看状态
SHOW SLAVE STATUS\G

```

-- 部分slq

```sql
-- user 
CREATE DATABASE db_user;
USE db_user;
CREATE TABLE t_user (
                        id BIGINT AUTO_INCREMENT,
                        uname VARCHAR(30),
                        PRIMARY KEY (id)
);

```

### 二、分库、分表

#### 1、数据库搭建

```shell
docker run -d \
-p 10081:3306 \
-v /qlan/mysql/order0/conf:/etc/mysql/conf.d \
-v /qlan/mysql/order0/data:/var/lib/mysql \
-e MYSQL_ROOT_PASSWORD=root \
--name mysql-order0 \
mysql:8.1.0


docker run -d \
-p 10082:3306 \
-v /qlan/mysql/order1/conf:/etc/mysql/conf.d \
-v /qlan/mysql/order1/data:/var/lib/mysql \
-e MYSQL_ROOT_PASSWORD=root \
--name mysql-order1 \
mysql:8.1.0
```



-- 部分slq

```sql
-- order
CREATE DATABASE db_order;
USE db_order;
CREATE TABLE t_order0 (
                          id BIGINT,
                          order_no VARCHAR(30),
                          user_id BIGINT,
                          PRIMARY KEY(id)
);
CREATE TABLE t_order1 (
                          id BIGINT,
                          order_no VARCHAR(30),
                          user_id BIGINT,
                          PRIMARY KEY(id)
);
CREATE TABLE t_order_item0(
                              id BIGINT,
                              user_id BIGINT,
                              order_id BIGINT,
                              price DECIMAL(10,2),
                              `count` INT,
                              PRIMARY KEY(id)
);

CREATE TABLE t_order_item1(
                              id BIGINT,
                              user_id BIGINT,
                              order_id BIGINT,
                              price DECIMAL(10,2),
                              `count` INT,
                              PRIMARY KEY(id)
);
```

#### 2、代码

##### 2.1 构建springboot项目  相关依赖

```xml
<!--springboot version3.0.5 -->

<dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>org.apache.shardingsphere</groupId>
            <artifactId>shardingsphere-jdbc-core</artifactId>
            <version>5.4.0</version>
        </dependency>

        <!--兼容jdk17 和 spring boot3-->
        <dependency>
            <groupId>org.yaml</groupId>
            <artifactId>snakeyaml</artifactId>
            <version>1.33</version>
        </dependency>
        <dependency>
            <groupId>org.glassfish.jaxb</groupId>
            <artifactId>jaxb-runtime</artifactId>
            <version>2.3.8</version>
        </dependency>

        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <version>8.0.30</version>
        </dependency>

        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-boot-starter</artifactId>
            <version>3.5.3.1</version>
        </dependency>

        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>
```

##### 2.2 mapper

```java
/**
 * @author qlan
 */
@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {
}
```

```java
/**
 * @author qlan
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {
    List<Order> selectOrdersAndOrderItems();

}
```

```java
/**
 * @author qlan
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
```



```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.qlan.shardingspheredemo.mapper.OrderMapper">
    <resultMap id="selectOrdersAndOrderItemsResultMap" autoMapping="true" type="com.qlan.shardingspheredemo.pojo.Order">
        <id property="id" column="id"></id>
        <collection property="orderItemList"
                    ofType="com.qlan.shardingspheredemo.pojo.OrderItem">
            <id property="id" column="order_item_id"></id>
            <result column="price" property="price"></result>
            <result column="order_id" property="orderId"></result>
            <result column="count" property="count"></result>
        </collection>
    </resultMap>
    <!-- 自定义sql的表名需要和 sharding 定义的逻辑表名一样 -->
    <select id="selectOrdersAndOrderItems" resultMap="selectOrdersAndOrderItemsResultMap">
        SELECT t1.id,
               t1.order_no,
               t1.user_id,
               t2.id order_item_id,
               t2.`order_id`,
               t2.`price`,
               t2.`count`
        FROM t_order t1
                 LEFT JOIN t_order_item t2
                           ON t1.`id` = t2.`order_id`
    </select>
</mapper>

```



##### 2.3 pojo

```java
/**
 * @author qlan
 */
@TableName("t_order")
@Data
public class Order {
    // 水平分片必须在代码中生成id：用来判断使用哪张分片表
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String orderNo;
    private Long userId;

    @TableField(exist = false)
    private List<OrderItem> orderItemList;
}
```

```java
/**
 * @author qlan
 */
@TableName("t_order_item")
@Data
public class OrderItem {

    @TableId(type = IdType.ASSIGN_ID) //分布式id
    private Long id;
    private Long orderId;
    private Long userId;
    private BigDecimal price;
    private Integer count;
}
```

```java
/**
 * @author qlan
 */
@Data
@TableName("t_user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String uname;
}
```



##### 2.4 service

```java
/**
 * @author qlan
 */
public interface OrderItemService extends IService<OrderItem> {
}

```

```java
/**
 * @author qlan
 */
public interface OrderService extends IService<Order> {


    List<Order> listOrdersAndOrderItems();

}
```

```java
/**
 * @author qlan
 */
public interface UserService extends IService<User> {
}
```

-- Impl

```java
/**
 * @author qlan
 */
@Service
public class OrderItemServiceImpl extends ServiceImpl<OrderItemMapper, OrderItem> implements OrderItemService {
}

```

```java
/**
 * @author qlan
 */
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {
    @Override
    public List<Order> listOrdersAndOrderItems() {
        return baseMapper.selectOrdersAndOrderItems();
    }
}
```

```java
/**
 * @author qlan
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
}
```



##### 2.5 test

``` java

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@SpringBootTest
class ShardingsphereDemoApplicationTests {


    @Resource
    UserService userService;
    // 垂直分库：
    @Resource
    OrderService orderService;
    @Resource
    OrderItemService orderItemService;

    // 测试读
    @Test
    void contextLoads() {
        User user = userService.getById(1);
        System.out.println("1-" + user);
        user = userService.getById(1);
        System.out.println("2-" + user);

        user = userService.getById(1);
        System.out.println("3-" + user);
        user = userService.getById(1);
        System.out.println("4-" + user);
        user = userService.getById(1);
        System.out.println("5-" + user);
        user = userService.getById(1);
        System.out.println("6-" + user);
    }

    @Test
    void test2() {
        User user = new User();
        user.setId(2L);
        user.setUname("lisi");

        userService.save(user);

        System.out.println(userService.list());
        System.out.println(userService.list());
        System.out.println(userService.list());

    }

    @Test
    void test3() {
        Order order = new Order();
        order.setId(1L);
//        order.setId(2L);
        order.setOrderNo("11111");
        order.setUserId(1L);
        orderService.save(order);

        System.out.println(orderService.list());


        System.out.println(userService.list());
        System.out.println(userService.list());
        System.out.println(userService.list());
        System.out.println(userService.list());
    }

    //水平分片测试
    @Test
    void test4() {
        // 查询用户的订单集合：订单根据用户id分库了
        for (int i = 0; i < 30; i++) {
            Order order = new Order();
            order.setOrderNo(UUID.randomUUID().toString().substring(0, 20));
            order.setUserId(i * 100 / 3L);
            orderService.save(order);
        }

        System.out.println(orderService.list());
    }

    @Test
    public void test05() {

        orderService.list().forEach(order -> {
            OrderItem orderItem = new OrderItem();
            orderItem.setCount(new Random().nextInt(100));
            orderItem.setPrice(new BigDecimal(new Random().nextInt(2000)));
            orderItem.setUserId(order.getUserId());
            orderItem.setOrderId(order.getId());
            orderItemService.save(orderItem);
        });

        // 订单和订单详情：同时查询订单及订单的详情： 测试跨库join查询
        List<Order> orderList = orderService.listOrdersAndOrderItems();

        orderList.forEach(System.out::println);
    }

}

```

##### 2.6 配置

``` yaml
spring:
  datasource:
    # 表示使用sharding驱动类连接数据库
    driver-class-name: org.apache.shardingsphere.driver.ShardingSphereDriver
    # 配置使用shardinng的配置文件加载数据库连接
    url: jdbc:shardingsphere:classpath:shardingsphere.yaml

```

```yaml
mode:
  type: Standalone
  repository:
    # 使用jdbc连接数据库
    type: JDBC
# sharding的数据源配置
dataSources:
  # 写数据源配置： 主库配置
  write_ds_0:
    # 当前数据源连接池类型
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource
    # mysql连接参数
    driverClassName: com.mysql.cj.jdbc.Driver
    jdbcUrl: jdbc:mysql://192.168.200.25:10080/db_user
    username: root
    password: root
  # 读数据源配置
  read_ds_0:
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource
    driverClassName: com.mysql.cj.jdbc.Driver
    jdbcUrl: jdbc:mysql://192.168.200.25:10080/db_user
    username: root
    password: root
  read_ds_1:
    # 当前数据源连接池类型
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource
    # mysql连接参数
    driverClassName: com.mysql.cj.jdbc.Driver
    jdbcUrl: jdbc:mysql://192.168.200.25:10090/db_user
    username: root
    password: root
  # 订单库数据源配置
  order_ds_0:
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource
    driverClassName: com.mysql.cj.jdbc.Driver
    jdbcUrl: jdbc:mysql://192.168.200.25:10081/db_order
    username: root
    password: root
  # order_ds 和order_ds_1 两个数据源 都创建了db_order库 库中都准备了t_order0~1 两张表
  # 一共四张表 用来分库分表存储订单数据
  order_ds_1:
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource
    driverClassName: com.mysql.cj.jdbc.Driver
    jdbcUrl: jdbc:mysql://192.168.200.25:10082/db_order
    username: root
    password: root
# 规则配置： 读写分离、分库、分表
rules:
  # 读写分离配置规则
  - !READWRITE_SPLITTING
    dataSources:
      # 读写数据源配置
      readwrite_ds:
        # 写数据源配置，使用上面dataSources定义的数据源名称
        writeDataSourceName: write_ds_0
        # 读数据源配置，使用上面dataSources定义的数据源名称
        readDataSourceNames:
          - read_ds_1
          - read_ds_0
        transactionalReadQueryStrategy: PRIMARY # 事务内读请求的路由策略，可选值：PRIMARY（路由至主库）、FIXED（同一事务内路由至固定数据源）、DYNAMIC（同一事务内路由至非固定数据源）。默认值：DYNAMIC
        # 负载均衡策略：使用的是loadBalancers下定义的负载均衡策略
        loadBalancerName: round_robin
    # 定义读的负载均衡策略
    loadBalancers:
      # 随机
      random2:
        type: RANDOM
      # 轮询策略
      round_robin:
        type: ROUND_ROBIN
      # 加权轮询
      weight:
        type: WEIGHT
        props:
          read_ds_1: 2
          read_ds_0: 1
  # 垂直分片配置(分库：项目可以从多个不同的数据库中加载到不同的表在一个项目中使用)
  - !SHARDING
    tables:
      # 表示代码中生成的sql要操作的数据库表名
      t_user:
        # 表示t_user实际要到sharding上面定义的哪个数据源中查找该表
        actualDataNodes: readwrite_ds.t_user
      # 表示代码中生成的sql要操作的t_order表到order_ds数据源连接的数据库中 查找t_order表
      #      t_order:
      #        actualDataNodes: order_ds.t_order
      t_order:
        # inline写法
        # 实际要访问的数据库表，一定要保证 数据源连接的数据库中存在对应的表
        actualDataNodes: order_ds_${0..1}.t_order${0..1}
        #  t_order表的查询的分库策略：基于user_id判断查找哪个库
        databaseStrategy:
          standard:
            # 分库列字段
            shardingColumn: user_id
            # 选择使用的分库算法： 配置shardingAlgorithms指定的算法
            shardingAlgorithmName: userid_inline
        # t_order表查询的 分表策略：判断在上面的库中查找哪张表
        tableStrategy:
          standard:
            shardingColumn: id
            shardingAlgorithmName: orderid_inline
      t_order_item:
        actualDataNodes: order_ds_${0..1}.t_order_item${0..1}
        databaseStrategy:
          standard:
            shardingColumn: user_id
            shardingAlgorithmName: userid_inline
        tableStrategy:
          standard:
            # 虽然order_item分表策略和order表一样，但是使用的字段名不一样
            shardingColumn: order_id
            shardingAlgorithmName: orderid_item_inline
    # 分库算法：
    shardingAlgorithms:
      userid_inline:
        type: INLINE
        props:
          # 使用user_id对2取余 计算的结果和 order_ds_拼接出一个库名(order_ds_0 , order_ds_1)
          algorithm-expression: order_ds_${user_id % 2}
      orderid_inline:
        type: INLINE
        props:
          # 使用id对2取余 计算的结果和 t_order拼接出一个表名(t_order0 , t_order1)
          algorithm-expression: t_order${id % 2}
      orderid_item_inline:
        type: INLINE
        props:
          algorithm-expression: t_order_item${order_id % 2}
# 打印sharding的sql语句
props:
  sql-show: true
```





