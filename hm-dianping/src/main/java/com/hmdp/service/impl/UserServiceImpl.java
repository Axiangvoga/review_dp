package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;
import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private RedisTemplate redisTemplate;

    @Override
    public Result sendCode(String phone, HttpSession session) {
//        if(RegexUtils.isPhoneInvalid(phone)){
//            return Result.fail("手机号格式错误！");
//        }
//        String code = RandomUtil.randomNumbers(6);
//        redisTemplate.opsForValue().set(LOGIN_CODE_KEY+phone,code,LOGIN_CODE_TTL, TimeUnit.MINUTES);
//        log.debug("发送验证码成功，验证码：{}",code);
//        return Result.ok();
        // 1. 校验手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误！");
        }

        // 2. 【新增】滑动窗口限流校验
        String limitKey = "limit:code:" + phone;
        long now = System.currentTimeMillis();
        long windowSize = 60 * 1000L; // 窗口大小：60秒

        // (1) 清除窗口之外的历史记录（只保留最近60秒的）
        redisTemplate.opsForZSet().removeRangeByScore(limitKey, 0, now - windowSize);

        // (2) 统计当前窗口内的发送次数
        Long count = redisTemplate.opsForZSet().zCard(limitKey);
        if (count != null && count >= 1) { // 阈值设为1次
            return Result.fail("验证码发送太频繁，请稍后再试");
        }

        // 3. 生成验证码
        String code = RandomUtil.randomNumbers(6);

        // 4. 保存到 Redis (TTL 2分钟)
        redisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);

        // 5. 【新增】记录本次发送时间戳到限流 ZSet
        redisTemplate.opsForZSet().add(limitKey, String.valueOf(now), now);
        // 设置限流 Key 的过期时间，防止冷数据堆积
        redisTemplate.expire(limitKey, 2, TimeUnit.MINUTES);

        log.debug("发送验证码成功，验证码：{}", code);
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        String phone = loginForm.getPhone();
        if(RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机号格式错误！");
        }
        String cacheCode = (String) redisTemplate.opsForValue().get(LOGIN_CODE_KEY+phone);
        String code = loginForm.getCode();
        if(cacheCode==null||!cacheCode.equals(code)){
            return Result.fail("验证码错误");
        }
        User user = query().eq("phone", phone).one();
        if(user==null){
            user = creatUserWithPhone(phone);
        }
        String token = UUID.randomUUID().toString();
        UserDTO userDTO = BeanUtil.copyProperties(user,UserDTO.class);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO);
        redisTemplate.opsForHash().putAll(LOGIN_USER_KEY +token,userMap);
        redisTemplate.expire(LOGIN_USER_KEY+token,30,TimeUnit.MINUTES);
        return Result.ok(token);
    }

    private User creatUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX+RandomUtil.randomString(10));
        save(user);
        return user;
    }

}
