package com.sx.server0.entity.res;

import lombok.Data;

/**
 * @Author: ZhangQi
 * @Date: 2023/7/24 9:38
 * @Description: 线路电流和功率实体
 */
@Data
public class LineCtAndPResEntity {
    private  Integer id;
    private float currentA;
    private float powerActive;
    private float powerReactive;
}
