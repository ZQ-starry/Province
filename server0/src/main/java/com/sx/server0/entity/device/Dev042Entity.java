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
public class Dev042Entity {
    private String netIdDev = "169.254.87.70.1.1"; // 设备042对应的NetId
    private Integer lineDutNum = 2; // 设备中的线路结构体个数，和线路结果表的个数一致
    private Integer nodeDutNum = 2; // 设备中的节点结构体个数，和站点结果表的个数一致
    private Integer allDutNum = 4; // 设备中所有结构体个数，和所有结果表的个数一致
    private Integer lineDataLen = 360; // 设备中的线路数据的长度 180*lineDutNum
    private Integer nodeDataLen = 120; // 设备中的节点数据的长度 60*nodeDutNum
    private Integer staNum = 10; // 设备中的变电站的个数
    private Integer lineNum = 21; //设备中的线路条数
    private Integer nodeNum = 17; //设备中的站点个数
    private Integer iLen = 40; // 设备中的变电站电流的数据长度 staNum*4
}
