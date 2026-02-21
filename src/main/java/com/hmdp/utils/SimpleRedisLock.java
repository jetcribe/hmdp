package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * 简单的redis分布式锁
 * 可以锁住多个服务器上的线程,保证分布式环境下的互斥访问
 * 分布式锁特征：1.锁存储在Redis中，所有服务器共享；
 * 2.使用SET NX EX命令实现互斥（NX: 只在key不存在时设置（互斥），EX 10: 设置过期时间10秒（防止死锁））；
 * 3.使用线程标识防止误删（每个线程有唯一的标识，防止误删其他线程的锁）
 * 4.使用Lua脚本释放锁，保证原子性
 * 常用场境：秒杀商品、缓存重建
 * @author CHEN
 * @date 2022/10/09
 */
public class SimpleRedisLock implements ILock {
    private String name;
    private StringRedisTemplate stringRedisTemplate;
    //锁的key前缀
    private static final String KET_PREFIX="lock:";
    //线程标识前缀
    private static final String ID_PREFIX= UUID.randomUUID().toString(true)+"-";
    //lua脚本 释放锁 - 避免误删其他线程的锁，保证原子性
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;

    static {
        UNLOCK_SCRIPT=new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean tryLock(Long timeoutSec) {
        String threadId =ID_PREFIX+ Thread.currentThread().getId();
        //获取锁
        Boolean isSuccess = stringRedisTemplate
                .opsForValue()
                .setIfAbsent(KET_PREFIX + name
                        , threadId
                        , timeoutSec
                        , TimeUnit.SECONDS);
        //避免自动拆箱引发空指针异常
        return Boolean.TRUE.equals(isSuccess);
    }

    @Override
    public void unLock() {
        /*//获取线程标识
        String threadId =ID_PREFIX+ Thread.currentThread().getId();
        //获取锁中标识
        String id = stringRedisTemplate.opsForValue().get(KET_PREFIX + name);
        //判断时候一致
        if (StringUtils.equals(id,threadId)){
            //一致 释放锁
            stringRedisTemplate.delete(KET_PREFIX + name);
        }*/
        //使用lua脚本保证操作原子性
        //整个Lua脚本作为一个原子操作执行在执行过程中，不会被其他命令打断
        stringRedisTemplate.execute(UNLOCK_SCRIPT
                , Collections.singletonList(KET_PREFIX+name)
                ,ID_PREFIX+Thread.currentThread().getId());
    }
}
