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
public class Dev112Entity {
    private String netIdDev = "169.254.65.42.1.1"; // 设备112对应的NetId
    private Integer lineDutNum = 2; // 设备011中的线路结构体个数,和结果表的个数一致
    private Integer nodeDutNum = 1; // 设备011中的节点结构体个数
    private Integer allDutNum = 3; // 设备所有结构体个数
    private Integer lineDataLen = 360; // 设备011中的线路数据的长度 180*LINE_EN_LEN
    private Integer nodeDataLen = 60; // 设备011中的节点数据的长度 60*NODE_EN_LEN
    private Integer lineNum = 19; //设备011中的线路条数
    private Integer nodeNum = 13; //设备011中的站点个数
    private Integer staNum = 13; // 设备011中的变电站的个数
    private Integer iLen = 52; // 设备011中的变电站电流的数据长度 staNum*4
}
