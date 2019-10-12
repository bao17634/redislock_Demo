package com.byr.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Collections;

/**
 * @ClassName: RedisLockLUA
 * @Description: 单节点分布式锁，锁不具有重入性
 * @Author: yanrong
 * @Date: 2019/10/12 13:45
 * @Version: 1.0
 */
@Slf4j
public class RedisLockLUA {
    @Autowired
    JedisPool jedisPool;
    private String lock_key = "redis_lock"; //锁键
    protected long internalLockLeaseTime = 30000;//锁过期时间
    private long timeout = 999999; //获取锁的超时时间
    //SET命令的参数
//    SetParams params = SetParams.setParams().nx().px(internalLockLeaseTime);

    /**
     * 加锁
     * @param value 请求标示
     * @return
     */
    public boolean lock(String value) {
        Jedis jedis = jedisPool.getResource();
        Long start = System.currentTimeMillis();
        try {
            while (true) {
                //SET命令返回OK ，则证明获取锁成功
                String lock = jedis.set(lock_key, value);
                if ("OK".equals(lock)) {
                    return true;
                }
                //否则循环等待，在timeout时间内仍未获取到锁，则获取失败
                long l = System.currentTimeMillis() - start;
                if (l >= timeout) {
                    return false;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            jedis.close();
        }
    }

    /**
     * 释放分布式锁
     * @param lockKey 锁
     * @param value 请求标识
     * @return 是否释放成功
     */
    public boolean unlock(String lockKey,String value){
        Jedis jedis = jedisPool.getResource();
        String script =
                "if redis.call('get',KEYS[1]) == ARGV[1] then " +
                        " return redis.call('del',KEYS[1]) " +
                        "else  return 0 " +
                        "end";
        try {
            Object result = jedis.eval(script, Collections.singletonList(lock_key),
                    Collections.singletonList(value));
            if("1".equals(result.toString())){
                return true;
            }
            return false;
        }finally {
            jedis.close();
        }
    }
}