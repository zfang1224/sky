package com.sky.service;

import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.vo.UserLoginVO;

public interface UserService {

    // 微信登录
    User xvLogin(UserLoginDTO userLoginDTO);
}
