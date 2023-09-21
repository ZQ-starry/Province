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
public class Dev091Entity {
    private String netIdDev = "169.254.167.228.1.1"; // 设备091对应的NetId
    private Integer lineDutNum = 3; // 设备011中的线路结构体个数,和结果表的个数一致
    private Integer nodeDutNum = 2; // 设备011中的节点结构体个数
    private Integer allDutNum = 5; // 设备所有结构体个数
    private Integer lineDataLen = 540; // 设备011中的线路数据的长度 180*LINE_EN_LEN
    private Integer nodeDataLen = 120; // 设备011中的节点数据的长度 60*NODE_EN_LEN
    private Integer lineNum = 37; //设备011中的线路条数
    private Integer nodeNum = 26; //设备011中的站点个数
    private Integer staNum = 18; // 设备011中的变电站的个数
    private Integer iLen = 72; // 设备011中的变电站电流的数据长度 staNum*4
}
