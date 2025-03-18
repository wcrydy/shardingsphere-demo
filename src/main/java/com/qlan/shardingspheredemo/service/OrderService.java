package com.qlan.shardingspheredemo.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.qlan.shardingspheredemo.pojo.Order;

import java.util.List;

/**
 * @author qlan
 */
public interface OrderService extends IService<Order> {


    List<Order> listOrdersAndOrderItems();

}
