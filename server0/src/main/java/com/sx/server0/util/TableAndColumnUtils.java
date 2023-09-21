package com.sx.server0.util;


import com.sx.server0.entity.req.NodeInfoEntity;

/**
 * @Author: ZhangQi
 * @Date: 2023/7/24 10:54
 * @Description: 数据的字段名设定
 */
public class TableAndColumnUtils {
    public NodeInfoEntity columnNames(NodeInfoEntity nodeInfo){
        String number = nodeInfo.getNodeColumnNum();
        String variable = nodeInfo.getNodeVariable();
        if (variable.equals("线路")){
            nodeInfo.setNodeColumnName1("current" + number + "_a");
            nodeInfo.setNodeColumnName2("power_active" + number);
            nodeInfo.setNodeColumnName3("power_reactive" + number);
        }else {
            nodeInfo.setNodeColumnName1("voltage"+ number + "_a");
        }
        return nodeInfo;
    }
}
