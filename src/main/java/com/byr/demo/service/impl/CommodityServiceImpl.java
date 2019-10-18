package com.byr.demo.service.impl;


import com.byr.demo.service.CommodityService;
import com.byr.util.RedisLockGetSet;
import com.byr.util.RedisLockLUA;
import com.byr.util.RedisLockLUA2;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * 商品 服务层实现
 *
 * @author final
 * @date 2019-10-08
 */
@Service
@Slf4j
public class CommodityServiceImpl implements CommodityService {
    /**
     * getSet方式实现分布式锁
     */
    @Autowired
    RedisLockGetSet redisLockGetSet;
    /**
     * redisson实现分布式锁
     */
    @Autowired
    private Redisson redisson;
    /**
     * redis实现集群锁
     */
    @Autowired
    private RedisLockLUA redisLockLUA;

    @Autowired
    private RedisLockLUA2 redisLockLUA2;
    //线程等待时间
    final static long WAIT_TIME = 10 * 1000;
    // 模拟库存
    Integer NUMMBER = 100000;
    //获得锁的线程数量
    Integer THREAD_COUNT_GET_LOCK = 0;
    //总的线程数量
    Integer THREAD_COUNT = 0;

    /**
     * 减少获取库存
     *
     * @param value 货物编码
     * @return
     */
    public Integer getSetReduce(String value, String key) throws InterruptedException {
        log.info("第:{}个线程", ++THREAD_COUNT);
        if (redisLockGetSet.lock(key)) {
            try {
                Integer count = doSomething(key);
                if (count < 1) {
                    throw new RuntimeException("减库存数量为零");
                }
                return count;
            } catch (Exception e) {
                throw new RuntimeException("减库存失败", e);
            } finally {
                redisLockGetSet.unlock(key);
            }
        }

        return 0;
    }

    @Override
    public Integer redissonReduce(String value, String key) throws InterruptedException {
        log.info("第:{}个线程", ++THREAD_COUNT);
        Lock lock = redisson.getLock(key);
        /**
         *  可重入锁,线程等待WAIT_TiME 就会自动释放锁
         */
        if (lock.tryLock(WAIT_TIME, TimeUnit.MILLISECONDS)) {
            try {
                Integer count = doSomething(key);
                if (count < 1) {
                    throw new RuntimeException("库存为零");
                }
                return count;
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                lock.unlock();
            }
        }
        return null;
    }

    @Override
    public Integer redisLUAReduce(String value, String key) {
        log.info("第:{}个线程", ++THREAD_COUNT);
        if (redisLockLUA.lock(key, value)) {
            try {
                Integer count = doSomething(key);
                if (count < 1) {
                    throw new RuntimeException("库存为零");
                }
                return count;
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                redisLockLUA.unlock(key, value);
            }
        }
        return null;
    }

    @Override
    public Integer redisLUA2Reduce(String value, String key) {
        log.info("第:{}个线程", ++THREAD_COUNT);
        if (redisLockLUA2.lock(key, value)) {
            try {
                Integer count = doSomething(key);
                if (count < 1) {
                    throw new RuntimeException("库存为零");
                }
                return count;
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                redisLockLUA2.unlock(key, value);
            }
        }
        return null;
    }

    /**
     * 业务
     *
     * @param key
     * @return
     */
    private Integer doSomething(String key) {
        log.info("结果为：{}", --NUMMBER);
        log.info("获得锁的线程数为：{}", ++THREAD_COUNT_GET_LOCK);
        if (NUMMBER < 1) {
            throw new RuntimeException("资源为零");
        }
        return NUMMBER;
    }
}
