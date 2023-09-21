package com.sx.server0.entity.data;

import lombok.Data;

/**
 * @Author: ZhangQi
 * @Date: 2023/9/8 10:16
 * @Description: 用于负载率计算时的基础信息赋值
 */
@Data
public class PreLineInfoEntity {
    private String nodeNum;
    private String nodeNameStart;
    private String nodeNameEnd;
}
