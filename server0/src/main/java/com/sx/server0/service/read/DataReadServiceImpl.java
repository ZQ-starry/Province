package com.sx.server0.service.read;


import com.alibaba.fastjson.JSONObject;
import com.sx.server0.dao.read.DataReadDao;
import com.sx.server0.entity.common.BaseRes;
import com.sx.server0.entity.common.ResStatus;
import com.sx.server0.entity.data.FaultsStandardEntity;
import com.sx.server0.entity.req.*;
import com.sx.server0.entity.res.*;
import com.sx.server0.entity.result.GridInfoEntity;
import com.sx.server0.entity.result.GridInfoRes;
import com.sx.server0.util.TableAndColumnUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author: ZhangQi
 * @Date: 2023/7/21 10:48
 * @Description:
 */
@Service
public class DataReadServiceImpl implements DataReadService{

    @Autowired
    private DataReadDao dataReadDao;

    @Override
    public GridInfoRes getGridInfo() {
        GridInfoRes gridInfoRes = new GridInfoRes();
        gridInfoRes.setUser("zhejiang");
        List<GridInfoEntity> gridCapacity = dataReadDao.getGridCapacity();
        List<GridInfoEntity> installCapacity = dataReadDao.getInstallCapacity();
        if (gridCapacity.size()>0 && installCapacity.size()>0){
            gridInfoRes.setStatus(ResStatus.SUCCESS);
            gridInfoRes.setGridCapacity(gridCapacity);
            gridInfoRes.setInstallCapacity(installCapacity);
        }else {
            gridInfoRes.setStatus(ResStatus.FAILED);
        }
        return gridInfoRes;
    }

    @Override
    public ListsResEntity ssVoltageRead(NodeInfoReqEntity nodeInfoReqEntity) {
        ListsResEntity listsResEntity = new ListsResEntity();
        listsResEntity.setUser("zhejiang");
        try {
            if (nodeInfoReqEntity.getNodeNum() != null){
                listsResEntity.setNodeNum(nodeInfoReqEntity.getNodeNum());
                // 根据节点信息判断节点所在的表和字段编号
                NodeInfoEntity nodeInfoEntity = dataReadDao.getNodeInfo(nodeInfoReqEntity.getNodeNum());
                NodeInfoEntity nodeInfoAll = new TableAndColumnUtils().columnNames(nodeInfoEntity);
                // 时间轴查询
                List<String> timeList = dataReadDao.getTimeAxis();
                Collections.reverse(timeList);
                String[] times = new String[40];
                timeList.toArray(times);
                List<SsVolResEntity> lists = dataReadDao.getSsVoltage(nodeInfoAll);
                Collections.reverse(lists);
                if (lists.size()==0){
                    listsResEntity.setStatus(ResStatus.FAILED);
                }else {
                    listsResEntity.setSsVolResEntityList(lists);
                    listsResEntity.setTimes(times);
                    listsResEntity.setStatus(ResStatus.SUCCESS);
                }
            }else {
                listsResEntity.setStatus(ResStatus.FAILED);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return listsResEntity;
    }

    @Override
    public ListsResEntity lineCtAndPRead(NodeInfoReqEntity nodeInfoReqEntity) {
        ListsResEntity listsResEntity = new ListsResEntity();
        listsResEntity.setUser("JX");
        try {
            if (nodeInfoReqEntity.getNodeNum() != null){
                listsResEntity.setNodeNum(nodeInfoReqEntity.getNodeNum());
                // 根据节点信息判断节点所在的表和字段编号
                NodeInfoEntity nodeInfoEntity = dataReadDao.getNodeInfo(nodeInfoReqEntity.getNodeNum());
                NodeInfoEntity nodeInfoAll = new TableAndColumnUtils().columnNames(nodeInfoEntity);
                // 时间轴查询
                List<String> timeList = dataReadDao.getTimeAxis();
                Collections.reverse(timeList);
                String[] times = new String[40];
                timeList.toArray(times);
                List<LineCtAndPResEntity> lists = dataReadDao.getLineCtAndP(nodeInfoAll);
                Collections.reverse(lists);
                if (lists.size()<=0){
                    listsResEntity.setStatus(ResStatus.FAILED);
                }else {
                    listsResEntity.setLineCtAndPResEntityList(lists);
                    listsResEntity.setTimes(times);
                    listsResEntity.setStatus(ResStatus.SUCCESS);
                }
            }else {
                listsResEntity.setStatus(ResStatus.FAILED);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return listsResEntity;
    }

    @Override
    public ListsResEntity loadRateRead() {
        ListsResEntity listsResEntity = new ListsResEntity();
        listsResEntity.setUser("JX");
        try {
            List<LoadRateEntity> list1 = dataReadDao.getLoadRateLine();
            List<LoadRateEntity> list2 = dataReadDao.getLoadRateNode();
            list1.addAll(list2);
            if (list1.size() == 0){
                listsResEntity.setStatus(ResStatus.FAILED);
            }else {
                listsResEntity.setStatus(ResStatus.SUCCESS);
                listsResEntity.setLoadRateEntityList(list1);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return listsResEntity;
    }
    /**
     * 薄弱环节诊断
     * @return
     */
    @Override
    public FaultsListResEntity faultsRead() {
        FaultsListResEntity faultsLists = new FaultsListResEntity();
        faultsLists.setUser("zhejiang");
        // 查询标准
        FaultsStandardEntity faultsStandardEntity = dataReadDao.getFaultsStandard();
        float nodeFaultValue = faultsStandardEntity.getNodeFaultValue();
        float lineFaultValue = faultsStandardEntity.getLineFaultValue();
        float freqFaultValueMax = faultsStandardEntity.getFreqFaultValueMax();
        float freqFaultValueMin = faultsStandardEntity.getFreqFaultValueMin();
        try {
            // 查询线路负载率的历史记录，查询条件为前端设置的负载率标准 faults_load_rate_line
            List<LoadRateFaultEntity> listsLine = dataReadDao.getLineLoadRateFaults(lineFaultValue);
            // 查询变电站负载率的历史记录，查询条件为前端设置的负载率标准 faults_load_rate_node
            List<LoadRateFaultEntity> listsNode = dataReadDao.getNodeLoadRateFaults(nodeFaultValue);
            // 频率异常查询 monitor_freq
            List<FreqMonitorEntity> listsFreq = dataReadDao.getFreqFaults(freqFaultValueMax,freqFaultValueMin);
            /*变电站电压越限查询 faults_vol_node */
            List<VolMonitorEntity> listsVol = dataReadDao.getVolFaults();
            if (listsNode.size()>0 || listsLine.size()>0){
                faultsLists.setNodeLoadRateFaults(listsNode);
                faultsLists.setLineLoadRateFaults(listsLine);
                faultsLists.setFreqFaults(listsFreq);
                faultsLists.setVolFaults(listsVol);
                faultsLists.setStatus(ResStatus.SUCCESS);
            }else {
                faultsLists.setStatus(ResStatus.FAILED);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return faultsLists;
    }

    /**
     * 电压、频率监测
     * @return
     */
    @Override
    public ResMonitorEntity volAndFreqRead() {
        /* 查询每个时间点各电压等级变电站的 */
        ResMonitorEntity resMonitorEntity = new ResMonitorEntity();
        resMonitorEntity.setUser("zhejiang");
        // 最低电压监测，根据表名查询min_vol_station中每个时间点的最低电压
        List<VolMonitorEntity> volMonitorEntityList220 = new ArrayList<>();
        List<VolMonitorEntity> volMonitorEntityList500 = new ArrayList<>();
        List<VolMonitorEntity> volMonitorEntityList1000 = new ArrayList<>();
        List<FreqMonitorEntity> freqMonitorEntityList = new ArrayList<>();
        try {
            volMonitorEntityList220 = dataReadDao.getMinVolStation(220);
            resMonitorEntity.setVolMonitorEntityList220(volMonitorEntityList220);
            volMonitorEntityList500 = dataReadDao.getMinVolStation(500);
            resMonitorEntity.setVolMonitorEntityList500(volMonitorEntityList500);
            volMonitorEntityList1000 = dataReadDao.getMinVolStation(1000);
            resMonitorEntity.setVolMonitorEntityList1000(volMonitorEntityList1000);
            // 频率监测,此处为查询最新的20个历史数据
            freqMonitorEntityList = dataReadDao.getFreq();
            resMonitorEntity.setFreqMonitorEntityList(freqMonitorEntityList);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (freqMonitorEntityList.size()>0 || volMonitorEntityList220.size()>0 ||
                volMonitorEntityList500.size()>0 || volMonitorEntityList1000.size()>0){
            resMonitorEntity.setStatus(ResStatus.SUCCESS);
        }else {
            resMonitorEntity.setStatus(ResStatus.FAILED);
        }
        return resMonitorEntity;
    }

    @Override
    public ListsResEntity nodeInfoQueryByStation(StationReqEntity stationReqEntity) {
        ListsResEntity listsResEntity = new ListsResEntity();
        listsResEntity.setUser("zj");
        List<NodeInfoEntity> listFirst = new ArrayList<>();
        List<NodeInfoEntity> listFinal = new ArrayList<>();
        List<String> city = stationReqEntity.getCity();
        List<String> nodeVariable = stationReqEntity.getNodeVariable();
        List<Integer> nodeVolLevel = stationReqEntity.getNodeVolLevel();
        // 用一个表存放全省的线路和站点
        try {
            if (city.size() > 0){
                for (int i=0; i<city.size(); i++){
                    // 按城市进行条件查询
                    String cityName = city.get(i);
                    List<NodeInfoEntity> listCity = dataReadDao.getNodeInfoByCityOfStation(cityName);
                    listFirst.addAll(listCity);
                }
                if (nodeVariable.size()>0 && nodeVolLevel.size()>0) {
                    List<NodeInfoEntity> filteredFinal = new ArrayList<>();
                    // 先按照类型筛选
                    filteredByNodeVar(nodeVariable, listFirst, filteredFinal);//类型数组、被筛选集合、筛选完成的集合
                    // 再从上述结果中，按照电压等级再筛选一次
                    filteredByNodeVol(nodeVolLevel, filteredFinal, listFinal);//类型数组、被筛选集合、筛选完成的集合
                }else if (nodeVariable.size()>0 && nodeVolLevel.size()<=0){
                    // 按照类型筛选
                    filteredByNodeVar(nodeVariable, listFirst, listFinal);//类型数组、被筛选集合、筛选完成的集合
                }else if (nodeVariable.size()<=0 && nodeVolLevel.size()>0){
                    // 按照电压等级筛选
                    filteredByNodeVol(nodeVolLevel, listFirst, listFinal);//类型数组、被筛选集合、筛选完成的集合
                }else if (nodeVariable.size()<=0 && nodeVolLevel.size()<=0){
                    listFinal.addAll(listFirst);
                }
            }else {
                // 没有城市条件
                if (nodeVariable.size()>0 && nodeVolLevel.size()>0){
                    // 站点类型和站点电压等级都有条件，先根据站点类型从数据库进行条件查询
                    for (int i=0; i<nodeVariable.size(); i++){
                        // 按城市进行条件查询
                        String value = nodeVariable.get(i);
                        List<NodeInfoEntity> listVar = dataReadDao.getNodeInfoByNodeVar(value);
                        listFirst.addAll(listVar);
                    }
                    // 根据站点电压等级进行对查询的结果筛选
                    filteredByNodeVol(nodeVolLevel, listFirst, listFinal);//类型数组、被筛选集合、筛选完成的集合
                }else if (nodeVariable.size()>0 && nodeVolLevel.size()<=0) {
                    for (int i=0; i<nodeVariable.size(); i++){
                        // 按类型进行条件查询
                        String value = nodeVariable.get(i);
                        List<NodeInfoEntity> listVar = dataReadDao.getNodeInfoByNodeVar(value);
                        listFirst.addAll(listVar);
                    }
                    listFinal.addAll(listFirst);
                }else if (nodeVariable.size()<=0 && nodeVolLevel.size()>0){
                    for (int i=0; i<nodeVolLevel.size(); i++){
                        // 按电压等级进行条件查询
                        Integer value = nodeVolLevel.get(i);
                        List<NodeInfoEntity> listVol = dataReadDao.getNodeInfoByNodeVol(value);
                        listFirst.addAll(listVol);
                    }
                    listFinal.addAll(listFirst);
                }
            }
            listsResEntity.setNodeInfoEntityList(listFinal);
            listsResEntity.setStatus(ResStatus.SUCCESS);
            return listsResEntity;
        } catch (Exception e) {
            e.printStackTrace();
            listsResEntity.setNodeInfoEntityList(listFinal);
            listsResEntity.setStatus(ResStatus.FAILED);
            return listsResEntity;
        }
    }
    /**
     * 站点的搜索
     * @param nameInfo
     * @return
     */
    @Override
    public ListsResEntity nodeInfoQueryByStation1(JSONObject nameInfo) {
        ListsResEntity listsResEntity = new ListsResEntity();
        listsResEntity.setUser("zhejiang");
        String name = nameInfo.getString("name");
        // node_info_all查询确定名称的站点，采用模糊查询
        List<NodeInfoEntity> listNode = dataReadDao.getNodeInfoQueryByStation1(name);
        if (listNode.size()>0){
            listsResEntity.setStatus(ResStatus.SUCCESS);
            listsResEntity.setNodeInfoEntityList(listNode);
        }else {
            listsResEntity.setStatus(ResStatus.FAILED);
        }
        return listsResEntity;
    }

    @Override
    public ListsResEntity nodeInfoQueryByLine1(JSONObject nameInfo) {
        ListsResEntity listsResEntity = new ListsResEntity();
        listsResEntity.setUser("zhejiang");
        String name = nameInfo.getString("name");
        // node_info_all查询确定名称的站点，采用模糊查询
        List<NodeInfoEntity> listLineStart = dataReadDao.getNodeInfoQueryByLine1Start(name);
        List<NodeInfoEntity> listLineEnd = dataReadDao.getNodeInfoQueryByLine1End(name);
        listLineStart.addAll(listLineEnd);
        if (listLineStart.size()>0){
            listsResEntity.setStatus(ResStatus.SUCCESS);
            listsResEntity.setNodeInfoEntityList(listLineStart);
        }else {
            listsResEntity.setStatus(ResStatus.FAILED);
        }
        return listsResEntity;
    }

    @Override
    public ListsResEntity setFaultsConfig(FaultsStandardEntity faultsStandardEntity) {
        ListsResEntity listsResEntity = new ListsResEntity();
        listsResEntity.setUser("zhejiang");
        int flag = dataReadDao.setFaultsConfig(faultsStandardEntity);
        if (flag != 0){
            listsResEntity.setStatus(ResStatus.SUCCESS);
        }else {
            listsResEntity.setStatus(ResStatus.FAILED);
        }
        return listsResEntity;
    }

    @Override
    public ListsResEntity getFaultsConfig() {
        ListsResEntity listsResEntity = new ListsResEntity();
        listsResEntity.setUser("zhejiang");
        FaultsStandardEntity faultsStandardEntity = dataReadDao.getFaultsConfig();
        listsResEntity.setStatus(ResStatus.SUCCESS);
        listsResEntity.setFaultsStandardEntity(faultsStandardEntity);
        return listsResEntity;
    }

    @Override
    public ListsResEntity nodeInfoQueryByLine(LineReqEntity lineReqEntity) {
        // 线路根据首末端信息、城市、电压等级进行查询
        ListsResEntity listsResEntity = new ListsResEntity();
        listsResEntity.setUser("zj");
        List<NodeInfoEntity> listFirst = new ArrayList<>();
        // 最终返回结果的集合
        List<NodeInfoEntity> listFinal = new ArrayList<>();
        List<String> city = lineReqEntity.getCity();
        List<Integer> nodeVolLevel = lineReqEntity.getNodeVolLevel();
        String nodeNameStart = lineReqEntity.getNodeNameStart();
        String nodeNameEnd = lineReqEntity.getNodeNameEnd();
        try {
            if (city.size()>0){
                for (int i=0; i<city.size(); i++){
                    // 按城市进行条件查询
                    String cityName = city.get(i);
                    List<NodeInfoEntity> listCity = dataReadDao.getNodeInfoByCityOfLine(cityName);
                    listFirst.addAll(listCity);
                }
                if (nodeVolLevel.size()>0) {
                    List<NodeInfoEntity> filteredFinal = new ArrayList<>();
                    // 先按照电压等级筛选
                    filteredByNodeVol(nodeVolLevel, listFirst, filteredFinal);//类型数组、被筛选集合、筛选完成的集合
                    filteredByStartOrEnd(nodeNameStart, nodeNameEnd, filteredFinal, listFinal);//首端、末端、被筛选集合、筛选完成的集合
                }else {
                    // 电压等级条件为空
                    filteredByStartOrEnd(nodeNameStart, nodeNameEnd, listFirst, listFinal);//首端、末端、被筛选集合、筛选完成的集合
                }
            }else {
                // 城市条件为空
                if (nodeVolLevel.size()>0){
                    // 按照电压等级进行条件查询
                    for (int i=0; i<nodeVolLevel.size(); i++){
                        // 按电压等级进行条件查询
                        Integer value = nodeVolLevel.get(i);
                        List<NodeInfoEntity> listVar = dataReadDao.getLineInfoByNodeVol(value);
                        listFirst.addAll(listVar);
                    }
                    if (nodeNameStart == null && nodeNameEnd == null){
                        listFinal.addAll(listFirst);
                    }else {
                        // 根据首末端进行结果筛选
                        filteredByStartOrEnd(nodeNameStart, nodeNameEnd, listFirst, listFinal);//首端、末端、被筛选集合、筛选完成的集合
                    }
                }else {
                    // 电压等级条件为空
                    if (nodeNameStart.length()!=0 && nodeNameEnd.length()!=0){
                        // 按首末端进行条件查询
                        List<NodeInfoEntity> listStartAndEnd = dataReadDao.getLineInfoByStartAndEnd(nodeNameStart,nodeNameEnd);
                        listFinal.addAll(listStartAndEnd);
                    }else if (nodeNameStart.length()!=0 && nodeNameEnd.length()==0){
                        // 按首端进行条件查询
                        List<NodeInfoEntity> listStart = dataReadDao.getLineInfoByStart(nodeNameStart);
                        listFinal.addAll(listStart);
                    }else if (nodeNameStart.length()==0 && nodeNameEnd.length()!=0){
                        List<NodeInfoEntity> listStart = dataReadDao.getLineInfoByStart(nodeNameEnd);
                        listFinal.addAll(listStart);
                    }
                }
            }
            listsResEntity.setNodeInfoEntityList(listFinal);
            listsResEntity.setStatus(ResStatus.SUCCESS);
            return listsResEntity;
        } catch (Exception e) {
            e.printStackTrace();
            listsResEntity.setNodeInfoEntityList(listFinal);
            listsResEntity.setStatus(ResStatus.FAILED);
            return listsResEntity;
        }

    }

    private void filteredByStartOrEnd(String nodeNameStart, String nodeNameEnd, List<NodeInfoEntity> filteredFirst, List<NodeInfoEntity> filteredFinal) {
        if (nodeNameStart.length()!=0 && nodeNameEnd.length()!=0){
            // 首末端信息均不为空，进行筛选
            List<NodeInfoEntity> filteredMid = filteredFirst.stream()
                    .filter(entity -> nodeNameStart.equals(entity.getNodeNameStart())
                            && entity.getNodeNameEnd().equals(nodeNameEnd))
                    .collect(Collectors.toList());
            filteredFinal.addAll(filteredMid);
        }else if (nodeNameStart.length()!=0 && nodeNameEnd.length()==0){
            List<NodeInfoEntity> filteredMid = filteredFirst.stream()
                    .filter(entity -> nodeNameStart.equals(entity.getNodeNameStart()))
                    .collect(Collectors.toList());
            filteredFinal.addAll(filteredMid);
        }else if (nodeNameStart.length()==0 && nodeNameEnd.length()!=0){
            List<NodeInfoEntity> filteredMid = filteredFirst.stream()
                    .filter(entity -> nodeNameEnd.equals(entity.getNodeNameEnd()))
                    .collect(Collectors.toList());
            filteredFinal.addAll(filteredMid);
        }else if (nodeNameStart.length()==0 && nodeNameEnd.length()==0){
            filteredFinal.addAll(filteredFirst);
        }
    }
    /**
     * 类型数组、被筛选集合、筛选完成的集合
     * @param nodeVariable
     * @param filteredFirst
     * @param filteredFinal
     */
    private void filteredByNodeVar(List<String> nodeVariable, List<NodeInfoEntity> filteredFirst, List<NodeInfoEntity> filteredFinal) {
        for (int i = 0; i < nodeVariable.size(); i++) {
            String nodeVar = nodeVariable.get(i);
            List<NodeInfoEntity> filteredMid = filteredFirst.stream()
                    .filter(entity -> nodeVar.equals(entity.getNodeVariable()))
                    .collect(Collectors.toList());
            filteredFinal.addAll(filteredMid);
        }
    }

    /**
     * 类型数组、被筛选集合、筛选完成的集合
     * @param nodeVolLevel
     * @param filteredFirst
     * @param filteredFinal
     */
    private void filteredByNodeVol(List<Integer> nodeVolLevel, List<NodeInfoEntity> filteredFirst, List<NodeInfoEntity> filteredFinal) {
        for (int i=0; i<nodeVolLevel.size(); i++){
            Integer nodeVol = nodeVolLevel.get(i);
            List<NodeInfoEntity> filteredMid = filteredFirst.stream()
                    .filter(entity -> nodeVol.equals(entity.getNodeVolLevel()))
                    .collect(Collectors.toList());
            filteredFinal.addAll(filteredMid);
        }
    }
}
