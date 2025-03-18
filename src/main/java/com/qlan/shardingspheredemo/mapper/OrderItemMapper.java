package com.qlan.shardingspheredemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qlan.shardingspheredemo.pojo.OrderItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author qlan
 */
@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {
}