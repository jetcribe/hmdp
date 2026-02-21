package com.hmdp.controller;

import com.hmdp.dto.Result;
import com.hmdp.entity.BlogComments;
import com.hmdp.service.IBlogCommentsService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/blog-comments")
public class BlogCommentsController {
    
    @Resource
    private IBlogCommentsService commentService;
    
    @PostMapping
    public Result addComment(@RequestBody BlogComments comment) {
        return commentService.addComment(comment);
    }
    
    @GetMapping("/list/{blogId}")
    public Result queryComments(@PathVariable("blogId") Long blogId,
                                @RequestParam(value = "current", defaultValue = "1") Integer current) {
        return commentService.queryComments(blogId, current);
    }
    
    @PutMapping("/like/{id}")
    public Result likeComment(@PathVariable("id") Long id) {
        return commentService.likeComment(id);
    }
    
    @DeleteMapping("/{id}")
    public Result deleteComment(@PathVariable("id") Long id) {
        return commentService.deleteComment(id);
    }
}
