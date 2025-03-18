package com.qlan.shardingspheredemo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qlan.shardingspheredemo.mapper.OrderMapper;
import com.qlan.shardingspheredemo.pojo.Order;
import com.qlan.shardingspheredemo.service.OrderService;
import org.springframework.stereotype.Service;

import java.util.List;

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
