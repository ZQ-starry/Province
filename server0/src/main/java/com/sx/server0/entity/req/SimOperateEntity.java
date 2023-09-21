package com.sx.server0.entity.req;

import lombok.Data;

/**
 * @Author: ZhangQi
 * @Date: 2023/9/21 9:38
 * @Description:
 */
@Data
public class SimOperateEntity {
    private float simPercent;
    private float pvCapacity;
    private int status;
}
