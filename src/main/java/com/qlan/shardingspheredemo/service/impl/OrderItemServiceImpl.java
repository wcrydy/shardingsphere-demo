package com.qlan.shardingspheredemo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qlan.shardingspheredemo.mapper.OrderItemMapper;
import com.qlan.shardingspheredemo.pojo.OrderItem;
import com.qlan.shardingspheredemo.service.OrderItemService;
import org.springframework.stereotype.Service;

/**
 * @author qlan
 */
@Service
public class OrderItemServiceImpl extends ServiceImpl<OrderItemMapper, OrderItem> implements OrderItemService {
}
