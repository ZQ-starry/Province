package com.sx.server0.util;


import com.sx.server0.entity.data.PreLineInfoEntity;
import com.sx.server0.entity.data.PreNodeInfoEntity;
import com.sx.server0.entity.res.LoadRateEntity;
import com.sx.server0.entity.result.LineResultEntity;
import com.sx.server0.entity.result.NodeResultEntity;
import com.sx.server0.entity.result.StationFaultEntity;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class LoadRateUtil {

    /**
     * 线路负载率计算方法
     * @param preLineInfos 包含线路的编号、首末端名称
     * @param lineList 包含该设备所有线路的最新数据，索引值对应表的个数，一个实体对应一个表的数据
     * @param ratedCurrents 设备中的线路的额定电流数组
     * @return
     */
    public List<LoadRateEntity> lineLoadRate(String time[], List<PreLineInfoEntity> preLineInfos, List<LineResultEntity> lineList, Float[] ratedCurrents) {
        Float[] lineCurrent = new Float[lineList.size()*15];
        List<LoadRateEntity> loadRateList = new ArrayList<>();
        String timeValue = time[0];
        for (int i = 0; i < lineList.size(); i++) {
            LineResultEntity lineResultEntity = lineList.get(i);
            try {
                Field[] fields = LineResultEntity.class.getDeclaredFields();
                for (Field field : fields) {
                    field.setAccessible(true);
                }
                for (int j = 0; j < 15; j++) {
                    // 取A相电流有效值
                    lineCurrent[i * 15 + j] = ((Float) fields[j * 3].get(lineResultEntity));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (preLineInfos.size() == ratedCurrents.length) {
            for (int i = 0; i < preLineInfos.size(); i++) {
                LoadRateEntity loadRateEntity = new LoadRateEntity();
                // 放入节点编号
                loadRateEntity.setTime(timeValue);
                loadRateEntity.setNodeNum(preLineInfos.get(i).getNodeNum());
                loadRateEntity.setNodeNameStart(preLineInfos.get(i).getNodeNameStart());
                loadRateEntity.setNodeNameEnd(preLineInfos.get(i).getNodeNameEnd());
                float loadRate = lineCurrent[i] / ratedCurrents[i] * 100;
                loadRateEntity.setLoadRate(loadRate);
                loadRateList.add(loadRateEntity);
            }
        } else {
            System.out.println("-------------线路额定电流长度不一致--------------");
        }
        return loadRateList;
    }

    /**
     * 变电站负载率计算方法
     * @param uis
     * @param nodeRatedCapacity
     * @return
     */
    public List<LoadRateEntity> nodeLoadRate(String time[], List<PreNodeInfoEntity> preNodeInfos, Float[] uis, Float[] nodeRatedCapacity) {
        List<LoadRateEntity> loadRateList = new ArrayList<>();
        String timeValue = time[0];
        for (int i = 0; i < uis.length; i++) {
            LoadRateEntity loadRateEntity = new LoadRateEntity();
            loadRateEntity.setTime(timeValue);
            loadRateEntity.setNodeNum(preNodeInfos.get(i).getNodeNum());
            loadRateEntity.setNodeNameStart(preNodeInfos.get(i).getNodeNameStart());
            float loadRate = uis[i] / nodeRatedCapacity[i];
            loadRateEntity.setLoadRate(loadRate);
            loadRateList.add(loadRateEntity);
        }
        return loadRateList;
    }
}
