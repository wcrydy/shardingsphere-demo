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

##### 2.1 构建springboot项目

demo地址：https://github.com/wcrydy/shardingsphere-demo.git

Official Website:：https://github.com/apache/shardingsphere.git




