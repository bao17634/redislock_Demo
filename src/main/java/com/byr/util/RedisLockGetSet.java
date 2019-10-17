package com.byr.util;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * @ClassName: RedissonLockService
 * @Description: redis锁
 * @Author: yanrong
 * @Date: 2019/10/9 11:16
 * @Version: 1.0
 */
@Component
@Slf4j
public class RedisLockGetSet {
    @Autowired
    private RedisTemplate redisTemplate;
    /**
     * 循环间隔时间(根据自己业务执行时间设置)
     */
    private static final int DEFAULT_ACQUIRY_RESOLUTION_MILLIS = 200;
    private boolean LOCKED = false;
    /**
     * 锁超时时间，防止线程在入锁以后，无限的执行等待（这个时间根据自己业务执行时间来设定）
     */
    private int WXPIRE_MESECS = 60 * 1000;
    /**
     * 锁等待时间，防止线程饥饿（这个时间根据自己业务执行时间来设定）
     */
    private int TIMEOUT_MESECS = 10 * 1000;

    /**
     * @return lock key
     */

    public String get(final String key) {
        ValueOperations<Serializable, Object> operations = redisTemplate.opsForValue();
        Object result = operations.get(key);
        return null == result ? null : (String) result;
    }

    /**
     * 写入缓存，如果key不存在才写入，用于锁，同时设置key过期时间
     *
     * @param key
     * @param value
     * @param expireTime 锁超时时间
     * @return
     */
    private boolean setNX(final String key, Object value, long expireTime) {
        try {
            ValueOperations<Serializable, Object> operations = redisTemplate.opsForValue();
            //设置key，value
            Boolean result = operations.setIfAbsent(key, value);
            //设置key的有效时间（和设置key不是原子操作，会出现设置了key，但过期时间未设置成功）
            redisTemplate.expire(key, expireTime, TimeUnit.MILLISECONDS);
            return null == result ? false : result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 设置新的value的值，并返回旧值
     *
     * @param key
     * @param value
     * @return
     */
    private String getSet(final String key, final String value) {
        try {
            ValueOperations<Serializable, Object> operations = redisTemplate.opsForValue();
            Object result = operations.getAndSet(key, value);
            return null == result ? null : (String) result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获得 lock.
     * 实现思路: 主要是使用了redis 的setnx命令,缓存了锁.
     * reids缓存的key是锁的key,所有的共享, value是锁的到期时间(注意:这里把过期时间放在value了,没有时间上设置其超时时间)
     * 执行过程:
     * 1.通过setnx尝试设置某个key的值,成功(当前没有这个锁)则返回,成功获得锁
     * 2.锁已经存在则获取锁的到期时间,和当前时间比较,超时的话,则设置新的值
     *
     * @param lockKey 锁key
     * @return true if lock is acquired, false acquire timeouted
     * @throws InterruptedException in case of thread interruption
     */
    public synchronized boolean lock(String lockKey) throws InterruptedException {
        int timeout = TIMEOUT_MESECS / 10;
        try {
            while (timeout >= 0) {
                //当前时间+key过期时间，作为value进行存储
                long expires = System.currentTimeMillis() + WXPIRE_MESECS + 1;
                String expiresStr = String.valueOf(expires);
                if (this.setNX(lockKey, expiresStr, WXPIRE_MESECS)) {
                    LOCKED = true;
                    return true;
                }
                //获取key的value的值，这里的value是锁的过期时间
                String currentValueStr = this.get(lockKey);
                /**
                 * 代码作用：确认锁是否出现过期但是未自动删除情况
                 * 如果返回的值为空，说明此时该锁已经被释放了；
                 * 如果返回的值小于系统当前时间，则说明锁已经过期，但redis没有自动删除
                 */
                if (currentValueStr != null && Long.parseLong(currentValueStr) < System.currentTimeMillis()) {
                    //设置新的value（保存key的过期时间），并返回旧的value。
                    String oldValueStr = this.getSet(lockKey, expiresStr);
                    /**
                     * 代码作用：确认锁是否已被其他线程修改
                     * 如果返回的的旧值为空，说明此时该锁已经被释放了；
                     * 如果oldValueStr和currentValueStr不相等说明该锁已被其他线程修改
                     */
                    if (oldValueStr != null && oldValueStr.equals(currentValueStr)) {
                        LOCKED = true;
                        //更新锁的过期时间
                        redisTemplate.expire(lockKey, WXPIRE_MESECS, TimeUnit.MILLISECONDS);
                        return true;
                    }
                }
                timeout -= DEFAULT_ACQUIRY_RESOLUTION_MILLIS;
                /**
                 * 休眠线程，防止出过多的线程出现饥饿问题(根据自己业务执行时间设置)
                 */
                Thread.sleep(DEFAULT_ACQUIRY_RESOLUTION_MILLIS);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    /**
     * 根据key判断是否存在锁
     *
     * @param lockKey
     * @return
     */
    public boolean isLocked(String lockKey) {
        /**
         * 只根据key判断当前锁是否存在，但锁有可能不是自己的
         */
        String currentValueStr = this.get(lockKey);
        //检验是否超过有效期，如果不在有效期内，那说明当前锁已经失效，不能进行删除操作
        if (currentValueStr != null && Long.parseLong(currentValueStr) > System.currentTimeMillis()) {
            return true;
        }
        return false;
    }

    /**
     * 释放线程
     *
     * @param lockKey
     */
    public synchronized void unlock(String lockKey) {
        try {
            if (isLocked(lockKey) && LOCKED) {
                redisTemplate.delete(lockKey);
                LOCKED = false;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}