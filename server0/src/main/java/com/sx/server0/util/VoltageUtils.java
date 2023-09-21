package com.sx.server0.util;

import com.sx.server0.component.SpringUtil;
import com.sx.server0.dao.read.DataReadDao;
import com.sx.server0.entity.data.FaultsStandardEntity;
import com.sx.server0.entity.res.VolMonitorEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author: ZhangQi
 * @Date: 2023/9/11 15:52
 * @Description: 电压计算工具类
 */
@Component
public class VoltageUtils {

    @Autowired
    private DataReadDao dataReadDao = SpringUtil.getBean(DataReadDao.class);
    /**
     * 电压监测方法,用于计算每个时间段的各电压等级下的最低电压
     */
    public void minVoltageOfLevel(String time){
        try {
            int countDev = dataReadDao.getCurrentNumber();
            List<VolMonitorEntity> list1000 = dataReadDao.getStationTables(1000);
            List<VolMonitorEntity> list500 = dataReadDao.getStationTables(500);
            List<VolMonitorEntity> list220 = dataReadDao.getStationTables(220);
            List<VolMonitorEntity> listFinal = new ArrayList<>(3);
            VolMonitorEntity volMonitorEntityStart = new VolMonitorEntity();
            listFinal.add(0,volMonitorEntityStart);
            listFinal.add(1,volMonitorEntityStart);
            listFinal.add(2,volMonitorEntityStart);
            float volValue1000 = Float.MAX_VALUE;
            float volValue500 = Float.MAX_VALUE;
            float volValue220 = Float.MAX_VALUE;
            if (countDev>0){
                if (list1000.size()>0){
                    for (int i=0; i<list1000.size(); i++){
                        // 查询电压
                        VolMonitorEntity volMonitorEntity = list1000.get(i);
                        String number = volMonitorEntity.getNodeColumnNum();
                        String columnName = "voltage"+ number + "_a";
                        String tableName = list1000.get(i).getNodeTableName();
                        float voltageA = dataReadDao.getStationVol(columnName,tableName,countDev);
                        // 比较最低电压
                        if (voltageA != 0 && volValue1000 > voltageA){
                            // 进行替换
                            volValue1000 = voltageA;
                            volMonitorEntity.setVoltageA(volValue1000);
                            volMonitorEntity.setTime(time);
                            listFinal.set(0,volMonitorEntity);
                        }
                    }
                }

                if (list500.size()>0){
                    for (int i=0; i<list500.size(); i++){
                        VolMonitorEntity volMonitorEntity = list500.get(i);
                        // 查询电压
                        String number = volMonitorEntity.getNodeColumnNum();
                        String columnName = "voltage"+ number + "_a";
                        String tableName = list500.get(i).getNodeTableName();
                        float voltageA = dataReadDao.getStationVol(columnName,tableName,countDev);
                        // 比较最低电压
                        if (voltageA != 0 && volValue500 > voltageA){
                            volValue500 = voltageA;
                            volMonitorEntity.setVoltageA(volValue500);
                            volMonitorEntity.setTime(time);
                            listFinal.set(1,volMonitorEntity);
                        }
                    }
                }
                if (list220.size()>0){
                    for (int i=0; i<list220.size(); i++){
                        VolMonitorEntity volMonitorEntity = list220.get(i);
                        // 查询电压
                        String number = volMonitorEntity.getNodeColumnNum();
                        String columnName = "voltage"+ number + "_a";
                        String tableName = list220.get(i).getNodeTableName();
                        float voltageA = dataReadDao.getStationVol(columnName,tableName,countDev);
                        // 比较最低电压
                        if (voltageA != 0 && volValue220 > voltageA){
                            volValue220 = voltageA;
                            volMonitorEntity.setVoltageA(volValue220);
                            volMonitorEntity.setTime(time);
                            listFinal.set(2,volMonitorEntity);
                        }
                    }
                }
            }
            // 执行插入
            dataReadDao.insertMinVol(listFinal);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 电压越限故障方法,各电压等级变电站电压越限计算工具类
     */
    public void voltageFault(String time){
        try {
            // 取出最新一次已经读取的次数
            int countDev = dataReadDao.getCurrentNumber();
            // 数据库取出电压越限的标准
            FaultsStandardEntity faultsStandardEntity = dataReadDao.getFaultsStandard();
            float volFaultValueMax1000 = faultsStandardEntity.getVolFaultValueMax1000();
            float volFaultValueMin1000 = faultsStandardEntity.getVolFaultValueMin1000();
            float volFaultValueMax500 = faultsStandardEntity.getVolFaultValueMax500();
            float volFaultValueMin500 = faultsStandardEntity.getVolFaultValueMin500();
            float volFaultValueMax220 = faultsStandardEntity.getVolFaultValueMax220();
            float volFaultValueMin220 = faultsStandardEntity.getVolFaultValueMax220();
            List<VolMonitorEntity> list1000 = dataReadDao.getStationTables(1000);
            List<VolMonitorEntity> list500 = dataReadDao.getStationTables(500);
            List<VolMonitorEntity> list220 = dataReadDao.getStationTables(220);
            List<VolMonitorEntity> volFinal = new ArrayList<>();
            if (countDev>0){
                VoltageFaultsOperate(time, countDev, volFaultValueMax1000, volFaultValueMin1000, list1000, volFinal);
                VoltageFaultsOperate(time, countDev, volFaultValueMax500, volFaultValueMin500, list500, volFinal);
                VoltageFaultsOperate(time, countDev, volFaultValueMax220, volFaultValueMin220, list220, volFinal);
            }
            // 将该时段的电压越限信息插入数据库,faults_vol_node
            if (volFinal.size()>0){
                dataReadDao.insertVolFault(volFinal);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param time 当前仿真的一个时间
     * @param countDev 当前的一个读取次数
     * @param volFaultValueMax 电压上限
     * @param volFaultValueMin 电压下限
     * @param list 含变电站信息的集合
     * @param volFinal 得到的一个电压越限的变电站的集合
     */
    private void VoltageFaultsOperate(String time, int countDev, float volFaultValueMax, float volFaultValueMin, List<VolMonitorEntity> list, List<VolMonitorEntity> volFinal) {
        if (list.size()>0) {
            for (int i = 0; i < list.size(); i++) {
                // 查询电压
                String number = list.get(i).getNodeColumnNum();
                String columnName = "voltage" + number + "_a";
                String tableName = list.get(i).getNodeTableName();
                float voltageA = dataReadDao.getStationVol(columnName, tableName, countDev);
                // 比较是否越限
                if (voltageA != 0){
                    if (voltageA>volFaultValueMax || voltageA<volFaultValueMin) {
                        list.get(i).setVoltageA(voltageA);
                        list.get(i).setTime(time);
                        volFinal.add(list.get(i));
                    }
                }
            }
        }
    }
}
