package com.byr.demo.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.Date;
import lombok.NoArgsConstructor;


/**
 * 商品表 commodity
 *
 * @author final
 * @date 2019-10-08
 */
@Data
public class Commodity {
	private static final long serialVersionUID = 1L;

	/** 商品名*/
	private String commodityName;

	/**商品代码*/
	private String commodityCode;

	/** 商品数量*/
	private Long commodityCount;

}
