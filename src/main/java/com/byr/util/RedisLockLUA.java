package com.byr.util;

import com.byr.config.RedisPoolConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.params.SetParams;

import javax.annotation.Resource;
import java.io.Serializable;
import java.util.Collections;

/**
 * @ClassName: RedisLockLUA
 * @Description: redis分布式锁，锁不具有重入性
 * @Author: yanrong
 * @Date: 2019/10/12 13:45
 * @Version: 1.0
 */
@Component
@Slf4j
public class RedisLockLUA {

    @Resource
    RedisPoolConfig jedisPool;
    protected long internalLockLeaseTime = 30 * 1000;//锁过期时间
    private long TIME_OUT = 30 * 1000; //获取锁的超时时间
    //SET命令的参数
    SetParams params = SetParams.setParams().nx().px(internalLockLeaseTime);

    /**
     * @param key
     * @param value 请求标示，每次请求都是唯一的
     * @return
     */
    public boolean lock(String key, String value) {
        JedisCluster jedis=jedisPool.JedisClusterConfig();
//        Jedis jedis = jedisPool.getPool().getResource();
        Long startTime = System.currentTimeMillis();
        try {
            while (true) {
                try {
                    //SET命令返回OK ，则证明获取锁成功
                    String lock = jedis.set(key, value, params);
                    if ("OK".equals(lock)) {
                        return true;
                    }
                    //否则循环等待，在TIME_OUT时间内仍未获取到锁，则获取失败
                    long waitTime = System.currentTimeMillis() - startTime;
                    if (waitTime >= TIME_OUT) {
                        return false;
                    }
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
     *
     * @param key   锁
     * @param value 请求标识
     * @return 是否释放成功
     */
    public boolean unlock(String key, String value) {
        JedisCluster jedis=jedisPool.JedisClusterConfig();
        String script = "if redis.call('get',KEYS[1]) == ARGV[1] then " +
                        " return redis.call('del',KEYS[1]) " +
                        "else  return 0 " +
                        "end";
        try {
            Object result = jedis.eval(script, Collections.singletonList(key),
                    Collections.singletonList(value));
            if ("1".equals(result.toString())) {
                return true;
            }
            return false;
        } finally {
            jedis.close();
        }
    }
}