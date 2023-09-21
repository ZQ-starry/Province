package com.sx.server0.dao.read;

import com.sx.server0.entity.data.FaultsStandardEntity;
import com.sx.server0.entity.req.NodeInfoEntity;
import com.sx.server0.entity.res.*;
import com.sx.server0.entity.result.GridInfoEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @Author: ZhangQi
 * @Date: 2023/7/13 12:32
 * @Description:
 */
@Mapper
@Repository
public interface DataReadDao {

    @Select("SELECT * FROM node_info_all WHERE node_num = #{nodeNum} ")
    NodeInfoEntity getNodeInfo(String nodeNum);

    List<SsVolResEntity> getSsVoltage(NodeInfoEntity nodeInfoEntity);

    List<LineCtAndPResEntity> getLineCtAndP(NodeInfoEntity nodeInfoEntity);

    @Select("SELECT * FROM load_rate_line")
    List<LoadRateEntity> getLoadRateLine();

    @Select("SELECT * FROM load_rate_node")
    List<LoadRateEntity> getLoadRateNode();

    @Select("SELECT time FROM time_axis ORDER BY id LIMIT 40")
    List<String> getTimeAxis();

    @Select("SELECT * FROM monitor_freq ORDER BY id DESC LIMIT 20")
    List<FreqMonitorEntity> getFreq();

    @Select("SELECT * FROM node_info_all WHERE node_variable = '线路' AND city = #{value}")
    List<NodeInfoEntity> getNodeInfoByCityOfLine(String value);

    @Select("SELECT * FROM node_info_all WHERE node_variable <> '线路' AND city = #{value}")
    List<NodeInfoEntity> getNodeInfoByCityOfStation(String cityName);

    @Select("SELECT * FROM node_info_all WHERE node_variable = #{value}")
    List<NodeInfoEntity> getNodeInfoByNodeVar(String value);

    @Select("SELECT * FROM node_info_all WHERE node_vol_level = #{value} AND node_variable<>'线路'")
    List<NodeInfoEntity> getNodeInfoByNodeVol(Integer value);

    @Select("SELECT * FROM node_info_all WHERE node_vol_level = #{value} AND node_variable='线路'")
    List<NodeInfoEntity> getLineInfoByNodeVol(Integer value);

    @Select("SELECT * FROM node_info_all WHERE node_variable='线路' AND node_name_start = #{nodeNameStart} AND node_name_end = #{nodeNameEnd}")
    List<NodeInfoEntity> getLineInfoByStartAndEnd(String nodeNameStart, String nodeNameEnd);

    @Select("SELECT * FROM node_info_all WHERE node_variable='线路' AND node_name_start = #{nodeNameStart}")
    List<NodeInfoEntity> getLineInfoByStart(String nodeNameStart);

    @Select("SELECT * FROM faults_load_rate_node WHERE load_rate >= #{nodeFaultValue}")
    List<LoadRateFaultEntity> getNodeLoadRateFaults(float nodeFaultValue);

    @Select("SELECT * FROM faults_load_rate_line WHERE load_rate >= #{lineFaultValue}")
    List<LoadRateFaultEntity> getLineLoadRateFaults(float lineFaultValue);

    @Select("SELECT * FROM monitor_freq WHERE freq > #{freqFaultValueMax} OR freq < #{freqFaultValueMin}")
    List<FreqMonitorEntity> getFreqFaults(float freqFaultValueMax, float freqFaultValueMin);

    @Select("SELECT * FROM node_info_all WHERE node_variable = '变电站' AND node_vol_level = #{volLevel}")
    List<VolMonitorEntity> getStationTables(Integer volLevel);

    @Select("SELECT ${columnName} AS voltageA FROM ${tableName} WHERE id = #{countDev}")
    float getStationVol(String columnName, String tableName, int countDev);

    @Select("SELECT * FROM faults_standard WHERE id=1")
    FaultsStandardEntity getFaultsStandard();

    void insertVolFault(List<VolMonitorEntity> volFinal);

    @Select("SELECT * FROM faults_vol_node")
    List<VolMonitorEntity> getVolFaults();

    void insertMinVol(List<VolMonitorEntity> listFinal);

    List<VolMonitorEntity> getMinVolStation(int volLevel);

    int getCurrentNumber();

    List<GridInfoEntity> getGridCapacity();

    List<GridInfoEntity> getInstallCapacity();

    List<NodeInfoEntity> getNodeInfoQueryByStation1(String name);

    List<NodeInfoEntity> getNodeInfoQueryByLine1Start(String name);

    List<NodeInfoEntity> getNodeInfoQueryByLine1End(String name);

    int setFaultsConfig(FaultsStandardEntity faultsStandardEntity);

    FaultsStandardEntity getFaultsConfig();
}
