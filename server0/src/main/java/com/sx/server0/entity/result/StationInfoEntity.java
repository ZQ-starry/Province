package com.sx.server0.entity.result;

import lombok.Data;

/**
 * @Author: ZhangQi
 * @Date: 2023/9/10 15:03
 * @Description:
 */
@Data
public class StationInfoEntity {
    private String nodeNameStart;
    private Integer nodeVolLevel;
    private String nodeTableName;
    private String nodeColumnNum;
}
