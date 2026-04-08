package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public List<ShopType> queryShopType() {
/*
        String key = "cache:shopType";
        List<String> shopType = new ArrayList<>();
        shopType = stringRedisTemplate.opsForList().range(key, 0, -1);
        if(!shopType.isEmpty()){ // redis 中有缓存
            List<ShopType> shopTypeList = new ArrayList<>();
            for(String s:shopType){
                ShopType type = JSONUtil.toBean(s, ShopType.class);
                shopTypeList.add(type);
            }
            return Result.ok(shopTypeList);
        } // 缓存中没有查询数据库
        List<ShopType> shopTypes = query().orderByAsc("sort").list();
        if(shopTypes.isEmpty())  return Result.fail("当前分类不存在");
        for(ShopType shop:shopTypes){
            String s = JSONUtil.toJsonStr(shop);
            shopType.add(s);
        }
        stringRedisTemplate.opsForList().rightPushAll(key,shopType);
        return Result.ok(shopTypes);
        */
        String key = "cache:shop-type";
        String jsonList = stringRedisTemplate.opsForValue().get(key);
        if(StrUtil.isNotBlank(jsonList)){
            List<ShopType> shopTypeList = JSONUtil.toList(jsonList, ShopType.class);
            // return Result.ok(shopTypeList);
            return shopTypeList;
        }
        List<ShopType> shopTypes = query().orderByAsc("sort").list();
        // if(shopTypes.isEmpty())  return null;// return Result.fail("当前分类不存在");
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shopTypes));
        return shopTypes;
    }
}
