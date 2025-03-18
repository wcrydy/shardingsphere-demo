package com.qlan.shardingspheredemo.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

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