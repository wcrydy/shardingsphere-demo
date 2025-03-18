package com.qlan.shardingspheredemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qlan.shardingspheredemo.pojo.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author qlan
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
