package com.sx.server0.entity.res;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * @Author: ZhangQi
 * @Date: 2023/8/30 10:49
 * @Description:
 */
@Data
public class LoadRateFaultEntity {
    private Integer id;
    // 负载率越限时间
    private String time;
    // 负载率
    private float loadRate;
    // 节点编号 nodeNum
    private String nodeNum;
    // 节点名称
    private String nodeNameStart;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String nodeNameEnd;
}
