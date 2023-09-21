package com.sx.server0.entity.res;

import lombok.Data;

/**
 * @Author: ZhangQi
 * @Date: 2023/7/25 16:46
 * @Description: 负载率实体类
 */
@Data
public class LoadRateEntity {
    private String time;
    private String nodeNum;
    private String nodeNameStart;
    private String nodeNameEnd;
    private float loadRate;
}
