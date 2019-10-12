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
 * @ClassName: RedisLock
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

    private static final int DEFAULT_ACQUIRY_RESOLUTION_MILLIS = 200;

    /**
     * Lock key path.
     */
//    private String lockKey;

    /**
     * 锁超时时间，防止线程在入锁以后，无限的执行等待
     */
    private int expireMsecs = 60 * 1000;

    /**
     * 锁等待时间，防止线程饥饿
     */
    private int timeoutMsecs = 10 * 1000;

    private volatile boolean locked = false;

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
            //设置key的有效时间
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
        int timeout = timeoutMsecs / 10;
        try {
            while (timeout >= 0) {
                //当前时间+key过期时间，作为value进行存储
                long expires = System.currentTimeMillis() + expireMsecs + 1;
                String expiresStr = String.valueOf(expires);
                if (this.setNX(lockKey, expiresStr, expireMsecs)) {
                    return true;
                }
                //获取key的value的值，这里的value是锁的过期时间
                String currentValueStr = this.get(lockKey);
                //判断是否为空，不为空的情况下，如果被其他线程设置了值，则第二个条件判断是过不去的,其他线程从而获取不到锁；
                if (currentValueStr != null && Long.parseLong(currentValueStr) < System.currentTimeMillis()) {
                    //设置新的value（保存key的过期时间），并返回旧的value（保存key的过期时间）。
                    String oldValueStr = this.getSet(lockKey, expiresStr);
                    //如果返回的的旧值为空，说明该锁已经被释放了，此时该进程可以获取锁；返回的旧值与上一步（this.get(lockKey)）获得的值一致说明中间未被其他进程获取该锁，可以获取锁
                    if (oldValueStr != null && oldValueStr.equals(currentValueStr)) {
                        //更新锁的过期时间
                        redisTemplate.expire(lockKey, expireMsecs, TimeUnit.MILLISECONDS);
                        return true;
                    }
                }
                timeout -= DEFAULT_ACQUIRY_RESOLUTION_MILLIS;
                /**
                 * 休眠线程，防止出过多的线程出现饥饿问题
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
        return null == currentValueStr ? false : true;
    }

    /**
     * 释放线程
     *
     * @param lockKey
     */
    public synchronized void unlock(String lockKey) {
        if (isLocked(lockKey)) {
            redisTemplate.delete(lockKey);
        }
    }

}