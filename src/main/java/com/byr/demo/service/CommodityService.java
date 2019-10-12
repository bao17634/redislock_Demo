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
     * @param commodityCode
     * @param key
     * @return
     * @throws InterruptedException
     */
    Integer getSetReduce(String commodityCode, String key)throws InterruptedException;

    /**
     * redisson 实现分布式锁
     * @param commodityCode
     * @param key
     * @return
     */
    Integer redissonReduce(String commodityCode, String key) throws InterruptedException;

    /**
     * redis 利用LUA脚本语言实现分布式锁
     * @param commodityCode
     * @param key
     * @return
     */
    Integer redisLUAReduce(String commodityCode, String key);
}
