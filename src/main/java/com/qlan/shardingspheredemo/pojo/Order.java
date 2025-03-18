package com.qlan.shardingspheredemo.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import lombok.Data;

import java.util.List;

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