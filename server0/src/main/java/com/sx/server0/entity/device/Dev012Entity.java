package com.sx.server0.entity.device;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: ZhangQi
 * @Date: 2023/9/4 9:25
 * @Description:
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Dev012Entity {
    private String netIdDev = "169.254.206.111.1.1"; // 设备012对应的NetId
    private int lineDutNum = 1; // 设备中的线路结构体个数，和线路结果表的个数一致
    private int nodeDutNum = 1; // 设备中的节点结构体个数，和站点结果表的个数一致
    private int allDutNum = 2; // 设备中所有结构体个数，和所有结果表的个数一致
    private int lineDataLen = 180; // 设备中的线路数据的长度 180*lineDutNum
    private int nodeDataLen = 60; // 设备中的节点数据的长度 60*nodeDutNum
    private int staNum = 9; // 设备中的变电站的个数
    private int lineNum = 12; //设备中的线路条数
    private int nodeNum = 9; //设备中的站点个数
    private int iLen = 36; // 设备中的变电站电流的数据长度 staNum*4
}
