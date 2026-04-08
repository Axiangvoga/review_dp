package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.dto.ScrollResult;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.Follow;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IFollowService;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


import com.hmdp.utils.RedisConstants;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    @Resource
    private UserServiceImpl userService;
    @Resource
    private BlogServiceImpl blogService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private IFollowService followService;

    @Override
    public Result queryHoyBlog(Integer current) {
        // 根据用户查询
        Page<Blog> page = blogService.query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询用户
        records.forEach(blog -> {
            this.queryBlogUser(blog);
            this.blogIsLiked(blog);
        });
        return Result.ok(records);
    }

    @Override
    public Result quertByBlogId(Long id) {
        Blog blog = getById(id);
        if (blog == null) return Result.fail("博客不存在");
        queryBlogUser(blog);
        blogIsLiked(blog);
        return Result.ok(blog);
    }
    private void blogIsLiked(Blog blog){
        UserDTO user = UserHolder.getUser();
        if(user==null) return;
        Long userId = user.getId();
        String key = RedisConstants.BLOG_LIKED_KEY+userId;
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        blog.setIsLike(score!=null);
    }

    @Override
    public Result likeBlog(Long id) {
        Long userId = UserHolder.getUser().getId();
        String key = RedisConstants.BLOG_LIKED_KEY+id;
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        if(score==null){ //尚未点赞
            boolean isSuccess = update().setSql("liked = liked + 1").eq("id", id).update();
            if(BooleanUtil.isTrue(isSuccess)){
                stringRedisTemplate.opsForZSet().add(key,userId.toString(),System.currentTimeMillis());
            }
        }else{//取消点赞
            boolean isSuccess = update().setSql("liked = liked - 1").eq("id", id).update();
            if(BooleanUtil.isTrue(isSuccess)){
                stringRedisTemplate.opsForZSet().remove(key,userId.toString());
            }
        }
        return Result.ok();
    }

    @Override
    public Result queryBlogLikes(Long id) {
        String key = RedisConstants.BLOG_LIKED_KEY+id;
        Set<String> top5 = stringRedisTemplate.opsForZSet().range(key, 0, 4);
        if(top5==null|| top5.isEmpty()) return Result.ok(Collections.emptyList());
        List<Long> ids = top5.stream().map(Long::valueOf).collect(Collectors.toList());
        List<UserDTO> userDTOS = userService.listByIds(ids).stream()
                        .map(user -> BeanUtil.copyProperties(user,UserDTO.class))
                        .collect(Collectors.toList());
        //Collections.reverse(userDTOS);
        return Result.ok(userDTOS);
    }

    @Override
    public Result saveBlog(Blog blog) {
        UserDTO user = UserHolder.getUser();
        blog.setUserId(user.getId());
        boolean isSuccess = save(blog);
        if(!isSuccess){
            return Result.fail("新增失败");
        }
        List<Follow> follows = followService.query().eq("follow_user_id",user.getId()).list();
        for(Follow follow:follows){
            Long userId = follow.getUserId();
            String key = RedisConstants.FEED_KEY+userId;
            stringRedisTemplate.opsForZSet().add(key,blog.getId().toString(),System.currentTimeMillis());
        }
        return Result.ok(blog.getId());
    }

    @Override
    public Result queryBlogOfFollow(Long max, Integer offset) {
        Long userId = UserHolder.getUser().getId();
        String key =RedisConstants.FEED_KEY+userId;
        // 滚动分页查询 key max min limit offset count
        Set<ZSetOperations.TypedTuple<String>> typedTuples =
                stringRedisTemplate.opsForZSet().reverseRangeByScoreWithScores(key, 0, max, offset, 2);
        if(typedTuples==null||typedTuples.isEmpty()) return Result.ok();
        List<Long> ids  = new ArrayList<>(typedTuples.size());
        long minTime = 0;
        int os = 1;
        for(ZSetOperations.TypedTuple<String> tuple:typedTuples){
            ids.add(Long.valueOf(tuple.getValue()));
            os = minTime==tuple.getScore().longValue() ? os+1:1;
            minTime=tuple.getScore().longValue();
        }
        List<Blog> blogs = listByIds(ids);
        Collections.reverse(blogs);
        for(Blog blog:blogs){queryBlogUser(blog);blogIsLiked(blog);}
        ScrollResult sr = new ScrollResult();
        sr.setOffset(os);
        sr.setList(blogs);
        sr.setMinTime(minTime);
        return Result.ok(sr);
    }

    private void queryBlogUser(Blog blog) {
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }
}
