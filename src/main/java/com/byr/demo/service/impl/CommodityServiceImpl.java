package com.byr.demo.service.impl;


import com.byr.demo.service.CommodityService;
import com.byr.util.RedisLockGetSet;
import com.byr.util.RedisLockLUA;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    @Autowired
    private RedisLockLUA redisLockLUA;
    //线程等待时间
    final static long WAIT_TIME = 10 * 1000;
    //自动解锁时间
    final static Integer UNLOCK_TIME = 10 * 1000;
    // 模拟库存
    Integer NUMMBER = 10000;
    //获得锁的线程数量
    Integer THREAD_COUNT_GET_LOCK = 0;
    //总的线程数量
    Integer THREAD_COUNT = 0;

    /**
     * 减少获取库存
     *
     * @param commodityCode 货物编码
     * @return
     */
    @Transactional
    public Integer getSetReduce(String commodityCode, String key) throws InterruptedException {
        log.info("第:{}个线程", ++THREAD_COUNT);
        if (redisLockGetSet.lock(key)) {
            try {
//                reduce = commodityMapper.reduceCommodity(1, commodityCode);
                Integer count = doSomething(key);
                if (count < 1) {
                    throw new RuntimeException("减库存数量为零");
                }
                redisLockGetSet.unlock(key);
                return count;
            } catch (Exception e) {
                throw new RuntimeException("减库存失败", e);
            }
        }

        return 0;
    }

    @Override
    public Integer redissonReduce(String commodityCode, String key) throws InterruptedException {
        Lock lock = redisson.getLock(key);
        /**
         *  可重入锁,线程等待WAIT_TiME 就会自动释放锁
         */
        if (lock.tryLock(WAIT_TIME, TimeUnit.MILLISECONDS) && true) {
            try {
                Integer count = doSomething(key);
                if (count < 1) {
                    throw new RuntimeException("库存为零");
                }
                lock.unlock();
                return count;
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                lock.unlock();
            }
        }
        //异步执行
        RLock rLock = redisson.getLock(key);
        try {
            if (rLock.tryLockAsync(WAIT_TIME, TimeUnit.MILLISECONDS).get() && false) {
                Integer count = doSomething(key);
                if (count < 1) {
                    throw new RuntimeException("库存为零");
                }
                rLock.unlock();
                return count;
            }
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } finally {
            rLock.unlock();
        }
        /**
         * 公平锁
         */
        //同步方法
        RLock fairLock = redisson.getFairLock(key);
        if (fairLock.tryLock(WAIT_TIME, TimeUnit.MILLISECONDS) && false) {
            try {
                Integer count = doSomething(key);
                if (count < 1) {
                    throw new RuntimeException("库存为零");
                }
                fairLock.unlock();
                return count;
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                fairLock.unlock();
            }
        }
        //异步实现
        try {
            if (fairLock.tryLockAsync(WAIT_TIME, TimeUnit.MILLISECONDS).get() && false) {
                Integer count = doSomething(key);
                if (count < 1) {
                    throw new RuntimeException("库存为零");
                }
                fairLock.unlock();
                return count;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            fairLock.unlock();
        }
        /**
         * 红锁
         */
        return null;
    }

    @Override
    public Integer redisLUAReduce(String commodityCode, String key) {
        String value = UUID.randomUUID().toString();
        if (redisLockLUA.lock(key, value)) {
            try {
                Integer count = doSomething(key);
                if (count < 1) {
                    throw new RuntimeException("库存为零");
                }
                redisLockLUA.unlock(key, value);
                return count;
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                redisLockLUA.unlock(key, value);
            }
        }
        return null;
    }

    private Integer doSomething(String key) {
        log.info("结果为：{}", --NUMMBER);
        log.info("获得锁的线程数为：{}", ++THREAD_COUNT_GET_LOCK);
        if (NUMMBER < 1) {
            throw new RuntimeException("资源为零");
        }
        redisLockGetSet.unlock(key);
        return NUMMBER;
    }
}
