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
    Integer reduce(String commodityCode, String key)throws InterruptedException;
}
