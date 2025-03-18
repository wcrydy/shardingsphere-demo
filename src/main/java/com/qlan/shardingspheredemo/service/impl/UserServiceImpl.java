package com.qlan.shardingspheredemo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qlan.shardingspheredemo.mapper.UserMapper;
import com.qlan.shardingspheredemo.pojo.User;
import com.qlan.shardingspheredemo.service.UserService;
import org.springframework.stereotype.Service;

/**
 * @author qlan
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
}
