package com.qlan.shardingspheredemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qlan.shardingspheredemo.pojo.Order;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @author qlan
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {
    List<Order> selectOrdersAndOrderItems();

}