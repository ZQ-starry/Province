package com.sx.server0.entity.res;

import lombok.Data;

/**
 * @Author: ZhangQi
 * @Date: 2023/8/30 16:36
 * @Description:
 */
@Data
public class FreqMonitorEntity {
    private Integer id;
    private String time;
    private float freq;
}
