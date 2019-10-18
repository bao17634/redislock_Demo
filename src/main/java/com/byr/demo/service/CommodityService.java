package com.byr.demo.service;

import com.byr.demo.entity.Commodity;
import com.byr.util.MyMapper;

/**
 * 商品 服务层
 * 
 * @author final
 * @date 2019-10-08
 */
public interface CommodityService {
    /**
     * redis get、set方式实现分布式锁
     * @param value
     * @param key
     * @return
     * @throws InterruptedException
     */
    Integer getSetReduce(String value, String key)throws InterruptedException;

    /**
     * redisson 实现分布式锁
     * @param value
     * @param key
     * @return
     */
    Integer redissonReduce(String value, String key) throws InterruptedException;

    /**
     * redis 实现集群redis分布式锁
     * @param value
     * @param key
     * @return
     */
    Integer redisLUAReduce(String value, String key);
    /**
     * redis 实现集群redis分布式锁
     * @param value
     * @param key
     * @return
     */
    Integer redisLUA2Reduce(String value, String key);
}
