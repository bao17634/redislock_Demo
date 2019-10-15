package com.byr.config;

import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.asm.Advice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.stereotype.Component;
import redis.clients.jedis.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @ClassName: RedisConfig
 * @Description: TODO
 * @Author: yanrong
 * @Date: 2019/10/15 11:08
 * @Version: 1.0
 */
@Component
@Slf4j
public class RedisPoolConfig {
    @Autowired
    RedisProperties redisProperties;
    //扫描次数
    final static Integer RENTRY_NUM = 5;

    public JedisPool getPool() {
        JedisPoolConfig config = new JedisPoolConfig();
        return new JedisPool(config);
    }

    public Jedis getJedis() {
        Jedis jedis = null;
        int count = 0;
        do {
            try {
                jedis = getPool().getResource();
                count++;
            } catch (Exception e) {
                log.error("get jedis failed ", e);
                if (jedis != null) {
                    jedis.close();
                }
            }
        } while (jedis == null && count < RENTRY_NUM);
        return jedis;
    }

    public JedisCluster JedisClusterConfig() {
        this.getPool();
        JedisCluster jedisCluster = null;
        List<String> nodes = redisProperties.getCluster().getNodes();
        Set<HostAndPort> clusterNodde = new HashSet<>();
        try {
            nodes.stream().forEach(node -> clusterNodde.add(new HostAndPort(node.substring(0, node.indexOf(":")),
                    Integer.valueOf(node.substring(node.indexOf(":") + 1, node.length())))));
            jedisCluster = new JedisCluster(clusterNodde);
        } catch (Exception e) {
            throw new RuntimeException("JedisCluster connect is error", e);
        }
        return jedisCluster;
    }
}
