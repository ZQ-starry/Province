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
public class Dev141Entity {
    private Integer lineNum = 83; //设备中的线路条数
    private Integer lineDutNum = 6; // 设备中的线路结构体个数，和线路结果表的个数一致
    private Integer nodeNum = 75; //设备中的站点个数
    private Integer nodeDutNum = 5; // 设备中的节点结构体个数，和站点结果表的个数一致
    private Integer staNum = 44; // 设备中的变电站的个数
    private Integer allDutNum = 11; // 设备中所有结构体个数，和所有结果表的个数一致
    private Integer iLen = 176; // 设备中的变电站电流的数据长度 staNum*4
    private Integer lineDataLen = 1080; // 设备中的线路数据的长度 180*lineDutNum
    private Integer nodeDataLen = 300; // 设备中的节点数据的长度 60*nodeDutNum
    private String netIdDev = "169.254.65.79.1.1"; // 设备141对应的NetId
}
