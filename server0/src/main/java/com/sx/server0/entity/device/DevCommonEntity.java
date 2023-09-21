package com.sx.server0.entity.device;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: ZhangQi
 * @Date: 2023/9/4 11:18
 * @Description:
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DevCommonEntity {
    private String netIdDev;
    private int lineDutNum;
    private int nodeDutNum;
    private int allDutNum;
    private int lineDataLen;
    private int nodeDataLen;
    private int staNum;
    private int lineNum;
    private int nodeNum;
    private int iLen;
}
