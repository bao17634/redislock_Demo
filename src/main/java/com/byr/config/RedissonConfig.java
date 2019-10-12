package com.byr.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.stereotype.Component;

/**
 * @ClassName: RedissonConfig
 * @Description: TODO
 * @Author: yanrong
 * @Date: 2019/10/12 9:25
 * @Version: 1.0
 */
@Component
public class RedissonConfig {
    public RedissonClient getClient() {
        //单机
//    RedissonClient redisson = Redisson.create();
//    Config config = new Config();
//config.useSingleServer().setAddress("myredisserver:6379");
//    RedissonClient redisson = Redisson.create(config);
//
//
////主从
//
//    Config config = new Config();
//config.useMasterSlaveServers()
//        .setMasterAddress("127.0.0.1:6379")
//    .addSlaveAddress("127.0.0.1:6389", "127.0.0.1:6332", "127.0.0.1:6419")
//    .addSlaveAddress("127.0.0.1:6399");
//    RedissonClient redisson = Redisson.create(config);
//
//
//    //哨兵
//    Config config = new Config();
//config.useSentinelServers()
//        .setMasterName("mymaster")
//    .addSentinelAddress("127.0.0.1:26389", "127.0.0.1:26379")
//    .addSentinelAddress("127.0.0.1:26319");
//    RedissonClient redisson = Redisson.create(config);


        //集群
        Config config = new Config();
        config.useClusterServers()
                .setScanInterval(2000) // cluster state scan interval in milliseconds
                .addNodeAddress("127.0.0.1:7000", "127.0.0.1:7001")
                .addNodeAddress("127.0.0.1:7002");
        RedissonClient redisson = Redisson.create(config);
        return redisson;
    }
}
