package com.byr.demo.service.impl;


import com.byr.demo.service.CommodityService;
import com.byr.util.RedisLockGetSet;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 商品 服务层实现
 *
 * @author final
 * @date 2019-10-08
 */
@Service
@Slf4j
public class CommodityServiceImpl implements CommodityService {
    @Autowired
    Redisson redisson;
    @Autowired
    RedisLockGetSet redisLockGetSet;
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

    public Integer reduce(String commodityCode, String key) throws InterruptedException {
        Integer reduce = 0;
        log.info("第:{}个线程", ++THREAD_COUNT);
        try {
            if (redisLockGetSet.lock(key)) {
//                reduce = commodityMapper.reduceCommodity(1, commodityCode);
                log.info("结果为：{}", --NUMMBER);
                log.info("获得锁的线程数为：{}", ++THREAD_COUNT_GET_LOCK);
                if (NUMMBER < 1) {
                    throw new RuntimeException("资源为零");
                }
                redisLockGetSet.unlock(key);
                return NUMMBER;
            }
        } catch (Exception e) {
            throw new RuntimeException("减库存失败", e);
        }
        return 0;
    }

    public void redissonRduce(){
        RLock rLock=redisson.getLock("key");

    }
}
