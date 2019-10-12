package com.byr.util;

import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @ClassName: RedissonLock
 * @Description: TODO
 * @Author: yanrong
 * @Date: 2019/10/12 15:02
 * @Version: 1.0
 */
public class RedissonLock {
    @Autowired
    Redisson redisson;
    public boolean lock(){
        RLock rLock=redisson.getLock("test");
        rLock.lock();
        rLock.unlock();
        return false;
    }
}
