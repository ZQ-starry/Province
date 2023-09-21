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
public class Dev022Entity {
    private String netIdDev = "169.254.251.57.1.1"; // 设备022对应的NetId
    private Integer lineDutNum = 1; // 设备中的线路结构体个数，和线路结果表的个数一致
    private Integer nodeDutNum = 1; // 设备中的节点结构体个数，和站点结果表的个数一致
    private Integer allDutNum = 2; // 设备中所有结构体个数，和所有结果表的个数一致
    private Integer lineDataLen = 180; // 设备中的线路数据的长度 180*lineDutNum
    private Integer nodeDataLen = 60; // 设备中的节点数据的长度 60*nodeDutNum
    private Integer staNum = 8; // 设备中的变电站的个数
    private Integer lineNum = 14; //设备中的线路条数
    private Integer nodeNum = 11; //设备中的站点个数
    private Integer iLen = 32; // 设备中的变电站电流的数据长度 staNum*4
}
