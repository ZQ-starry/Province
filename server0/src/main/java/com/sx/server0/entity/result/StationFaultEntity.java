package com.sx.server0.entity.result;

import lombok.Data;

/**
 * @Author: ZhangQi
 * @Date: 2023/9/10 15:30
 * @Description:
 */
@Data
public class StationFaultEntity {
    private String time;
    private Integer nodeVolLevel;
    private String nodeNum;
    private String nodeNameStart;
}
