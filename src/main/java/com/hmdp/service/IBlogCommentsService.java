package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.entity.BlogComments;
import com.hmdp.dto.Result;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IBlogCommentsService extends IService<BlogComments> {
    
    /**
     * 添加评论
     * @param comment 评论内容
     */
    Result addComment(BlogComments comment);
    
    /**
     * 查询博客的评论列表
     */
    Result queryComments(Long blogId, Integer current);
    
    /**
     * 点赞评论
     */
    Result likeComment(Long commentId);
    
    /**
     * 删除评论
     */
    Result deleteComment(Long commentId);
}
