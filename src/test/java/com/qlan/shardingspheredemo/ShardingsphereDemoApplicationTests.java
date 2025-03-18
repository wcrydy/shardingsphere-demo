package com.qlan.shardingspheredemo;

import com.qlan.shardingspheredemo.pojo.Order;
import com.qlan.shardingspheredemo.pojo.OrderItem;
import com.qlan.shardingspheredemo.pojo.User;
import com.qlan.shardingspheredemo.service.OrderItemService;
import com.qlan.shardingspheredemo.service.OrderService;
import com.qlan.shardingspheredemo.service.UserService;
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
