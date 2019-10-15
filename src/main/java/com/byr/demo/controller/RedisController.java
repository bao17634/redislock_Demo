package com.byr.demo.controller;

import com.byr.demo.service.CommodityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * @ClassName: RedisController
 * @Description: TODO
 * @Author: yanrong
 * @Date: 2019/10/8 11:16
 * @Version: 1.0
 */
@Slf4j
@RestController
@RequestMapping(value = "redisController")
public class RedisController {
    @Autowired
    private CommodityService commodityService;

    @RequestMapping(value = "redisGetSet")
    public Integer redisGetSetController() throws InterruptedException {
        Integer reduce = 0;
        try {
            reduce = commodityService.getSetReduce("9c63d5", "11222");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return reduce;
    }
    @RequestMapping(value = "redisson")
    public Integer redissonController() throws InterruptedException {
        Integer reduce = 0;
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        try {
            reduce = commodityService.redissonReduce(uuid, "11222");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return reduce;
    }
    @RequestMapping(value = "redisLUA")
    public Integer redisLUAController() throws InterruptedException {
        Integer reduce = 0;
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        try {
            reduce = commodityService.redisLUAReduce(uuid, "11222");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return reduce;
    }
}
