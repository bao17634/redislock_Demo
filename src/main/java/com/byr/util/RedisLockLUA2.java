package com.byr.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.SetParams;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @ClassName: RedisLockLUA
 * @Description: RedisTemplate 根据LUA脚本实现分布式锁
 * @Author: yanrong
 * @Date: 2019/10/12 13:45
 * @Version: 1.0
 */
@Component
@Slf4j
public class RedisLockLUA2 {
    @Autowired
    private RedisTemplate redisTemplate;
    //锁过期时间
    private long LOCK_LEASE_TIME = 30 * 1000;
    //线程等待时间（根据自己的业务执行时间来设置）
    private long WAIT_TIME = 5 * 1000;
    //循环时间间隔（根据自己的业务执行时间来设置）
    private long INTERVAL_TIME = 1000;

    /**
     * @param key
     * @param value 请求标示，每次请求都是唯一的
     * @return
     */
    public boolean lock(String key, String value) {
        List<String> list_key = new ArrayList();
        list_key.add(key);
        /**
         *  -- 加锁脚本，其中KEYS[]为外部传入参数
         *  -- KEYS[1]表示key
         *  -- ARGV[1]表示value
         *  -- ARGV[2]表示过期时间
         */
        String lua_script = "if redis.call('SETNX',KEYS[1],ARGV[1]) == 1 then" +
                "     return redis.call('pexpire',KEYS[1],ARGV[2])" +
                " else" +
                "     return 0 " +
                "end";
        try {
            while (WAIT_TIME >= 0) {
                RedisScript<String> redis_script = new DefaultRedisScript<>(lua_script, String.class);
                Object return_flag = redisTemplate.execute(redis_script, list_key, value, String.valueOf(LOCK_LEASE_TIME));
                if ("0".equals(String.valueOf(return_flag))) {
                    return true;
                }
                WAIT_TIME -= INTERVAL_TIME;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    public boolean unlock(String key, String value) {
        List<String> list_key = new ArrayList();
        list_key.add(key);
        String var_script = "if redis.call('get',KEYS[1]) == ARGV[1] then " +
                " return redis.call('del',KEYS[1]) " +
                "else  return 0 " +
                "end";
        try {
            RedisScript<Long> redis_script = new DefaultRedisScript<Long>(var_script, Long.class);
            Object return_flag = redisTemplate.execute(redis_script, list_key, value);
            if ("0".equals(String.valueOf(return_flag))) {
                return true;
            }
            return false;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}