package com.byr.demo.mapper;

import com.byr.demo.entity.Commodity;
import com.byr.util.MyMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;


/**
 * 商品 数据层
 * 
 * @author final
 * @date 2019-10-08
 */
public interface CommodityMapper extends MyMapper<Commodity> {
    /**
     *
     * @param count 减去的数量
     * @param commodityCode 获取编码
     * @return
     */
    Integer reduceCommodity(@Param("count") Integer count,@Param("commodityCode") String commodityCode);
}