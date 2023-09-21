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
public class Dev131Entity {
    private Integer lineNum = 42; //设备中的线路条数
    private Integer lineDutNum = 3; // 设备中的线路结构体个数，和线路结果表的个数一致
    private Integer nodeNum = 32; //设备中的站点个数
    private Integer nodeDutNum = 3; // 设备中的节点结构体个数，和站点结果表的个数一致
    private Integer staNum = 23; // 设备中的变电站的个数
    private Integer allDutNum = 6; // 设备中所有结构体个数，和所有结果表的个数一致
    private Integer iLen = 92; // 设备中的变电站电流的数据长度 staNum*4
    private Integer lineDataLen = 540; // 设备中的线路数据的长度 180*lineDutNum
    private Integer nodeDataLen = 180; // 设备中的节点数据的长度 60*nodeDutNum
    private String netIdDev = "169.254.95.239.1.1"; // 设备131对应的NetId
}
