package com.sx.server0.entity.res;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * @Author: ZhangQi
 * @Date: 2023/8/30 16:34
 * @Description:
 */
@Data
public class VolMonitorEntity {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer id;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String time;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer nodeVolLevel;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private float voltageA;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String nodeNum;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String nodeNameStart;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String nodeTableName;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String nodeColumnNum;
}
