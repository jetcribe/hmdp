package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.BlogComments;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogCommentsMapper;
import com.hmdp.service.IBlogCommentsService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import com.hmdp.dto.Result;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
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
public class BlogCommentsServiceImpl extends ServiceImpl<BlogCommentsMapper, BlogComments> implements IBlogCommentsService {
    @Resource
    private IUserService userService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public Result queryComments(Long blogId, Integer current) {
        // 1. 查询一级评论（parentId = 0）
        Page<BlogComments> page = lambdaQuery()
                .eq(BlogComments::getBlogId, blogId)
                .eq(BlogComments::getParentId, 0)
                .orderByDesc(BlogComments::getCreateTime)
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        
        List<BlogComments> records = page.getRecords();
        
        // 2. 查询每个一级评论的子评论
        for (BlogComments comment : records) {
            // 查询子评论
            List<BlogComments> children = lambdaQuery()
                    .eq(BlogComments::getParentId, comment.getId())
                    .orderByAsc(BlogComments::getCreateTime)
                    .list();
            
            // 设置子评论
            comment.setChildren(children);
            
            // 查询用户信息
            queryCommentUser(comment);
            children.forEach(this::queryCommentUser);
            
            // 判断当前用户是否点赞
            isCommentLiked(comment);
            children.forEach(this::isCommentLiked);
        }
        
        return Result.ok(records);
    }

    @Override
    public Result likeComment(Long commentId) {
        Long userId = UserHolder.getUser().getId();
        String key = RedisConstants.COMMENT_LIKED_KEY + commentId;
        
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        
        if (score == null) {
            // 点赞
            update().setSql("liked=liked+1").eq("id", commentId).update();
            stringRedisTemplate.opsForZSet().add(key, userId.toString(), System.currentTimeMillis());
        } else {
            // 取消点赞
            update().setSql("liked=liked-1").eq("id", commentId).update();
            stringRedisTemplate.opsForZSet().remove(key, userId.toString());
        }
        
        return Result.ok();
    }

    @Override
    public Result addComment(BlogComments comment) {
        UserDTO user = UserHolder.getUser();
        comment.setUserId(user.getId());
        boolean isSuccess = save(comment);
        if (!isSuccess) {
            return Result.fail("评论失败");
        }
        return Result.ok(comment);
    }

    @Override
    public Result deleteComment(Long commentId) {
        BlogComments comment = getById(commentId);
        if (comment == null) {
            return Result.fail("评论不存在");
        }
        UserDTO user = UserHolder.getUser();
        if (!comment.getUserId().equals(user.getId())) {
            return Result.fail("无权删除");
        }
        boolean isSuccess = removeById(commentId);
        if (!isSuccess) {
            return Result.fail("删除失败");
        }
        return Result.ok();
    }

    private void queryCommentUser(BlogComments comment) {
        Long userId = comment.getUserId();
        User user = userService.getById(userId);
        comment.setName(user.getNickName());
        comment.setIcon(user.getIcon());
    }

    private void isCommentLiked(BlogComments comment) {
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return;
        }
        Long userId = user.getId();
        String key = RedisConstants.COMMENT_LIKED_KEY + comment.getId();
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        comment.setIsLike(score != null);
    }

}
