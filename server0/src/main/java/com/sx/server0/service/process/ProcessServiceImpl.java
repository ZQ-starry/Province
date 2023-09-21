package com.sx.server0.service.process;


import com.sx.server0.component.AdsReadUtil;
import com.sx.server0.component.AllGVar;
import com.sx.server0.component.WebSocketService;
import com.sx.server0.dao.process.ProcessDao;
import com.sx.server0.entity.common.ResStatus;
import com.sx.server0.entity.data.PreLineInfoEntity;
import com.sx.server0.entity.data.PreNodeInfoEntity;
import com.sx.server0.entity.device.*;
import com.sx.server0.entity.res.LineCtAndPResEntity;
import com.sx.server0.entity.res.ListsResEntity;
import com.sx.server0.entity.res.LoadRateEntity;
import com.sx.server0.entity.res.SsVolResEntity;
import com.sx.server0.entity.result.LineResultEntity;
import com.sx.server0.entity.result.NodeResultEntity;
import com.sx.server0.entity.result.ResultListsEntity;
import com.sx.server0.service.read.DataReadServiceImpl;
import com.sx.server0.util.DateUtils;
import com.sx.server0.util.LoadRateUtil;
import com.sx.server0.util.VoltageUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author: ZhangQi
 * @Date: 2023/7/31 10:35
 * @Description:
 */
@Service
public class ProcessServiceImpl implements ProcessService {


    @Autowired
    private ProcessDao processDao;

    @Autowired
    private WebSocketService webSocketService;

    @Autowired
    private DataReadServiceImpl dataReadServiceImpl;


    @Override
    // @Async("task1")
    @Transactional
    public void dev011DataProcess() {
        /* 配置ADS读取信息，进行ADS读取 */
        Dev011Entity dev011Info = new Dev011Entity();
        DevCommonEntity devCommonEntity = new DevCommonEntity();
        BeanUtils.copyProperties(dev011Info, devCommonEntity);
        ResultListsEntity resultListsEntity = new AdsReadUtil().devStructRead(devCommonEntity);
        int size = resultListsEntity.getLineResultEntityList().size();
        if (size > 0) {
            /* 时间轴,获取时间并入库*/
            String[] times = new String[1];
            int id = 1;
            int countDev = processDao.getReadStatus(id);
            DateUtils dateUtils = new DateUtils();
            times[0] = dateUtils.timelineUtil(countDev + 1);
            processDao.updateReadStatus(id);
            processDao.insertTime(times[0]);
            // 取出线路结果数据、站点结果数据
            List<LineResultEntity> lineList = resultListsEntity.getLineResultEntityList();
            List<NodeResultEntity> nodeList = resultListsEntity.getNodeResultEntityList();
            /* 前后端通讯：对lineList和nodeList做一下拆分 */
            // 处理设备1的线路电流与功率数据
            List<PreLineInfoEntity> preLineInfos = AllGVar.DEV011_LINE_INFO;
            List<PreNodeInfoEntity> preNodeInfos = AllGVar.DEV011_NODE_INFO;
            for (int i = 0; i < dev011Info.getLineDutNum(); i++) {
                // 放入线路的编号，如zh001
                if (i != dev011Info.getLineDutNum() - 1) {
                    for (int j = 0; j < 15; j++) {
                        ListsResEntity listsResEntity = new ListsResEntity();
                        listsResEntity.setNodeNum(preLineInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        lineDataProcessor(lineList, i, j, listsResEntity);
                    }
                } else {
                    // 最后一个表数据不满
                    int end = dev011Info.getLineNum() % 15;
                    for (int j = 0; j < end; j++) {
                        ListsResEntity listsResEntity = new ListsResEntity();
                        listsResEntity.setNodeNum(preLineInfos.get(i * 15 + j).getNodeNum());
                        listsResEntity.setTimes(times);
                        lineDataProcessor(lineList, i, j, listsResEntity);
                    }
                }
            }
            // 处理设备1的节点电压数据
            for (int i = 0; i < dev011Info.getNodeDutNum(); i++) {
                if (i != dev011Info.getNodeDutNum() - 1) {
                    for (int j = 0; j < 15; j++) {
                        // 返回list放入节点的编号，如zh001
                        ListsResEntity listsResEntity = new ListsResEntity();
                        // 编号位置从设备1的线路编号最后一个的位置开始
                        listsResEntity.setNodeNum(preNodeInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        nodeDataProcessor(nodeList, i, j, listsResEntity);
                    }
                } else {
                    int end = dev011Info.getNodeNum()%15;
                    for (int j = 0; j < end; j++) {
                        // 返回list放入节点的编号，如zh001
                        ListsResEntity listsResEntity = new ListsResEntity();
                        // 编号位置从设备1的线路编号最后一个的位置开始
                        listsResEntity.setNodeNum(preNodeInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        nodeDataProcessor(nodeList, i, j, listsResEntity);
                    }
                }
            }
            List<String> devTableName = AllGVar.DEV011_Tab_Name;
            /* 执行数据插入 */
            for (int i = 0; i < dev011Info.getAllDutNum(); i++) {
                if (i < dev011Info.getLineDutNum()) {
                    processDao.insertLineResult(devTableName.get(i), lineList.get(i));
                } else {
                    processDao.insertNodeResult(devTableName.get(i), nodeList.get(i - dev011Info.getLineDutNum()));
                }
            }
            /* 线路负载率=单相电流/额定电流 */
            LoadRateUtil loadRateUtil = new LoadRateUtil();
            List<LineResultEntity> lineList1 = new ArrayList<>();
            // 取库中各线路的额定电流
            String lineRatedName = "capacity_line_dev011";
            String nodeRatedName = "capacity_node_dev011";
            Float[] ratedCurrents = processDao.getLineRatedCurrent(lineRatedName);
            for (int i=0; i<dev011Info.getLineDutNum(); i++) {
                // 从库中取出设备1的最新的一次线路的三相电流、有功无功等数据
                LineResultEntity lineResultEntity = processDao.selectLineData(devTableName.get(i));
                lineList1.add(lineResultEntity);
            }
            // 执行线路负载率的计算,把节点的编号一起传过去处理
            List<LoadRateEntity> lineLoadRatio = loadRateUtil.lineLoadRate(times,
                    preLineInfos, lineList1, ratedCurrents);
            // 执行更新dev1中线路负载率
            processDao.updateLineLoadRatio(lineLoadRatio);
            // 执行插入线路负载率历史记录表
            processDao.insertLineLoadRatio(lineLoadRatio);
            /* 变电站负载率=UI/额定容量 */
            // 取读取数据中的UI值
            Float[] uis = resultListsEntity.getNodeUI();
            // 取库中的变电站的额定容量
            Float[] nodeRatedCapacity = processDao.getNodeRatedCapacity(nodeRatedName);
            List<LoadRateEntity> nodeLoadRatio = loadRateUtil.nodeLoadRate(times,
                    preNodeInfos, uis, nodeRatedCapacity);
            // 更新库中的变电站负载率
            processDao.updateNodeLoadRatio(nodeLoadRatio);
            // 执行插入变电站负载率历史记录表
            processDao.insertNodeLoadRatio(nodeLoadRatio);
            /* 查询一次所有的负载率，发送给前端 */
            ListsResEntity listsResEntity1 = dataReadServiceImpl.loadRateRead();
            webSocketService.sendTextMsg("/response/getLoadRate", listsResEntity1);
            System.out.println("湖州01读取完毕");
            /* 频率的插入与主动发送 */
            float gridFreq = 50.00f;
            // 频率值保存表：monitor_freq
            processDao.insertFreq(times[0],gridFreq);
            webSocketService.sendTextMsg("/response/getFreq", 60);
            /* 电压越限计算 */
            VoltageUtils voltageUtils = new VoltageUtils();
            // // 电压越限数据表：faults_vol_node
            voltageUtils.voltageFault(times[0]);
            // /* 最低电压计算 最低电压表：min_vol_station */
            voltageUtils.minVoltageOfLevel(times[0]);
        } else {
            System.out.println("设备数据为空");
        }
    }

    @Override
    // @Async("task2")
    @Transactional
    public void dev012DataProcess() {
        Dev012Entity dev012Info = new Dev012Entity(); //修改实体 dev0162Info
        DevCommonEntity devCommonEntity = new DevCommonEntity();
        BeanUtils.copyProperties(dev012Info, devCommonEntity);
        ResultListsEntity resultListsEntity = new AdsReadUtil().devStructRead(devCommonEntity);
        int size = resultListsEntity.getLineResultEntityList().size();
        if (size > 0) {
            /* 时间轴,获取时间并入库*/
            String[] times = new String[1];
            int id = 2;
            int countDev = processDao.getReadStatus(id);
            DateUtils dateUtils = new DateUtils();
            times[0] = dateUtils.timelineUtil(countDev + 1);
            processDao.updateReadStatus(id);
            List<LineResultEntity> lineList = resultListsEntity.getLineResultEntityList();
            List<NodeResultEntity> nodeList = resultListsEntity.getNodeResultEntityList();
            /* 前后端通讯：对lineList和nodeList做一下拆分 */
            // 处理设备的线路电流与功率数据
            List<PreLineInfoEntity> preLineInfos = AllGVar.DEV012_LINE_INFO;
            List<PreNodeInfoEntity> preNodeInfos = AllGVar.DEV012_NODE_INFO;
            for (int i = 0; i < dev012Info.getLineDutNum(); i++) {
                // 放入线路的编号，如zh001
                if (i != dev012Info.getLineDutNum() - 1) {
                    for (int j = 0; j < 15; j++) {
                        ListsResEntity listsResEntity = new ListsResEntity();
                        listsResEntity.setNodeNum(preLineInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        lineDataProcessor(lineList, i, j, listsResEntity);
                    }
                } else {
                    // 最后一个表数据不满
                    int end = dev012Info.getLineNum() % 15;
                    for (int j = 0; j < end; j++) {
                        ListsResEntity listsResEntity = new ListsResEntity();
                        listsResEntity.setNodeNum(preLineInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        lineDataProcessor(lineList, i, j, listsResEntity);
                    }
                }
            }
            // 处理设备1的节点电压数据
            for (int i = 0; i < dev012Info.getNodeDutNum(); i++) {
                if (i != dev012Info.getNodeDutNum() - 1) {
                    for (int j = 0; j < 15; j++) {
                        // 返回list放入节点的编号，如zh001
                        ListsResEntity listsResEntity = new ListsResEntity();
                        // 编号位置从设备1的线路编号最后一个的位置开始
                        listsResEntity.setNodeNum(preNodeInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        nodeDataProcessor(nodeList, i, j, listsResEntity);
                    }
                } else {
                    int end = dev012Info.getNodeNum() % 15;
                    for (int j = 0; j < end; j++) {
                        // 返回list放入节点的编号，如zh001
                        ListsResEntity listsResEntity = new ListsResEntity();
                        // 编号位置从设备1的线路编号最后一个的位置开始
                        listsResEntity.setNodeNum(preNodeInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        nodeDataProcessor(nodeList, i, j, listsResEntity);
                    }
                }
            }
            /* 执行数据插入 */
            for (int i = 0; i < dev012Info.getAllDutNum(); i++) {
                if (i < dev012Info.getLineDutNum()) {
                    processDao.insertLineResult(AllGVar.DEV012_Tab_Name.get(i), lineList.get(i));
                } else {
                    processDao.insertNodeResult(AllGVar.DEV012_Tab_Name.get(i), nodeList.get(i - dev012Info.getLineDutNum()));
                }
            }
            /* 线路负载率=单相电流/额定电流 */
            LoadRateUtil loadRateUtil = new LoadRateUtil();
            List<LineResultEntity> lineList1 = new ArrayList<>();
            // 取库中各线路的额定电流
            String lineRatedName = "capacity_line_dev012";
            String nodeRatedName = "capacity_node_dev012";
            Float[] ratedCurrents = processDao.getLineRatedCurrent(lineRatedName);
            for (int i = 0; i < dev012Info.getLineDutNum(); i++) {
                // 从库中取出设备1的最新的一次线路的三相电流、有功无功等数据
                LineResultEntity lineResultEntity = processDao.selectLineData(AllGVar.DEV012_Tab_Name.get(i));
                lineList1.add(lineResultEntity);
            }
            // 执行线路负载率的计算,把节点的编号一起传过去处理
            List<LoadRateEntity> lineLoadRatio = loadRateUtil.lineLoadRate(times,
                    AllGVar.DEV012_LINE_INFO, lineList1, ratedCurrents);
            // 更新dev1中线路负载率
            processDao.updateLineLoadRatio(lineLoadRatio);
            // 执行插入线路负载率历史记录表
            processDao.insertLineLoadRatio(lineLoadRatio);
            /* 变电站负载率=UI/额定容量 */
            // 取读取数据中的UI值
            Float[] uis = resultListsEntity.getNodeUI();
            // 取库中的变电站的额定容量
            Float[] nodeRatedCapacity = processDao.getNodeRatedCapacity(nodeRatedName);
            List<LoadRateEntity> nodeLoadRatio = loadRateUtil.nodeLoadRate(times,
                    preNodeInfos, uis, nodeRatedCapacity);
            // 更新库中的变电站负载率
            processDao.updateNodeLoadRatio(nodeLoadRatio);
            // 执行插入变电站负载率历史记录表
            processDao.insertNodeLoadRatio(nodeLoadRatio);
            System.out.println("湖州02读取完毕");
        } else {
            System.out.println("设备数据为空");
        }
    }

    @Override
    // @Async("task1")
    @Transactional
    public void dev021DataProcess() {
        Dev021Entity dev021Info = new Dev021Entity(); //修改实体 dev0162Info
        DevCommonEntity devCommonEntity = new DevCommonEntity();
        BeanUtils.copyProperties(dev021Info, devCommonEntity);
        ResultListsEntity resultListsEntity = new AdsReadUtil().devStructRead(devCommonEntity);
        int size = resultListsEntity.getLineResultEntityList().size();
        if (size > 0) {
            /* 时间轴,获取时间并入库*/
            String[] times = new String[1];
            int id = 3;
            int countDev = processDao.getReadStatus(id);
            DateUtils dateUtils = new DateUtils();
            times[0] = dateUtils.timelineUtil(countDev + 1);
            processDao.updateReadStatus(id);
            List<LineResultEntity> lineList = resultListsEntity.getLineResultEntityList();
            List<NodeResultEntity> nodeList = resultListsEntity.getNodeResultEntityList();
            /* 前后端通讯：对lineList和nodeList做一下拆分 */
            // 处理设备1的线路电流与功率数据
            List<PreLineInfoEntity> preLineInfos = AllGVar.DEV021_LINE_INFO;
            List<PreNodeInfoEntity> preNodeInfos = AllGVar.DEV021_NODE_INFO;
            for (int i = 0; i < dev021Info.getLineDutNum(); i++) {
                // 放入线路的编号，如zh001
                if (i != dev021Info.getLineDutNum() - 1) {
                    for (int j = 0; j < 15; j++) {
                        ListsResEntity listsResEntity = new ListsResEntity();
                        listsResEntity.setNodeNum(preLineInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        lineDataProcessor(lineList, i, j, listsResEntity);
                    }
                } else {
                    // 最后一个表数据不满
                    int end = dev021Info.getLineNum() % 15;
                    for (int j = 0; j < end; j++) {
                        ListsResEntity listsResEntity = new ListsResEntity();
                        listsResEntity.setNodeNum(preLineInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        lineDataProcessor(lineList, i, j, listsResEntity);
                    }
                }
            }
            // 处理设备1的节点电压数据
            for (int i = 0; i < dev021Info.getNodeDutNum(); i++) {
                if (i != dev021Info.getNodeDutNum() - 1) {
                    for (int j = 0; j < 15; j++) {
                        // 返回list放入节点的编号，如zh001
                        ListsResEntity listsResEntity = new ListsResEntity();
                        // 编号位置从设备1的线路编号最后一个的位置开始
                        listsResEntity.setNodeNum(preNodeInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        nodeDataProcessor(nodeList, i, j, listsResEntity);
                    }
                } else {
                    int end = dev021Info.getNodeNum() % 15;
                    for (int j = 0; j < end; j++) {
                        // 返回list放入节点的编号，如zh001
                        ListsResEntity listsResEntity = new ListsResEntity();
                        // 编号位置从设备1的线路编号最后一个的位置开始
                        listsResEntity.setNodeNum(preNodeInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        nodeDataProcessor(nodeList, i, j, listsResEntity);
                    }
                }
            }
            /* 执行数据插入 */
            for (int i = 0; i < dev021Info.getAllDutNum(); i++) {
                if (i < dev021Info.getLineDutNum()) {
                    processDao.insertLineResult(AllGVar.DEV021_Tab_Name.get(i), lineList.get(i));
                } else {
                    processDao.insertNodeResult(AllGVar.DEV021_Tab_Name.get(i), nodeList.get(i - dev021Info.getLineDutNum()));
                }
            }
            /* 线路负载率=单相电流/额定电流 */
            LoadRateUtil loadRateUtil = new LoadRateUtil();
            List<LineResultEntity> lineList1 = new ArrayList<>();
            // 取库中各线路的额定电流
            String lineRatedName = "capacity_line_dev021";
            String nodeRatedName = "capacity_node_dev021";
            Float[] ratedCurrents = processDao.getLineRatedCurrent(lineRatedName);
            for (int i = 0; i < dev021Info.getLineDutNum(); i++) {
                // 从库中取出设备1的最新的一次线路的三相电流、有功无功等数据
                LineResultEntity lineResultEntity = processDao.selectLineData(AllGVar.DEV021_Tab_Name.get(i));
                lineList1.add(lineResultEntity);
            }
            // 执行线路负载率的计算,把节点的编号一起传过去处理
            List<LoadRateEntity> lineLoadRatio = loadRateUtil.lineLoadRate(times,
                    AllGVar.DEV021_LINE_INFO, lineList1, ratedCurrents);
            // 更新dev1中线路负载率
            processDao.updateLineLoadRatio(lineLoadRatio);
            // 执行插入线路负载率历史记录表
            processDao.insertLineLoadRatio(lineLoadRatio);
            /* 变电站负载率=UI/额定容量 */
            // 取读取数据中的UI值
            Float[] uis = resultListsEntity.getNodeUI();
            // 取库中的变电站的额定容量
            Float[] nodeRatedCapacity = processDao.getNodeRatedCapacity(nodeRatedName);
            List<LoadRateEntity> nodeLoadRatio = loadRateUtil.nodeLoadRate(times,
                    preNodeInfos, uis, nodeRatedCapacity);
            // 更新库中的变电站负载率
            processDao.updateNodeLoadRatio(nodeLoadRatio);
            // 执行插入变电站负载率历史记录表
            processDao.insertNodeLoadRatio(nodeLoadRatio);
            /**
             * 查询一次所有的负载率，发送给前端
             */
            ListsResEntity listsResEntity1 = dataReadServiceImpl.loadRateRead();
            webSocketService.sendTextMsg("/response/getLoadRate", listsResEntity1);
            System.out.println("嘉兴01读取结束");
        } else {
            System.out.println("设备数据为空");
        }
    }

    @Override
    // @Async("task2")
    @Transactional
    public void dev022DataProcess() {
        Dev022Entity dev022Info = new Dev022Entity(); //修改实体 dev0162Info
        DevCommonEntity devCommonEntity = new DevCommonEntity();
        BeanUtils.copyProperties(dev022Info, devCommonEntity);
        ResultListsEntity resultListsEntity = new AdsReadUtil().devStructRead(devCommonEntity);
        int size = resultListsEntity.getLineResultEntityList().size();
        if (size > 0) {
            /* 时间轴,获取时间并入库*/
            String[] times = new String[1];
            int id = 4;
            int countDev = processDao.getReadStatus(id);
            DateUtils dateUtils = new DateUtils();
            times[0] = dateUtils.timelineUtil(countDev + 1);
            processDao.updateReadStatus(id);
            List<LineResultEntity> lineList = resultListsEntity.getLineResultEntityList();
            List<NodeResultEntity> nodeList = resultListsEntity.getNodeResultEntityList();
            /* 前后端通讯：对lineList和nodeList做一下拆分 */
            // 处理设备1的线路电流与功率数据
            List<PreLineInfoEntity> preLineInfos = AllGVar.DEV022_LINE_INFO;
            List<PreNodeInfoEntity> preNodeInfos = AllGVar.DEV022_NODE_INFO;
            for (int i = 0; i < dev022Info.getLineDutNum(); i++) {
                // 放入线路的编号，如zh001
                if (i != dev022Info.getLineDutNum() - 1) {
                    for (int j = 0; j < 15; j++) {
                        ListsResEntity listsResEntity = new ListsResEntity();
                        listsResEntity.setNodeNum(preLineInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        lineDataProcessor(lineList, i, j, listsResEntity);
                    }
                } else {
                    // 最后一个表数据不满
                    int end = dev022Info.getLineNum() % 15;
                    for (int j = 0; j < end; j++) {
                        ListsResEntity listsResEntity = new ListsResEntity();
                        listsResEntity.setNodeNum(preLineInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        lineDataProcessor(lineList, i, j, listsResEntity);
                    }
                }
            }
            // 处理设备1的节点电压数据
            for (int i = 0; i < dev022Info.getNodeDutNum(); i++) {
                if (i != dev022Info.getNodeDutNum() - 1) {
                    for (int j = 0; j < 15; j++) {
                        // 返回list放入节点的编号，如zh001
                        ListsResEntity listsResEntity = new ListsResEntity();
                        // 编号位置从设备1的线路编号最后一个的位置开始
                        listsResEntity.setNodeNum(preNodeInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        nodeDataProcessor(nodeList, i, j, listsResEntity);
                    }
                } else {
                    int end = dev022Info.getNodeNum() % 15;
                    for (int j = 0; j < end; j++) {
                        // 返回list放入节点的编号，如zh001
                        ListsResEntity listsResEntity = new ListsResEntity();
                        // 编号位置从设备1的线路编号最后一个的位置开始
                        listsResEntity.setNodeNum(preNodeInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        nodeDataProcessor(nodeList, i, j, listsResEntity);
                    }
                }
            }
            /* 执行数据插入 */
            for (int i = 0; i < dev022Info.getAllDutNum(); i++) {
                if (i < dev022Info.getLineDutNum()) {
                    processDao.insertLineResult(AllGVar.DEV022_Tab_Name.get(i), lineList.get(i));
                } else {
                    processDao.insertNodeResult(AllGVar.DEV022_Tab_Name.get(i), nodeList.get(i - dev022Info.getLineDutNum()));
                }
            }
            /* 线路负载率=单相电流/额定电流 */
            LoadRateUtil loadRateUtil = new LoadRateUtil();
            List<LineResultEntity> lineList1 = new ArrayList<>();
            // 取库中各线路的额定电流
            String lineRatedName = "capacity_line_dev022";
            String nodeRatedName = "capacity_node_dev022";
            Float[] ratedCurrents = processDao.getLineRatedCurrent(lineRatedName);
            for (int i = 0; i < dev022Info.getLineDutNum(); i++) {
                // 从库中取出设备1的最新的一次线路的三相电流、有功无功等数据
                LineResultEntity lineResultEntity = processDao.selectLineData(AllGVar.DEV022_Tab_Name.get(i));
                lineList1.add(lineResultEntity);
            }
            // 执行线路负载率的计算,把节点的编号一起传过去处理
            List<LoadRateEntity> lineLoadRatio = loadRateUtil.lineLoadRate(times,
                    AllGVar.DEV022_LINE_INFO, lineList1, ratedCurrents);
            // 更新dev1中线路负载率
            processDao.updateLineLoadRatio(lineLoadRatio);
            // 执行插入线路负载率历史记录表
            processDao.insertLineLoadRatio(lineLoadRatio);
            /* 变电站负载率=UI/额定容量 */
            // 取读取数据中的UI值
            Float[] uis = resultListsEntity.getNodeUI();
            // 取库中的变电站的额定容量
            Float[] nodeRatedCapacity = processDao.getNodeRatedCapacity(nodeRatedName);
            List<LoadRateEntity> nodeLoadRatio = loadRateUtil.nodeLoadRate(times,
                    preNodeInfos, uis, nodeRatedCapacity);
            // 更新库中的变电站负载率
            processDao.updateNodeLoadRatio(nodeLoadRatio);
            // 执行插入变电站负载率历史记录表
            processDao.insertNodeLoadRatio(nodeLoadRatio);
            System.out.println("嘉兴02读取结束");
        } else {
            System.out.println("设备数据为空");
        }
    }

    @Override
    // @Async("task1")
    @Transactional
    public void dev031DataProcess() {
        Dev031Entity dev031Info = new Dev031Entity(); //修改实体 dev0162Info
        DevCommonEntity devCommonEntity = new DevCommonEntity();
        BeanUtils.copyProperties(dev031Info, devCommonEntity);
        ResultListsEntity resultListsEntity = new AdsReadUtil().devStructRead(devCommonEntity);
        int size = resultListsEntity.getLineResultEntityList().size();
        if (size > 0) {
            /* 时间轴,获取时间并入库*/
            String[] times = new String[1];
            int id = 5;
            int countDev = processDao.getReadStatus(id);
            DateUtils dateUtils = new DateUtils();
            times[0] = dateUtils.timelineUtil(countDev + 1);
            processDao.updateReadStatus(id);
            List<LineResultEntity> lineList = resultListsEntity.getLineResultEntityList();
            List<NodeResultEntity> nodeList = resultListsEntity.getNodeResultEntityList();
            /* 前后端通讯：对lineList和nodeList做一下拆分 */
            // 处理设备1的线路电流与功率数据
            List<PreLineInfoEntity> preLineInfos = AllGVar.DEV031_LINE_INFO;
            List<PreNodeInfoEntity> preNodeInfos = AllGVar.DEV031_NODE_INFO;
            for (int i = 0; i < dev031Info.getLineDutNum(); i++) {
                // 放入线路的编号，如zh001
                if (i != dev031Info.getLineDutNum() - 1) {
                    for (int j = 0; j < 15; j++) {
                        ListsResEntity listsResEntity = new ListsResEntity();
                        listsResEntity.setNodeNum(preLineInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        lineDataProcessor(lineList, i, j, listsResEntity);
                    }
                } else {
                    // 最后一个表数据不满
                    int end = dev031Info.getLineNum() % 15;
                    for (int j = 0; j < end; j++) {
                        ListsResEntity listsResEntity = new ListsResEntity();
                        listsResEntity.setNodeNum(preLineInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        lineDataProcessor(lineList, i, j, listsResEntity);
                    }
                }
            }
            // 处理设备1的节点电压数据
            for (int i = 0; i < dev031Info.getNodeDutNum(); i++) {
                if (i != dev031Info.getNodeDutNum() - 1) {
                    for (int j = 0; j < 15; j++) {
                        // 返回list放入节点的编号，如zh001
                        ListsResEntity listsResEntity = new ListsResEntity();
                        // 编号位置从设备1的线路编号最后一个的位置开始
                        listsResEntity.setNodeNum(preNodeInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        nodeDataProcessor(nodeList, i, j, listsResEntity);
                    }
                } else {
                    int end = dev031Info.getNodeNum() % 15;
                    for (int j = 0; j < end; j++) {
                        // 返回list放入节点的编号，如zh001
                        ListsResEntity listsResEntity = new ListsResEntity();
                        // 编号位置从设备1的线路编号最后一个的位置开始
                        listsResEntity.setNodeNum(preNodeInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        nodeDataProcessor(nodeList, i, j, listsResEntity);
                    }
                }
            }
            /* 执行数据插入 */
            for (int i = 0; i < dev031Info.getAllDutNum(); i++) {
                if (i < dev031Info.getLineDutNum()) {
                    processDao.insertLineResult(AllGVar.DEV031_Tab_Name.get(i), lineList.get(i));
                } else {
                    processDao.insertNodeResult(AllGVar.DEV031_Tab_Name.get(i), nodeList.get(i - dev031Info.getLineDutNum()));
                }
            }
            /* 线路负载率=单相电流/额定电流 */
            LoadRateUtil loadRateUtil = new LoadRateUtil();
            List<LineResultEntity> lineList1 = new ArrayList<>();
            // 取库中各线路的额定电流
            String lineRatedName = "capacity_line_dev031";
            String nodeRatedName = "capacity_node_dev031";
            Float[] ratedCurrents = processDao.getLineRatedCurrent(lineRatedName);
            for (int i = 0; i < dev031Info.getLineDutNum(); i++) {
                // 从库中取出设备1的最新的一次线路的三相电流、有功无功等数据
                LineResultEntity lineResultEntity = processDao.selectLineData(AllGVar.DEV031_Tab_Name.get(i));
                lineList1.add(lineResultEntity);
            }
            // 执行线路负载率的计算,把节点的编号一起传过去处理
            List<LoadRateEntity> lineLoadRatio = loadRateUtil.lineLoadRate(times,
                    AllGVar.DEV031_LINE_INFO, lineList1, ratedCurrents);
            // 更新dev1中线路负载率
            processDao.updateLineLoadRatio(lineLoadRatio);
            // 执行插入线路负载率历史记录表
            processDao.insertLineLoadRatio(lineLoadRatio);
            /* 变电站负载率=UI/额定容量 */
            // 取读取数据中的UI值
            Float[] uis = resultListsEntity.getNodeUI();
            // 取库中的变电站的额定容量
            Float[] nodeRatedCapacity = processDao.getNodeRatedCapacity(nodeRatedName);
            List<LoadRateEntity> nodeLoadRatio = loadRateUtil.nodeLoadRate(times,
                    preNodeInfos, uis, nodeRatedCapacity);
            // 更新库中的变电站负载率
            processDao.updateNodeLoadRatio(nodeLoadRatio);
            // 执行插入变电站负载率历史记录表
            processDao.insertNodeLoadRatio(nodeLoadRatio);
            /* 查询一次所有的负载率，发送给前端 */
            ListsResEntity listsResEntity1 = dataReadServiceImpl.loadRateRead();
            webSocketService.sendTextMsg("/response/getLoadRate", listsResEntity1);
            System.out.println("温州01读取完毕");
        } else {
            System.out.println("设备数据为空");
        }
    }

    @Override
    // @Async("task2")
    @Transactional
    public void dev032DataProcess() {
        Dev032Entity dev032Info = new Dev032Entity(); //修改实体 dev0162Info
        DevCommonEntity devCommonEntity = new DevCommonEntity();
        BeanUtils.copyProperties(dev032Info, devCommonEntity);
        ResultListsEntity resultListsEntity = new AdsReadUtil().devStructRead(devCommonEntity);
        int size = resultListsEntity.getLineResultEntityList().size();
        if (size > 0) {
            /* 时间轴,获取时间并入库*/
            String[] times = new String[1];
            int id = 6;
            int countDev = processDao.getReadStatus(id);
            DateUtils dateUtils = new DateUtils();
            times[0] = dateUtils.timelineUtil(countDev + 1);
            processDao.updateReadStatus(id);
            List<LineResultEntity> lineList = resultListsEntity.getLineResultEntityList();
            List<NodeResultEntity> nodeList = resultListsEntity.getNodeResultEntityList();
            /* 前后端通讯：对lineList和nodeList做一下拆分 */
            // 处理设备1的线路电流与功率数据
            List<PreLineInfoEntity> preLineInfos = AllGVar.DEV032_LINE_INFO;
            List<PreNodeInfoEntity> preNodeInfos = AllGVar.DEV032_NODE_INFO;
            for (int i = 0; i < dev032Info.getLineDutNum(); i++) {
                // 放入线路的编号，如zh001
                if (i != dev032Info.getLineDutNum() - 1) {
                    for (int j = 0; j < 15; j++) {
                        ListsResEntity listsResEntity = new ListsResEntity();
                        listsResEntity.setNodeNum(preLineInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        lineDataProcessor(lineList, i, j, listsResEntity);
                    }
                } else {
                    // 最后一个表数据不满
                    int end = dev032Info.getLineNum() % 15;
                    for (int j = 0; j < end; j++) {
                        ListsResEntity listsResEntity = new ListsResEntity();
                        listsResEntity.setNodeNum(preLineInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        lineDataProcessor(lineList, i, j, listsResEntity);
                    }
                }
            }
            // 处理设备1的节点电压数据
            for (int i = 0; i < dev032Info.getNodeDutNum(); i++) {
                if (i != dev032Info.getNodeDutNum() - 1) {
                    for (int j = 0; j < 15; j++) {
                        // 返回list放入节点的编号，如zh001
                        ListsResEntity listsResEntity = new ListsResEntity();
                        // 编号位置从设备1的线路编号最后一个的位置开始
                        listsResEntity.setNodeNum(preNodeInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        nodeDataProcessor(nodeList, i, j, listsResEntity);
                    }
                } else {
                    int end = dev032Info.getNodeNum() % 15;
                    for (int j = 0; j < end; j++) {
                        // 返回list放入节点的编号，如zh001
                        ListsResEntity listsResEntity = new ListsResEntity();
                        // 编号位置从设备1的线路编号最后一个的位置开始
                        listsResEntity.setNodeNum(preNodeInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        nodeDataProcessor(nodeList, i, j, listsResEntity);
                    }
                }
            }
            /* 执行数据插入 */
            for (int i = 0; i < dev032Info.getAllDutNum(); i++) {
                if (i < dev032Info.getLineDutNum()) {
                    processDao.insertLineResult(AllGVar.DEV032_Tab_Name.get(i), lineList.get(i));
                } else {
                    processDao.insertNodeResult(AllGVar.DEV032_Tab_Name.get(i), nodeList.get(i - dev032Info.getLineDutNum()));
                }
            }
            /* 线路负载率=单相电流/额定电流 */
            LoadRateUtil loadRateUtil = new LoadRateUtil();
            List<LineResultEntity> lineList1 = new ArrayList<>();
            // 取库中各线路的额定电流
            String lineRatedName = "capacity_line_dev032";
            String nodeRatedName = "capacity_node_dev032";
            Float[] ratedCurrents = processDao.getLineRatedCurrent(lineRatedName);
            for (int i = 0; i < dev032Info.getLineDutNum(); i++) {
                // 从库中取出设备1的最新的一次线路的三相电流、有功无功等数据
                LineResultEntity lineResultEntity = processDao.selectLineData(AllGVar.DEV032_Tab_Name.get(i));
                lineList1.add(lineResultEntity);
            }
            // 执行线路负载率的计算,把节点的编号一起传过去处理
            List<LoadRateEntity> lineLoadRatio = loadRateUtil.lineLoadRate(times,
                    AllGVar.DEV032_LINE_INFO, lineList1, ratedCurrents);
            // 更新dev1中线路负载率
            processDao.updateLineLoadRatio(lineLoadRatio);
            // 执行插入线路负载率历史记录表
            processDao.insertLineLoadRatio(lineLoadRatio);
            /* 变电站负载率=UI/额定容量 */
            // 取读取数据中的UI值
            Float[] uis = resultListsEntity.getNodeUI();
            // 取库中的变电站的额定容量
            Float[] nodeRatedCapacity = processDao.getNodeRatedCapacity(nodeRatedName);
            List<LoadRateEntity> nodeLoadRatio = loadRateUtil.nodeLoadRate(times,
                    preNodeInfos, uis, nodeRatedCapacity);
            // 更新库中的变电站负载率
            processDao.updateNodeLoadRatio(nodeLoadRatio);
            // 执行插入变电站负载率历史记录表
            processDao.insertNodeLoadRatio(nodeLoadRatio);
            System.out.println("温州02读取完毕");
        } else {
            System.out.println("设备数据为空");
        }
    }

    @Override
    // @Async("task1")
    @Transactional
    public void dev041DataProcess() {
        Dev041Entity dev041Info = new Dev041Entity(); //修改实体 dev0162Info
        DevCommonEntity devCommonEntity = new DevCommonEntity();
        BeanUtils.copyProperties(dev041Info, devCommonEntity);
        ResultListsEntity resultListsEntity = new AdsReadUtil().devStructRead(devCommonEntity);
        int size = resultListsEntity.getLineResultEntityList().size();
        if (size > 0) {
            /* 时间轴,获取时间并入库*/
            String[] times = new String[1];
            int id = 7;
            int countDev = processDao.getReadStatus(id);
            DateUtils dateUtils = new DateUtils();
            times[0] = dateUtils.timelineUtil(countDev + 1);
            processDao.updateReadStatus(id);
            List<LineResultEntity> lineList = resultListsEntity.getLineResultEntityList();
            List<NodeResultEntity> nodeList = resultListsEntity.getNodeResultEntityList();
            /* 前后端通讯：对lineList和nodeList做一下拆分 */
            // 处理设备1的线路电流与功率数据
            List<PreLineInfoEntity> preLineInfos = AllGVar.DEV041_LINE_INFO;
            List<PreNodeInfoEntity> preNodeInfos = AllGVar.DEV041_NODE_INFO;
            for (int i = 0; i < dev041Info.getLineDutNum(); i++) {
                // 放入线路的编号，如zh001
                if (i != dev041Info.getLineDutNum() - 1) {
                    for (int j = 0; j < 15; j++) {
                        ListsResEntity listsResEntity = new ListsResEntity();
                        listsResEntity.setNodeNum(preLineInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        lineDataProcessor(lineList, i, j, listsResEntity);
                    }
                } else {
                    // 最后一个表数据不满
                    int end = dev041Info.getLineNum() % 15;
                    for (int j = 0; j < end; j++) {
                        ListsResEntity listsResEntity = new ListsResEntity();
                        listsResEntity.setNodeNum(preLineInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        lineDataProcessor(lineList, i, j, listsResEntity);
                    }
                }
            }
            // 处理设备1的节点电压数据
            for (int i = 0; i < dev041Info.getNodeDutNum(); i++) {
                if (i != dev041Info.getNodeDutNum() - 1) {
                    for (int j = 0; j < 15; j++) {
                        // 返回list放入节点的编号，如zh001
                        ListsResEntity listsResEntity = new ListsResEntity();
                        // 编号位置从设备1的线路编号最后一个的位置开始
                        listsResEntity.setNodeNum(preNodeInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        nodeDataProcessor(nodeList, i, j, listsResEntity);
                    }
                } else {
                    int end = dev041Info.getNodeNum() % 15;
                    for (int j = 0; j < end; j++) {
                        // 返回list放入节点的编号，如zh001
                        ListsResEntity listsResEntity = new ListsResEntity();
                        // 编号位置从设备1的线路编号最后一个的位置开始
                        listsResEntity.setNodeNum(preNodeInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        nodeDataProcessor(nodeList, i, j, listsResEntity);
                    }
                }
            }
            /* 执行数据插入 */
            for (int i = 0; i < dev041Info.getAllDutNum(); i++) {
                if (i < dev041Info.getLineDutNum()) {
                    processDao.insertLineResult(AllGVar.DEV041_Tab_Name.get(i), lineList.get(i));
                } else {
                    processDao.insertNodeResult(AllGVar.DEV041_Tab_Name.get(i), nodeList.get(i - dev041Info.getLineDutNum()));
                }
            }
            /* 线路负载率=单相电流/额定电流 */
            LoadRateUtil loadRateUtil = new LoadRateUtil();
            List<LineResultEntity> lineList1 = new ArrayList<>();
            // 取库中各线路的额定电流
            String lineRatedName = "capacity_line_dev041";
            String nodeRatedName = "capacity_node_dev041";
            Float[] ratedCurrents = processDao.getLineRatedCurrent(lineRatedName);
            for (int i = 0; i < dev041Info.getLineDutNum(); i++) {
                // 从库中取出设备1的最新的一次线路的三相电流、有功无功等数据
                LineResultEntity lineResultEntity = processDao.selectLineData(AllGVar.DEV041_Tab_Name.get(i));
                lineList1.add(lineResultEntity);
            }
            // 执行线路负载率的计算,把节点的编号一起传过去处理
            List<LoadRateEntity> lineLoadRatio = loadRateUtil.lineLoadRate(times,
                    AllGVar.DEV041_LINE_INFO, lineList1, ratedCurrents);
            // 更新dev1中线路负载率
            processDao.updateLineLoadRatio(lineLoadRatio);
            // 执行插入线路负载率历史记录表
            processDao.insertLineLoadRatio(lineLoadRatio);
            /* 变电站负载率=UI/额定容量 */
            // 取读取数据中的UI值
            Float[] uis = resultListsEntity.getNodeUI();
            // 取库中的变电站的额定容量
            Float[] nodeRatedCapacity = processDao.getNodeRatedCapacity(nodeRatedName);
            List<LoadRateEntity> nodeLoadRatio = loadRateUtil.nodeLoadRate(times,
                    preNodeInfos, uis, nodeRatedCapacity);
            // 更新库中的变电站负载率
            processDao.updateNodeLoadRatio(nodeLoadRatio);
            // 执行插入变电站负载率历史记录表
            processDao.insertNodeLoadRatio(nodeLoadRatio);
            System.out.println("温州03读取完毕");
        } else {
            System.out.println("设备数据为空");
        }
    }

    @Override
    // @Async("task2")
    @Transactional
    public void dev042DataProcess() {
        Dev042Entity dev042Info = new Dev042Entity(); //修改实体 dev0162Info
        DevCommonEntity devCommonEntity = new DevCommonEntity();
        BeanUtils.copyProperties(dev042Info, devCommonEntity);
        ResultListsEntity resultListsEntity = new AdsReadUtil().devStructRead(devCommonEntity);
        int size = resultListsEntity.getLineResultEntityList().size();
        if (size > 0) {
            /* 时间轴,获取时间并入库*/
            String[] times = new String[1];
            int id = 8;
            int countDev = processDao.getReadStatus(id);
            DateUtils dateUtils = new DateUtils();
            times[0] = dateUtils.timelineUtil(countDev + 1);
            processDao.updateReadStatus(id);
            List<LineResultEntity> lineList = resultListsEntity.getLineResultEntityList();
            List<NodeResultEntity> nodeList = resultListsEntity.getNodeResultEntityList();
            /* 前后端通讯：对lineList和nodeList做一下拆分 */
            // 处理设备1的线路电流与功率数据
            List<PreLineInfoEntity> preLineInfos = AllGVar.DEV042_LINE_INFO;
            List<PreNodeInfoEntity> preNodeInfos = AllGVar.DEV042_NODE_INFO;
            for (int i = 0; i < dev042Info.getLineDutNum(); i++) {
                // 放入线路的编号，如zh001
                if (i != dev042Info.getLineDutNum() - 1) {
                    for (int j = 0; j < 15; j++) {
                        ListsResEntity listsResEntity = new ListsResEntity();
                        listsResEntity.setNodeNum(preLineInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        lineDataProcessor(lineList, i, j, listsResEntity);
                    }
                } else {
                    // 最后一个表数据不满
                    int end = dev042Info.getLineNum() % 15;
                    for (int j = 0; j < end; j++) {
                        ListsResEntity listsResEntity = new ListsResEntity();
                        listsResEntity.setNodeNum(preLineInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        lineDataProcessor(lineList, i, j, listsResEntity);
                    }
                }
            }
            // 处理设备1的节点电压数据
            for (int i = 0; i < dev042Info.getNodeDutNum(); i++) {
                if (i != dev042Info.getNodeDutNum() - 1) {
                    for (int j = 0; j < 15; j++) {
                        // 返回list放入节点的编号，如zh001
                        ListsResEntity listsResEntity = new ListsResEntity();
                        // 编号位置从设备1的线路编号最后一个的位置开始
                        listsResEntity.setNodeNum(preNodeInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        nodeDataProcessor(nodeList, i, j, listsResEntity);
                    }
                } else {
                    int end = dev042Info.getNodeNum() % 15;
                    for (int j = 0; j < end; j++) {
                        // 返回list放入节点的编号，如zh001
                        ListsResEntity listsResEntity = new ListsResEntity();
                        // 编号位置从设备1的线路编号最后一个的位置开始
                        listsResEntity.setNodeNum(preNodeInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        nodeDataProcessor(nodeList, i, j, listsResEntity);
                    }
                }
            }
            /* 执行数据插入 */
            for (int i = 0; i < dev042Info.getAllDutNum(); i++) {
                if (i < dev042Info.getLineDutNum()) {
                    processDao.insertLineResult(AllGVar.DEV042_Tab_Name.get(i), lineList.get(i));
                } else {
                    processDao.insertNodeResult(AllGVar.DEV042_Tab_Name.get(i), nodeList.get(i - dev042Info.getLineDutNum()));
                }
            }
            /* 线路负载率=单相电流/额定电流 */
            LoadRateUtil loadRateUtil = new LoadRateUtil();
            List<LineResultEntity> lineList1 = new ArrayList<>();
            // 取库中各线路的额定电流
            String lineRatedName = "capacity_line_dev041";
            String nodeRatedName = "capacity_node_dev041";
            Float[] ratedCurrents = processDao.getLineRatedCurrent(lineRatedName);
            for (int i = 0; i < dev042Info.getLineDutNum(); i++) {
                // 从库中取出设备1的最新的一次线路的三相电流、有功无功等数据
                LineResultEntity lineResultEntity = processDao.selectLineData(AllGVar.DEV042_Tab_Name.get(i));
                lineList1.add(lineResultEntity);
            }
            // 执行线路负载率的计算,把节点的编号一起传过去处理
            List<LoadRateEntity> lineLoadRatio = loadRateUtil.lineLoadRate(times,
                    AllGVar.DEV042_LINE_INFO, lineList1, ratedCurrents);
            // 更新dev1中线路负载率
            processDao.updateLineLoadRatio(lineLoadRatio);
            // 执行插入线路负载率历史记录表
            processDao.insertLineLoadRatio(lineLoadRatio);
            /* 变电站负载率=UI/额定容量 */
            // 取读取数据中的UI值
            Float[] uis = resultListsEntity.getNodeUI();
            // 取库中的变电站的额定容量
            Float[] nodeRatedCapacity = processDao.getNodeRatedCapacity(nodeRatedName);
            List<LoadRateEntity> nodeLoadRatio = loadRateUtil.nodeLoadRate(times,
                    preNodeInfos, uis, nodeRatedCapacity);
            // 更新库中的变电站负载率
            processDao.updateNodeLoadRatio(nodeLoadRatio);
            // 执行插入变电站负载率历史记录表
            processDao.insertNodeLoadRatio(nodeLoadRatio);
            System.out.println("温州03读取完毕");
        } else {
            System.out.println("设备数据为空");
        }
    }

    @Override
    // @Async("task1")
    @Transactional
    public void dev051DataProcess() {
        Dev051Entity dev051Info = new Dev051Entity(); //修改实体 dev0162Info
        DevCommonEntity devCommonEntity = new DevCommonEntity();
        BeanUtils.copyProperties(dev051Info, devCommonEntity);
        ResultListsEntity resultListsEntity = new AdsReadUtil().devStructRead(devCommonEntity);
        int size = resultListsEntity.getLineResultEntityList().size();
        if (size > 0) {
            /* 时间轴,获取时间并入库*/
            String[] times = new String[1];
            int id = 9;
            int countDev = processDao.getReadStatus(id);
            DateUtils dateUtils = new DateUtils();
            times[0] = dateUtils.timelineUtil(countDev + 1);
            processDao.updateReadStatus(id);
            List<LineResultEntity> lineList = resultListsEntity.getLineResultEntityList();
            List<NodeResultEntity> nodeList = resultListsEntity.getNodeResultEntityList();
            /* 前后端通讯：对lineList和nodeList做一下拆分 */
            // 处理设备1的线路电流与功率数据
            List<PreLineInfoEntity> preLineInfos = AllGVar.DEV051_LINE_INFO;
            List<PreNodeInfoEntity> preNodeInfos = AllGVar.DEV051_NODE_INFO;
            for (int i = 0; i < dev051Info.getLineDutNum(); i++) {
                // 放入线路的编号，如zh001
                if (i != dev051Info.getLineDutNum() - 1) {
                    for (int j = 0; j < 15; j++) {
                        ListsResEntity listsResEntity = new ListsResEntity();
                        listsResEntity.setNodeNum(preLineInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        lineDataProcessor(lineList, i, j, listsResEntity);
                    }
                } else {
                    // 最后一个表数据不满
                    int end = dev051Info.getLineNum() % 15;
                    for (int j = 0; j < end; j++) {
                        ListsResEntity listsResEntity = new ListsResEntity();
                        listsResEntity.setNodeNum(preLineInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        lineDataProcessor(lineList, i, j, listsResEntity);
                    }
                }
            }
            // 处理设备1的节点电压数据
            for (int i = 0; i < dev051Info.getNodeDutNum(); i++) {
                if (i != dev051Info.getNodeDutNum() - 1) {
                    for (int j = 0; j < 15; j++) {
                        // 返回list放入节点的编号，如zh001
                        ListsResEntity listsResEntity = new ListsResEntity();
                        // 编号位置从设备1的线路编号最后一个的位置开始
                        listsResEntity.setNodeNum(preNodeInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        nodeDataProcessor(nodeList, i, j, listsResEntity);
                    }
                } else {
                    int end = dev051Info.getNodeNum() % 15;
                    for (int j = 0; j < end; j++) {
                        // 返回list放入节点的编号，如zh001
                        ListsResEntity listsResEntity = new ListsResEntity();
                        // 编号位置从设备1的线路编号最后一个的位置开始
                        listsResEntity.setNodeNum(preNodeInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        nodeDataProcessor(nodeList, i, j, listsResEntity);
                    }
                }
            }
            /* 执行数据插入 */
            for (int i = 0; i < dev051Info.getAllDutNum(); i++) {
                if (i < dev051Info.getLineDutNum()) {
                    processDao.insertLineResult(AllGVar.DEV051_Tab_Name.get(i), lineList.get(i));
                } else {
                    processDao.insertNodeResult(AllGVar.DEV051_Tab_Name.get(i), nodeList.get(i - dev051Info.getLineDutNum()));
                }
            }
            /* 线路负载率=单相电流/额定电流 */
            LoadRateUtil loadRateUtil = new LoadRateUtil();
            List<LineResultEntity> lineList1 = new ArrayList<>();
            // 取库中各线路的额定电流
            String lineRatedName = "capacity_line_dev051";
            String nodeRatedName = "capacity_node_dev051";
            Float[] ratedCurrents = processDao.getLineRatedCurrent(lineRatedName);
            for (int i = 0; i < dev051Info.getLineDutNum(); i++) {
                // 从库中取出设备1的最新的一次线路的三相电流、有功无功等数据
                LineResultEntity lineResultEntity = processDao.selectLineData(AllGVar.DEV051_Tab_Name.get(i));
                lineList1.add(lineResultEntity);
            }
            // 执行线路负载率的计算,把节点的编号一起传过去处理
            List<LoadRateEntity> lineLoadRatio = loadRateUtil.lineLoadRate(times,
                    AllGVar.DEV051_LINE_INFO, lineList1, ratedCurrents);
            // 更新dev1中线路负载率
            processDao.updateLineLoadRatio(lineLoadRatio);
            // 执行插入线路负载率历史记录表
            processDao.insertLineLoadRatio(lineLoadRatio);
            /* 变电站负载率=UI/额定容量 */
            // 取读取数据中的UI值
            Float[] uis = resultListsEntity.getNodeUI();
            // 取库中的变电站的额定容量
            Float[] nodeRatedCapacity = processDao.getNodeRatedCapacity(nodeRatedName);
            List<LoadRateEntity> nodeLoadRatio = loadRateUtil.nodeLoadRate(times,
                    preNodeInfos, uis, nodeRatedCapacity);
            // 更新库中的变电站负载率
            processDao.updateNodeLoadRatio(nodeLoadRatio);
            // 执行插入变电站负载率历史记录表
            processDao.insertNodeLoadRatio(nodeLoadRatio);
            /**
             * 查询一次所有的负载率，发送给前端
             */
            ListsResEntity listsResEntity1 = dataReadServiceImpl.loadRateRead();
            webSocketService.sendTextMsg("/response/getLoadRate", listsResEntity1);
            System.out.println("绍兴01读取完毕");
        } else {
            System.out.println("设备数据为空");
        }
    }

    @Override
    // @Async("task2")
    @Transactional
    public void dev052DataProcess() {
        Dev052Entity dev052Info = new Dev052Entity(); //修改实体 dev0162Info
        DevCommonEntity devCommonEntity = new DevCommonEntity();
        BeanUtils.copyProperties(dev052Info, devCommonEntity);
        ResultListsEntity resultListsEntity = new AdsReadUtil().devStructRead(devCommonEntity);
        int size = resultListsEntity.getLineResultEntityList().size();
        if (size > 0) {
            /* 时间轴,获取时间并入库*/
            String[] times = new String[1];
            int id = 10;
            int countDev = processDao.getReadStatus(id);
            DateUtils dateUtils = new DateUtils();
            times[0] = dateUtils.timelineUtil(countDev + 1);
            processDao.updateReadStatus(id);
            List<LineResultEntity> lineList = resultListsEntity.getLineResultEntityList();
            List<NodeResultEntity> nodeList = resultListsEntity.getNodeResultEntityList();
            /* 前后端通讯：对lineList和nodeList做一下拆分 */
            // 处理设备1的线路电流与功率数据
            List<PreLineInfoEntity> preLineInfos = AllGVar.DEV052_LINE_INFO;
            List<PreNodeInfoEntity> preNodeInfos = AllGVar.DEV052_NODE_INFO;
            for (int i = 0; i < dev052Info.getLineDutNum(); i++) {
                // 放入线路的编号，如zh001
                if (i != dev052Info.getLineDutNum() - 1) {
                    for (int j = 0; j < 15; j++) {
                        ListsResEntity listsResEntity = new ListsResEntity();
                        listsResEntity.setNodeNum(preLineInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        lineDataProcessor(lineList, i, j, listsResEntity);
                    }
                } else {
                    // 最后一个表数据不满
                    int end = dev052Info.getLineNum() % 15;
                    for (int j = 0; j < end; j++) {
                        ListsResEntity listsResEntity = new ListsResEntity();
                        listsResEntity.setNodeNum(preLineInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        lineDataProcessor(lineList, i, j, listsResEntity);
                    }
                }
            }
            // 处理设备1的节点电压数据
            for (int i = 0; i < dev052Info.getNodeDutNum(); i++) {
                if (i != dev052Info.getNodeDutNum() - 1) {
                    for (int j = 0; j < 15; j++) {
                        // 返回list放入节点的编号，如zh001
                        ListsResEntity listsResEntity = new ListsResEntity();
                        // 编号位置从设备1的线路编号最后一个的位置开始
                        listsResEntity.setNodeNum(preNodeInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        nodeDataProcessor(nodeList, i, j, listsResEntity);
                    }
                } else {
                    int end = dev052Info.getNodeNum() % 15;
                    for (int j = 0; j < end; j++) {
                        // 返回list放入节点的编号，如zh001
                        ListsResEntity listsResEntity = new ListsResEntity();
                        // 编号位置从设备1的线路编号最后一个的位置开始
                        listsResEntity.setNodeNum(preNodeInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        nodeDataProcessor(nodeList, i, j, listsResEntity);
                    }
                }
            }
            /* 执行数据插入 */
            for (int i = 0; i < dev052Info.getAllDutNum(); i++) {
                if (i < dev052Info.getLineDutNum()) {
                    processDao.insertLineResult(AllGVar.DEV052_Tab_Name.get(i), lineList.get(i));
                } else {
                    processDao.insertNodeResult(AllGVar.DEV052_Tab_Name.get(i), nodeList.get(i - dev052Info.getLineDutNum()));
                }
            }
            /* 线路负载率=单相电流/额定电流 */
            LoadRateUtil loadRateUtil = new LoadRateUtil();
            List<LineResultEntity> lineList1 = new ArrayList<>();
            // 取库中各线路的额定电流
            String lineRatedName = "capacity_line_dev052";
            String nodeRatedName = "capacity_node_dev052";
            Float[] ratedCurrents = processDao.getLineRatedCurrent(lineRatedName);
            for (int i = 0; i < dev052Info.getLineDutNum(); i++) {
                // 从库中取出设备1的最新的一次线路的三相电流、有功无功等数据
                LineResultEntity lineResultEntity = processDao.selectLineData(AllGVar.DEV052_Tab_Name.get(i));
                lineList1.add(lineResultEntity);
            }
            // 执行线路负载率的计算,把节点的编号一起传过去处理
            List<LoadRateEntity> lineLoadRatio = loadRateUtil.lineLoadRate(times,
                    AllGVar.DEV052_LINE_INFO, lineList1, ratedCurrents);
            // 更新dev1中线路负载率
            processDao.updateLineLoadRatio(lineLoadRatio);
            // 执行插入线路负载率历史记录表
            processDao.insertLineLoadRatio(lineLoadRatio);
            /* 变电站负载率=UI/额定容量 */
            // 取读取数据中的UI值
            Float[] uis = resultListsEntity.getNodeUI();
            // 取库中的变电站的额定容量
            Float[] nodeRatedCapacity = processDao.getNodeRatedCapacity(nodeRatedName);
            List<LoadRateEntity> nodeLoadRatio = loadRateUtil.nodeLoadRate(times,
                    preNodeInfos, uis, nodeRatedCapacity);
            // 更新库中的变电站负载率
            processDao.updateNodeLoadRatio(nodeLoadRatio);
            // 执行插入变电站负载率历史记录表
            processDao.insertNodeLoadRatio(nodeLoadRatio);
            System.out.println("绍兴02读取完毕");
        } else {
            System.out.println("设备数据为空");
        }
    }

    @Override
    // @Async("task1")
    @Transactional
    public void dev061DataProcess() {
        Dev061Entity dev061Info = new Dev061Entity(); //修改实体 dev0162Info
        DevCommonEntity devCommonEntity = new DevCommonEntity();
        BeanUtils.copyProperties(dev061Info, devCommonEntity);
        ResultListsEntity resultListsEntity = new AdsReadUtil().devStructRead(devCommonEntity);
        int size = resultListsEntity.getLineResultEntityList().size();
        if (size > 0) {
            /* 时间轴,获取时间并入库*/
            String[] times = new String[1];
            int id = 11;
            int countDev = processDao.getReadStatus(id);
            DateUtils dateUtils = new DateUtils();
            times[0] = dateUtils.timelineUtil(countDev + 1);
            processDao.updateReadStatus(id);
            List<LineResultEntity> lineList = resultListsEntity.getLineResultEntityList();
            List<NodeResultEntity> nodeList = resultListsEntity.getNodeResultEntityList();
            /* 前后端通讯：对lineList和nodeList做一下拆分 */
            // 处理设备1的线路电流与功率数据
            List<PreLineInfoEntity> preLineInfos = AllGVar.DEV061_LINE_INFO;
            List<PreNodeInfoEntity> preNodeInfos = AllGVar.DEV061_NODE_INFO;
            for (int i = 0; i < dev061Info.getLineDutNum(); i++) {
                // 放入线路的编号，如zh001
                if (i != dev061Info.getLineDutNum() - 1) {
                    for (int j = 0; j < 15; j++) {
                        ListsResEntity listsResEntity = new ListsResEntity();
                        listsResEntity.setNodeNum(preLineInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        lineDataProcessor(lineList, i, j, listsResEntity);
                    }
                } else {
                    // 最后一个表数据不满
                    int end = dev061Info.getLineNum() % 15;
                    for (int j = 0; j < end; j++) {
                        ListsResEntity listsResEntity = new ListsResEntity();
                        listsResEntity.setNodeNum(preLineInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        lineDataProcessor(lineList, i, j, listsResEntity);
                    }
                }
            }
            // 处理设备1的节点电压数据
            for (int i = 0; i < dev061Info.getNodeDutNum(); i++) {
                if (i != dev061Info.getNodeDutNum() - 1) {
                    for (int j = 0; j < 15; j++) {
                        // 返回list放入节点的编号，如zh001
                        ListsResEntity listsResEntity = new ListsResEntity();
                        // 编号位置从设备1的线路编号最后一个的位置开始
                        listsResEntity.setNodeNum(preNodeInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        nodeDataProcessor(nodeList, i, j, listsResEntity);
                    }
                } else {
                    int end = dev061Info.getNodeNum() % 15;
                    for (int j = 0; j < end; j++) {
                        // 返回list放入节点的编号，如zh001
                        ListsResEntity listsResEntity = new ListsResEntity();
                        // 编号位置从设备1的线路编号最后一个的位置开始
                        listsResEntity.setNodeNum(preNodeInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        nodeDataProcessor(nodeList, i, j, listsResEntity);
                    }
                }
            }
            /* 执行数据插入 */
            for (int i = 0; i < dev061Info.getAllDutNum(); i++) {
                if (i < dev061Info.getLineDutNum()) {
                    processDao.insertLineResult(AllGVar.DEV061_Tab_Name.get(i), lineList.get(i));
                } else {
                    processDao.insertNodeResult(AllGVar.DEV061_Tab_Name.get(i), nodeList.get(i - dev061Info.getLineDutNum()));
                }
            }
            /* 线路负载率=单相电流/额定电流 */
            LoadRateUtil loadRateUtil = new LoadRateUtil();
            List<LineResultEntity> lineList1 = new ArrayList<>();
            // 取库中各线路的额定电流
            String lineRatedName = "capacity_line_dev061";
            String nodeRatedName = "capacity_node_dev061";
            Float[] ratedCurrents = processDao.getLineRatedCurrent(lineRatedName);
            for (int i = 0; i < dev061Info.getLineDutNum(); i++) {
                // 从库中取出设备1的最新的一次线路的三相电流、有功无功等数据
                LineResultEntity lineResultEntity = processDao.selectLineData(AllGVar.DEV061_Tab_Name.get(i));
                lineList1.add(lineResultEntity);
            }
            // 执行线路负载率的计算,把节点的编号一起传过去处理
            List<LoadRateEntity> lineLoadRatio = loadRateUtil.lineLoadRate(times,
                    AllGVar.DEV061_LINE_INFO, lineList1, ratedCurrents);
            // 更新dev1中线路负载率
            processDao.updateLineLoadRatio(lineLoadRatio);
            // 执行插入线路负载率历史记录表
            processDao.insertLineLoadRatio(lineLoadRatio);
            /* 变电站负载率=UI/额定容量 */
            // 取读取数据中的UI值
            Float[] uis = resultListsEntity.getNodeUI();
            // 取库中的变电站的额定容量
            Float[] nodeRatedCapacity = processDao.getNodeRatedCapacity(nodeRatedName);
            List<LoadRateEntity> nodeLoadRatio = loadRateUtil.nodeLoadRate(times,
                    preNodeInfos, uis, nodeRatedCapacity);
            // 更新库中的变电站负载率
            processDao.updateNodeLoadRatio(nodeLoadRatio);
            // 执行插入变电站负载率历史记录表
            processDao.insertNodeLoadRatio(nodeLoadRatio);
            /**
             * 查询一次所有的负载率，发送给前端
             */
            ListsResEntity listsResEntity1 = dataReadServiceImpl.loadRateRead();
            webSocketService.sendTextMsg("/response/getLoadRate", listsResEntity1);
            System.out.println("台州01读取结束");
        } else {
            System.out.println("设备数据为空");
        }
    }

    @Override
    // @Async("task2")
    @Transactional
    public void dev062DataProcess() {
        Dev062Entity dev062Info = new Dev062Entity(); //修改实体 dev0162Info
        DevCommonEntity devCommonEntity = new DevCommonEntity();
        BeanUtils.copyProperties(dev062Info, devCommonEntity);
        ResultListsEntity resultListsEntity = new AdsReadUtil().devStructRead(devCommonEntity);
        int size = resultListsEntity.getLineResultEntityList().size();
        if (size > 0) {
            /* 时间轴,获取时间并入库*/
            String[] times = new String[1];
            int id = 12;
            int countDev = processDao.getReadStatus(id);
            DateUtils dateUtils = new DateUtils();
            times[0] = dateUtils.timelineUtil(countDev + 1);
            processDao.updateReadStatus(id);
            List<LineResultEntity> lineList = resultListsEntity.getLineResultEntityList();
            List<NodeResultEntity> nodeList = resultListsEntity.getNodeResultEntityList();
            /* 前后端通讯：对lineList和nodeList做一下拆分 */
            // 处理设备1的线路电流与功率数据
            List<PreLineInfoEntity> preLineInfos = AllGVar.DEV062_LINE_INFO;
            List<PreNodeInfoEntity> preNodeInfos = AllGVar.DEV062_NODE_INFO;
            for (int i = 0; i < dev062Info.getLineDutNum(); i++) {
                // 放入线路的编号，如zh001
                if (i != dev062Info.getLineDutNum() - 1) {
                    for (int j = 0; j < 15; j++) {
                        ListsResEntity listsResEntity = new ListsResEntity();
                        listsResEntity.setNodeNum(preLineInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        lineDataProcessor(lineList, i, j, listsResEntity);
                    }
                } else {
                    // 最后一个表数据不满
                    int end = dev062Info.getLineNum() % 15;
                    for (int j = 0; j < end; j++) {
                        ListsResEntity listsResEntity = new ListsResEntity();
                        listsResEntity.setNodeNum(preLineInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        lineDataProcessor(lineList, i, j, listsResEntity);
                    }
                }
            }
            // 处理设备1的节点电压数据
            for (int i = 0; i < dev062Info.getNodeDutNum(); i++) {
                if (i != dev062Info.getNodeDutNum() - 1) {
                    for (int j = 0; j < 15; j++) {
                        // 返回list放入节点的编号，如zh001
                        ListsResEntity listsResEntity = new ListsResEntity();
                        // 编号位置从设备1的线路编号最后一个的位置开始
                        listsResEntity.setNodeNum(preNodeInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        nodeDataProcessor(nodeList, i, j, listsResEntity);
                    }
                } else {
                    int end = dev062Info.getNodeNum() % 15;
                    for (int j = 0; j < end; j++) {
                        // 返回list放入节点的编号，如zh001
                        ListsResEntity listsResEntity = new ListsResEntity();
                        // 编号位置从设备1的线路编号最后一个的位置开始
                        listsResEntity.setNodeNum(preNodeInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        nodeDataProcessor(nodeList, i, j, listsResEntity);
                    }
                }
            }
            /* 执行数据插入 */
            for (int i = 0; i < dev062Info.getAllDutNum(); i++) {
                if (i < dev062Info.getLineDutNum()) {
                    processDao.insertLineResult(AllGVar.DEV062_Tab_Name.get(i), lineList.get(i));
                } else {
                    processDao.insertNodeResult(AllGVar.DEV062_Tab_Name.get(i), nodeList.get(i - dev062Info.getLineDutNum()));
                }
            }
            /* 线路负载率=单相电流/额定电流 */
            LoadRateUtil loadRateUtil = new LoadRateUtil();
            List<LineResultEntity> lineList1 = new ArrayList<>();
            // 取库中各线路的额定电流
            String lineRatedName = "capacity_line_dev062";
            String nodeRatedName = "capacity_node_dev062";
            Float[] ratedCurrents = processDao.getLineRatedCurrent(lineRatedName);
            for (int i = 0; i < dev062Info.getLineDutNum(); i++) {
                // 从库中取出设备1的最新的一次线路的三相电流、有功无功等数据
                LineResultEntity lineResultEntity = processDao.selectLineData(AllGVar.DEV062_Tab_Name.get(i));
                lineList1.add(lineResultEntity);
            }
            // 执行线路负载率的计算,把节点的编号一起传过去处理
            List<LoadRateEntity> lineLoadRatio = loadRateUtil.lineLoadRate(times,
                    AllGVar.DEV062_LINE_INFO, lineList1, ratedCurrents);
            // 更新dev1中线路负载率
            processDao.updateLineLoadRatio(lineLoadRatio);
            // 执行插入线路负载率历史记录表
            processDao.insertLineLoadRatio(lineLoadRatio);
            /* 变电站负载率=UI/额定容量 */
            // 取读取数据中的UI值
            Float[] uis = resultListsEntity.getNodeUI();
            // 取库中的变电站的额定容量
            Float[] nodeRatedCapacity = processDao.getNodeRatedCapacity(nodeRatedName);
            List<LoadRateEntity> nodeLoadRatio = loadRateUtil.nodeLoadRate(times,
                    preNodeInfos, uis, nodeRatedCapacity);
            // 更新库中的变电站负载率
            processDao.updateNodeLoadRatio(nodeLoadRatio);
            // 执行插入变电站负载率历史记录表
            processDao.insertNodeLoadRatio(nodeLoadRatio);
            System.out.println("台州02读取结束");
        } else {
            System.out.println("设备数据为空");
        }
    }

    @Override
    // @Async("task1")
    @Transactional
    public void dev071DataProcess() {
        Dev071Entity dev071Info = new Dev071Entity();
        DevCommonEntity devCommonEntity = new DevCommonEntity();
        BeanUtils.copyProperties(dev071Info, devCommonEntity);
        ResultListsEntity resultListsEntity = new AdsReadUtil().devStructRead(devCommonEntity);
        int size = resultListsEntity.getLineResultEntityList().size();
        if (size > 0) {
            /* 时间轴,获取时间并入库*/
            String[] times = new String[1];
            int id = 13;
            int countDev = processDao.getReadStatus(id);
            DateUtils dateUtils = new DateUtils();
            times[0] = dateUtils.timelineUtil(countDev + 1);
            processDao.updateReadStatus(id);
            List<LineResultEntity> lineList = resultListsEntity.getLineResultEntityList();
            List<NodeResultEntity> nodeList = resultListsEntity.getNodeResultEntityList();
            /* 前后端通讯：对lineList和nodeList做一下拆分 */
            // 处理设备1的线路电流与功率数据
            List<PreLineInfoEntity> preLineInfos = AllGVar.DEV071_LINE_INFO;
            List<PreNodeInfoEntity> preNodeInfos = AllGVar.DEV071_NODE_INFO;
            for (int i = 0; i < dev071Info.getLineDutNum(); i++) {
                // 放入线路的编号，如zh001
                if (i != dev071Info.getLineDutNum() - 1) {
                    for (int j = 0; j < 15; j++) {
                        ListsResEntity listsResEntity = new ListsResEntity();
                        listsResEntity.setNodeNum(preLineInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        lineDataProcessor(lineList, i, j, listsResEntity);
                    }
                } else {
                    // 最后一个表数据不满
                    int end = dev071Info.getLineNum() % 15;
                    for (int j = 0; j < end; j++) {
                        ListsResEntity listsResEntity = new ListsResEntity();
                        listsResEntity.setNodeNum(preLineInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        lineDataProcessor(lineList, i, j, listsResEntity);
                    }
                }
            }
            // 处理设备1的节点电压数据
            for (int i = 0; i < dev071Info.getNodeDutNum(); i++) {
                if (i != dev071Info.getNodeDutNum() - 1) {
                    for (int j = 0; j < 15; j++) {
                        // 返回list放入节点的编号，如zh001
                        ListsResEntity listsResEntity = new ListsResEntity();
                        // 编号位置从设备1的线路编号最后一个的位置开始
                        listsResEntity.setNodeNum(preNodeInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        nodeDataProcessor(nodeList, i, j, listsResEntity);
                    }
                } else {
                    int end = dev071Info.getNodeNum() % 15;
                    for (int j = 0; j < end; j++) {
                        // 返回list放入节点的编号，如zh001
                        ListsResEntity listsResEntity = new ListsResEntity();
                        // 编号位置从设备1的线路编号最后一个的位置开始
                        listsResEntity.setNodeNum(preNodeInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        nodeDataProcessor(nodeList, i, j, listsResEntity);
                    }
                }
            }
            /* 执行数据插入 */
            for (int i = 0; i < dev071Info.getAllDutNum(); i++) {
                if (i < dev071Info.getLineDutNum()) {
                    processDao.insertLineResult(AllGVar.DEV071_Tab_Name.get(i), lineList.get(i));
                } else {
                    processDao.insertNodeResult(AllGVar.DEV071_Tab_Name.get(i), nodeList.get(i - dev071Info.getLineDutNum()));
                }
            }
            /* 线路负载率=单相电流/额定电流 */
            LoadRateUtil loadRateUtil = new LoadRateUtil();
            List<LineResultEntity> lineList1 = new ArrayList<>();
            // 取库中各线路的额定电流
            String lineRatedName = "capacity_line_dev071";
            String nodeRatedName = "capacity_node_dev071";
            Float[] ratedCurrents = processDao.getLineRatedCurrent(lineRatedName);
            for (int i = 0; i < dev071Info.getLineDutNum(); i++) {
                // 从库中取出设备1的最新的一次线路的三相电流、有功无功等数据
                LineResultEntity lineResultEntity = processDao.selectLineData(AllGVar.DEV071_Tab_Name.get(i));
                lineList1.add(lineResultEntity);
            }
            // 执行线路负载率的计算,把节点的编号一起传过去处理
            List<LoadRateEntity> lineLoadRatio = loadRateUtil.lineLoadRate(times,
                    AllGVar.DEV071_LINE_INFO, lineList1, ratedCurrents);
            // 更新dev1中线路负载率
            processDao.updateLineLoadRatio(lineLoadRatio);
            // 执行插入线路负载率历史记录表
            processDao.insertLineLoadRatio(lineLoadRatio);
            /* 变电站负载率=UI/额定容量 */
            // 取读取数据中的UI值
            Float[] uis = resultListsEntity.getNodeUI();
            // 取库中的变电站的额定容量
            Float[] nodeRatedCapacity = processDao.getNodeRatedCapacity(nodeRatedName);
            List<LoadRateEntity> nodeLoadRatio = loadRateUtil.nodeLoadRate(times,
                    preNodeInfos, uis, nodeRatedCapacity);
            // 更新库中的变电站负载率
            processDao.updateNodeLoadRatio(nodeLoadRatio);
            // 执行插入变电站负载率历史记录表
            processDao.insertNodeLoadRatio(nodeLoadRatio);
            /**
             * 查询一次所有的负载率，发送给前端
             */
            ListsResEntity listsResEntity1 = dataReadServiceImpl.loadRateRead();
            webSocketService.sendTextMsg("/response/getLoadRate", listsResEntity1);
            System.out.println("宁波01读取完毕");
        } else {
            System.out.println("设备数据为空");
        }
    }

    @Override
    // @Async("task2")
    @Transactional
    public void dev072DataProcess() {
        Dev072Entity dev072Info = new Dev072Entity();
        DevCommonEntity devCommonEntity = new DevCommonEntity();
        BeanUtils.copyProperties(dev072Info, devCommonEntity);
        ResultListsEntity resultListsEntity = new AdsReadUtil().devStructRead(devCommonEntity);
        int size = resultListsEntity.getLineResultEntityList().size();
        if (size > 0) {
            /* 时间轴,获取时间并入库*/
            String[] times = new String[1];
            int id = 14;
            int countDev = processDao.getReadStatus(id);
            DateUtils dateUtils = new DateUtils();
            times[0] = dateUtils.timelineUtil(countDev + 1);
            processDao.updateReadStatus(id);
            List<LineResultEntity> lineList = resultListsEntity.getLineResultEntityList();
            List<NodeResultEntity> nodeList = resultListsEntity.getNodeResultEntityList();
            /* 前后端通讯：对lineList和nodeList做一下拆分 */
            // 处理设备1的线路电流与功率数据
            List<PreLineInfoEntity> preLineInfos = AllGVar.DEV072_LINE_INFO;
            List<PreNodeInfoEntity> preNodeInfos = AllGVar.DEV072_NODE_INFO;
            for (int i = 0; i < dev072Info.getLineDutNum(); i++) {
                // 放入线路的编号，如zh001
                if (i != dev072Info.getLineDutNum() - 1) {
                    for (int j = 0; j < 15; j++) {
                        ListsResEntity listsResEntity = new ListsResEntity();
                        listsResEntity.setNodeNum(preLineInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        lineDataProcessor(lineList, i, j, listsResEntity);
                    }
                } else {
                    // 最后一个表数据不满
                    int end = dev072Info.getLineNum() % 15;
                    for (int j = 0; j < end; j++) {
                        ListsResEntity listsResEntity = new ListsResEntity();
                        listsResEntity.setNodeNum(preLineInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        lineDataProcessor(lineList, i, j, listsResEntity);
                    }
                }
            }
            // 处理设备1的节点电压数据
            for (int i = 0; i < dev072Info.getNodeDutNum(); i++) {
                if (i != dev072Info.getNodeDutNum() - 1) {
                    for (int j = 0; j < 15; j++) {
                        // 返回list放入节点的编号，如zh001
                        ListsResEntity listsResEntity = new ListsResEntity();
                        // 编号位置从设备1的线路编号最后一个的位置开始
                        listsResEntity.setNodeNum(preNodeInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        nodeDataProcessor(nodeList, i, j, listsResEntity);
                    }
                } else {
                    int end = dev072Info.getNodeNum() % 15;
                    for (int j = 0; j < end; j++) {
                        // 返回list放入节点的编号，如zh001
                        ListsResEntity listsResEntity = new ListsResEntity();
                        // 编号位置从设备1的线路编号最后一个的位置开始
                        listsResEntity.setNodeNum(preNodeInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        nodeDataProcessor(nodeList, i, j, listsResEntity);
                    }
                }
            }
            /* 执行数据插入 */
            for (int i = 0; i < dev072Info.getAllDutNum(); i++) {
                if (i < dev072Info.getLineDutNum()) {
                    processDao.insertLineResult(AllGVar.DEV072_Tab_Name.get(i), lineList.get(i));
                } else {
                    processDao.insertNodeResult(AllGVar.DEV072_Tab_Name.get(i), nodeList.get(i - dev072Info.getLineDutNum()));
                }
            }
            /* 线路负载率=单相电流/额定电流 */
            LoadRateUtil loadRateUtil = new LoadRateUtil();
            List<LineResultEntity> lineList1 = new ArrayList<>();
            // 取库中各线路的额定电流
            String lineRatedName = "capacity_line_dev072";
            String nodeRatedName = "capacity_node_dev072";
            Float[] ratedCurrents = processDao.getLineRatedCurrent(lineRatedName);
            for (int i = 0; i < dev072Info.getLineDutNum(); i++) {
                // 从库中取出设备1的最新的一次线路的三相电流、有功无功等数据
                LineResultEntity lineResultEntity = processDao.selectLineData(AllGVar.DEV072_Tab_Name.get(i));
                lineList1.add(lineResultEntity);
            }
            // 执行线路负载率的计算,把节点的编号一起传过去处理
            List<LoadRateEntity> lineLoadRatio = loadRateUtil.lineLoadRate(times,
                    AllGVar.DEV072_LINE_INFO, lineList1, ratedCurrents);
            // 更新dev1中线路负载率
            processDao.updateLineLoadRatio(lineLoadRatio);
            // 执行插入线路负载率历史记录表
            processDao.insertLineLoadRatio(lineLoadRatio);
            /* 变电站负载率=UI/额定容量 */
            // 取读取数据中的UI值
            Float[] uis = resultListsEntity.getNodeUI();
            // 取库中的变电站的额定容量
            Float[] nodeRatedCapacity = processDao.getNodeRatedCapacity(nodeRatedName);
            List<LoadRateEntity> nodeLoadRatio = loadRateUtil.nodeLoadRate(times,
                    preNodeInfos, uis, nodeRatedCapacity);
            // 更新库中的变电站负载率
            processDao.updateNodeLoadRatio(nodeLoadRatio);
            // 执行插入变电站负载率历史记录表
            processDao.insertNodeLoadRatio(nodeLoadRatio);
            System.out.println("宁波02读取完毕");
        } else {
            System.out.println("设备数据为空");
        }
    }

    @Override
    // @Async("task1")
    @Transactional
    public void dev081DataProcess() {
        Dev081Entity dev081Info = new Dev081Entity();
        DevCommonEntity devCommonEntity = new DevCommonEntity();
        BeanUtils.copyProperties(dev081Info, devCommonEntity);
        ResultListsEntity resultListsEntity = new AdsReadUtil().devStructRead(devCommonEntity);
        int size = resultListsEntity.getLineResultEntityList().size();
        if (size > 0) {
            /* 时间轴,获取时间并入库*/
            String[] times = new String[1];
            int id = 15;
            int countDev = processDao.getReadStatus(id);
            DateUtils dateUtils = new DateUtils();
            times[0] = dateUtils.timelineUtil(countDev + 1);
            processDao.updateReadStatus(id);
            List<LineResultEntity> lineList = resultListsEntity.getLineResultEntityList();
            List<NodeResultEntity> nodeList = resultListsEntity.getNodeResultEntityList();
            /* 前后端通讯：对lineList和nodeList做一下拆分 */
            // 处理设备1的线路电流与功率数据
            List<PreLineInfoEntity> preLineInfos = AllGVar.DEV081_LINE_INFO;
            List<PreNodeInfoEntity> preNodeInfos = AllGVar.DEV081_NODE_INFO;
            for (int i = 0; i < dev081Info.getLineDutNum(); i++) {
                // 放入线路的编号，如zh001
                if (i != dev081Info.getLineDutNum() - 1) {
                    for (int j = 0; j < 15; j++) {
                        ListsResEntity listsResEntity = new ListsResEntity();
                        listsResEntity.setNodeNum(preLineInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        lineDataProcessor(lineList, i, j, listsResEntity);
                    }
                } else {
                    // 最后一个表数据不满
                    int end = dev081Info.getLineNum() % 15;
                    for (int j = 0; j < end; j++) {
                        ListsResEntity listsResEntity = new ListsResEntity();
                        listsResEntity.setNodeNum(preLineInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        lineDataProcessor(lineList, i, j, listsResEntity);
                    }
                }
            }
            // 处理设备1的节点电压数据
            for (int i = 0; i < dev081Info.getNodeDutNum(); i++) {
                if (i != dev081Info.getNodeDutNum() - 1) {
                    for (int j = 0; j < 15; j++) {
                        // 返回list放入节点的编号，如zh001
                        ListsResEntity listsResEntity = new ListsResEntity();
                        // 编号位置从设备1的线路编号最后一个的位置开始
                        listsResEntity.setNodeNum(preNodeInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        nodeDataProcessor(nodeList, i, j, listsResEntity);
                    }
                } else {
                    int end = dev081Info.getNodeNum() % 15;
                    for (int j = 0; j < end; j++) {
                        // 返回list放入节点的编号，如zh001
                        ListsResEntity listsResEntity = new ListsResEntity();
                        // 编号位置从设备1的线路编号最后一个的位置开始
                        listsResEntity.setNodeNum(preNodeInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        nodeDataProcessor(nodeList, i, j, listsResEntity);
                    }
                }
            }
            /* 执行数据插入 */
            for (int i = 0; i < dev081Info.getAllDutNum(); i++) {
                if (i < dev081Info.getLineDutNum()) {
                    processDao.insertLineResult(AllGVar.DEV081_Tab_Name.get(i), lineList.get(i));
                } else {
                    processDao.insertNodeResult(AllGVar.DEV081_Tab_Name.get(i), nodeList.get(i - dev081Info.getLineDutNum()));
                }
            }
            /* 线路负载率=单相电流/额定电流 */
            LoadRateUtil loadRateUtil = new LoadRateUtil();
            List<LineResultEntity> lineList1 = new ArrayList<>();
            // 取库中各线路的额定电流
            String lineRatedName = "capacity_line_dev081";
            String nodeRatedName = "capacity_node_dev081";
            Float[] ratedCurrents = processDao.getLineRatedCurrent(lineRatedName);
            for (int i = 0; i < dev081Info.getLineDutNum(); i++) {
                // 从库中取出设备1的最新的一次线路的三相电流、有功无功等数据
                LineResultEntity lineResultEntity = processDao.selectLineData(AllGVar.DEV081_Tab_Name.get(i));
                lineList1.add(lineResultEntity);
            }
            // 执行线路负载率的计算,把节点的编号一起传过去处理
            List<LoadRateEntity> lineLoadRatio = loadRateUtil.lineLoadRate(times,
                    AllGVar.DEV081_LINE_INFO, lineList1, ratedCurrents);
            // 更新dev1中线路负载率
            processDao.updateLineLoadRatio(lineLoadRatio);
            // 执行插入线路负载率历史记录表
            processDao.insertLineLoadRatio(lineLoadRatio);
            /* 变电站负载率=UI/额定容量 */
            // 取读取数据中的UI值
            Float[] uis = resultListsEntity.getNodeUI();
            // 取库中的变电站的额定容量
            Float[] nodeRatedCapacity = processDao.getNodeRatedCapacity(nodeRatedName);
            List<LoadRateEntity> nodeLoadRatio = loadRateUtil.nodeLoadRate(times,
                    preNodeInfos, uis, nodeRatedCapacity);
            // 更新库中的变电站负载率
            processDao.updateNodeLoadRatio(nodeLoadRatio);
            // 执行插入变电站负载率历史记录表
            processDao.insertNodeLoadRatio(nodeLoadRatio);
            System.out.println("宁波03读取完毕");
        } else {
            System.out.println("设备数据为空");
        }
    }

    @Override
    // @Async("task2")
    @Transactional
    public void dev082DataProcess() {
        Dev082Entity dev082Info = new Dev082Entity();
        DevCommonEntity devCommonEntity = new DevCommonEntity();
        BeanUtils.copyProperties(dev082Info, devCommonEntity);
        ResultListsEntity resultListsEntity = new AdsReadUtil().devStructRead(devCommonEntity);
        int size = resultListsEntity.getLineResultEntityList().size();
        if (size > 0) {
            /* 时间轴,获取时间并入库*/
            String[] times = new String[1];
            int id = 16;
            int countDev = processDao.getReadStatus(id);
            DateUtils dateUtils = new DateUtils();
            times[0] = dateUtils.timelineUtil(countDev + 1);
            processDao.updateReadStatus(id);
            List<LineResultEntity> lineList = resultListsEntity.getLineResultEntityList();
            List<NodeResultEntity> nodeList = resultListsEntity.getNodeResultEntityList();
            /* 前后端通讯：对lineList和nodeList做一下拆分 */
            // 处理设备1的线路电流与功率数据
            List<PreLineInfoEntity> preLineInfos = AllGVar.DEV082_LINE_INFO;
            List<PreNodeInfoEntity> preNodeInfos = AllGVar.DEV082_NODE_INFO;
            for (int i = 0; i < dev082Info.getLineDutNum(); i++) {
                // 放入线路的编号，如zh001
                if (i != dev082Info.getLineDutNum() - 1) {
                    for (int j = 0; j < 15; j++) {
                        ListsResEntity listsResEntity = new ListsResEntity();
                        listsResEntity.setNodeNum(preLineInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        lineDataProcessor(lineList, i, j, listsResEntity);
                    }
                } else {
                    // 最后一个表数据不满
                    int end = dev082Info.getLineNum() % 15;
                    for (int j = 0; j < end; j++) {
                        ListsResEntity listsResEntity = new ListsResEntity();
                        listsResEntity.setNodeNum(preLineInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        lineDataProcessor(lineList, i, j, listsResEntity);
                    }
                }
            }
            // 处理设备1的节点电压数据
            for (int i = 0; i < dev082Info.getNodeDutNum(); i++) {
                if (i != dev082Info.getNodeDutNum() - 1) {
                    for (int j = 0; j < 15; j++) {
                        // 返回list放入节点的编号，如zh001
                        ListsResEntity listsResEntity = new ListsResEntity();
                        // 编号位置从设备1的线路编号最后一个的位置开始
                        listsResEntity.setNodeNum(preNodeInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        nodeDataProcessor(nodeList, i, j, listsResEntity);
                    }
                } else {
                    int end = dev082Info.getNodeNum() % 15;
                    for (int j = 0; j < end; j++) {
                        // 返回list放入节点的编号，如zh001
                        ListsResEntity listsResEntity = new ListsResEntity();
                        // 编号位置从设备1的线路编号最后一个的位置开始
                        listsResEntity.setNodeNum(preNodeInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        nodeDataProcessor(nodeList, i, j, listsResEntity);
                    }
                }
            }
            /* 执行数据插入 */
            for (int i = 0; i < dev082Info.getAllDutNum(); i++) {
                if (i < dev082Info.getLineDutNum()) {
                    processDao.insertLineResult(AllGVar.DEV082_Tab_Name.get(i), lineList.get(i));
                } else {
                    processDao.insertNodeResult(AllGVar.DEV082_Tab_Name.get(i), nodeList.get(i - dev082Info.getLineDutNum()));
                }
            }
            /* 线路负载率=单相电流/额定电流 */
            LoadRateUtil loadRateUtil = new LoadRateUtil();
            List<LineResultEntity> lineList1 = new ArrayList<>();
            // 取库中各线路的额定电流
            String lineRatedName = "capacity_line_dev082";
            String nodeRatedName = "capacity_node_dev082";
            Float[] ratedCurrents = processDao.getLineRatedCurrent(lineRatedName);
            for (int i = 0; i < dev082Info.getLineDutNum(); i++) {
                // 从库中取出设备1的最新的一次线路的三相电流、有功无功等数据
                LineResultEntity lineResultEntity = processDao.selectLineData(AllGVar.DEV082_Tab_Name.get(i));
                lineList1.add(lineResultEntity);
            }
            // 执行线路负载率的计算,把节点的编号一起传过去处理
            List<LoadRateEntity> lineLoadRatio = loadRateUtil.lineLoadRate(times,
                    AllGVar.DEV082_LINE_INFO, lineList1, ratedCurrents);
            // 更新dev1中线路负载率
            processDao.updateLineLoadRatio(lineLoadRatio);
            // 执行插入线路负载率历史记录表
            processDao.insertLineLoadRatio(lineLoadRatio);
            /* 变电站负载率=UI/额定容量 */
            // 取读取数据中的UI值
            Float[] uis = resultListsEntity.getNodeUI();
            // 取库中的变电站的额定容量
            Float[] nodeRatedCapacity = processDao.getNodeRatedCapacity(nodeRatedName);
            List<LoadRateEntity> nodeLoadRatio = loadRateUtil.nodeLoadRate(times,
                    preNodeInfos, uis, nodeRatedCapacity);
            // 更新库中的变电站负载率
            processDao.updateNodeLoadRatio(nodeLoadRatio);
            // 执行插入变电站负载率历史记录表
            processDao.insertNodeLoadRatio(nodeLoadRatio);
            /**
             * 查询一次所有的负载率，发送给前端
             */
            ListsResEntity listsResEntity1 = dataReadServiceImpl.loadRateRead();
            webSocketService.sendTextMsg("/response/getLoadRate", listsResEntity1);
            System.out.println("舟山01读取完毕");
        } else {
            System.out.println("设备数据为空");
        }
    }

    @Override
    // @Async("task1")
    @Transactional
    public void dev091DataProcess() {
        Dev091Entity dev091Info = new Dev091Entity();
        DevCommonEntity devCommonEntity = new DevCommonEntity();
        BeanUtils.copyProperties(dev091Info, devCommonEntity);
        ResultListsEntity resultListsEntity = new AdsReadUtil().devStructRead(devCommonEntity);
        int size = resultListsEntity.getLineResultEntityList().size();
        if (size > 0) {
            /* 时间轴,获取时间并入库*/
            String[] times = new String[1];
            int id = 17;
            int countDev = processDao.getReadStatus(id);
            DateUtils dateUtils = new DateUtils();
            times[0] = dateUtils.timelineUtil(countDev + 1);
            processDao.updateReadStatus(id);
            List<LineResultEntity> lineList = resultListsEntity.getLineResultEntityList();
            List<NodeResultEntity> nodeList = resultListsEntity.getNodeResultEntityList();
            /* 前后端通讯：对lineList和nodeList做一下拆分 */
            // 处理设备1的线路电流与功率数据
            List<PreLineInfoEntity> preLineInfos = AllGVar.DEV091_LINE_INFO;
            List<PreNodeInfoEntity> preNodeInfos = AllGVar.DEV091_NODE_INFO;
            for (int i = 0; i < dev091Info.getLineDutNum(); i++) {
                // 放入线路的编号，如zh001
                if (i != dev091Info.getLineDutNum() - 1) {
                    for (int j = 0; j < 15; j++) {
                        ListsResEntity listsResEntity = new ListsResEntity();
                        listsResEntity.setNodeNum(preLineInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        lineDataProcessor(lineList, i, j, listsResEntity);
                    }
                } else {
                    // 最后一个表数据不满
                    int end = dev091Info.getLineNum() % 15;
                    for (int j = 0; j < end; j++) {
                        ListsResEntity listsResEntity = new ListsResEntity();
                        listsResEntity.setNodeNum(preLineInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        lineDataProcessor(lineList, i, j, listsResEntity);
                    }
                }
            }
            // 处理设备1的节点电压数据
            for (int i = 0; i < dev091Info.getNodeDutNum(); i++) {
                if (i != dev091Info.getNodeDutNum() - 1) {
                    for (int j = 0; j < 15; j++) {
                        // 返回list放入节点的编号，如zh001
                        ListsResEntity listsResEntity = new ListsResEntity();
                        // 编号位置从设备1的线路编号最后一个的位置开始
                        listsResEntity.setNodeNum(preNodeInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        nodeDataProcessor(nodeList, i, j, listsResEntity);
                    }
                } else {
                    int end = dev091Info.getNodeNum() % 15;
                    for (int j = 0; j < end; j++) {
                        // 返回list放入节点的编号，如zh001
                        ListsResEntity listsResEntity = new ListsResEntity();
                        // 编号位置从设备1的线路编号最后一个的位置开始
                        listsResEntity.setNodeNum(preNodeInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        nodeDataProcessor(nodeList, i, j, listsResEntity);
                    }
                }
            }
            /* 执行数据插入 */
            for (int i = 0; i < dev091Info.getAllDutNum(); i++) {
                if (i < dev091Info.getLineDutNum()) {
                    processDao.insertLineResult(AllGVar.DEV091_Tab_Name.get(i), lineList.get(i));
                } else {
                    processDao.insertNodeResult(AllGVar.DEV091_Tab_Name.get(i), nodeList.get(i - dev091Info.getLineDutNum()));
                }
            }
            /* 线路负载率=单相电流/额定电流 */
            LoadRateUtil loadRateUtil = new LoadRateUtil();
            List<LineResultEntity> lineList1 = new ArrayList<>();
            // 取库中各线路的额定电流
            String lineRatedName = "capacity_line_dev091";
            String nodeRatedName = "capacity_node_dev091";
            Float[] ratedCurrents = processDao.getLineRatedCurrent(lineRatedName);
            for (int i = 0; i < dev091Info.getLineDutNum(); i++) {
                // 从库中取出设备1的最新的一次线路的三相电流、有功无功等数据
                LineResultEntity lineResultEntity = processDao.selectLineData(AllGVar.DEV091_Tab_Name.get(i));
                lineList1.add(lineResultEntity);
            }
            // 执行线路负载率的计算,把节点的编号一起传过去处理
            List<LoadRateEntity> lineLoadRatio = loadRateUtil.lineLoadRate(times,
                    AllGVar.DEV091_LINE_INFO, lineList1, ratedCurrents);
            // 更新dev1中线路负载率
            processDao.updateLineLoadRatio(lineLoadRatio);
            // 执行插入线路负载率历史记录表
            processDao.insertLineLoadRatio(lineLoadRatio);
            /* 变电站负载率=UI/额定容量 */
            // 取读取数据中的UI值
            Float[] uis = resultListsEntity.getNodeUI();
            // 取库中的变电站的额定容量
            Float[] nodeRatedCapacity = processDao.getNodeRatedCapacity(nodeRatedName);
            List<LoadRateEntity> nodeLoadRatio = loadRateUtil.nodeLoadRate(times,
                    preNodeInfos, uis, nodeRatedCapacity);
            // 更新库中的变电站负载率
            processDao.updateNodeLoadRatio(nodeLoadRatio);
            // 执行插入变电站负载率历史记录表
            processDao.insertNodeLoadRatio(nodeLoadRatio);
            /**
             * 查询一次所有的负载率，发送给前端
             */
            ListsResEntity listsResEntity1 = dataReadServiceImpl.loadRateRead();
            webSocketService.sendTextMsg("/response/getLoadRate", listsResEntity1);
            System.out.println("杭州01读取完毕");
        } else {
            System.out.println("设备数据为空");
        }
    }

    @Override
    // @Async("task2")
    @Transactional
    public void dev092DataProcess() {
        Dev092Entity dev092Info = new Dev092Entity();
        DevCommonEntity devCommonEntity = new DevCommonEntity();
        BeanUtils.copyProperties(dev092Info, devCommonEntity);
        ResultListsEntity resultListsEntity = new AdsReadUtil().devStructRead(devCommonEntity);
        int size = resultListsEntity.getLineResultEntityList().size();
        if (size > 0) {
            /* 时间轴,获取时间并入库*/
            String[] times = new String[1];
            int id = 18;
            int countDev = processDao.getReadStatus(id);
            DateUtils dateUtils = new DateUtils();
            times[0] = dateUtils.timelineUtil(countDev + 1);
            processDao.updateReadStatus(id);
            List<LineResultEntity> lineList = resultListsEntity.getLineResultEntityList();
            List<NodeResultEntity> nodeList = resultListsEntity.getNodeResultEntityList();
            /* 前后端通讯：对lineList和nodeList做一下拆分 */
            // 处理设备1的线路电流与功率数据
            List<PreLineInfoEntity> preLineInfos = AllGVar.DEV092_LINE_INFO;
            List<PreNodeInfoEntity> preNodeInfos = AllGVar.DEV092_NODE_INFO;
            for (int i = 0; i < dev092Info.getLineDutNum(); i++) {
                // 放入线路的编号，如zh001
                if (i != dev092Info.getLineDutNum() - 1) {
                    for (int j = 0; j < 15; j++) {
                        ListsResEntity listsResEntity = new ListsResEntity();
                        listsResEntity.setNodeNum(preLineInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        lineDataProcessor(lineList, i, j, listsResEntity);
                    }
                } else {
                    // 最后一个表数据不满
                    int end = dev092Info.getLineNum() % 15;
                    for (int j = 0; j < end; j++) {
                        ListsResEntity listsResEntity = new ListsResEntity();
                        listsResEntity.setNodeNum(preLineInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        lineDataProcessor(lineList, i, j, listsResEntity);
                    }
                }
            }
            // 处理设备1的节点电压数据
            for (int i = 0; i < dev092Info.getNodeDutNum(); i++) {
                if (i != dev092Info.getNodeDutNum() - 1) {
                    for (int j = 0; j < 15; j++) {
                        // 返回list放入节点的编号，如zh001
                        ListsResEntity listsResEntity = new ListsResEntity();
                        // 编号位置从设备1的线路编号最后一个的位置开始
                        listsResEntity.setNodeNum(preNodeInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        nodeDataProcessor(nodeList, i, j, listsResEntity);
                    }
                } else {
                    int end = dev092Info.getNodeNum() % 15;
                    for (int j = 0; j < end; j++) {
                        // 返回list放入节点的编号，如zh001
                        ListsResEntity listsResEntity = new ListsResEntity();
                        // 编号位置从设备1的线路编号最后一个的位置开始
                        listsResEntity.setNodeNum(preNodeInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        nodeDataProcessor(nodeList, i, j, listsResEntity);
                    }
                }
            }
            /* 执行数据插入 */
            for (int i = 0; i < dev092Info.getAllDutNum(); i++) {
                if (i < dev092Info.getLineDutNum()) {
                    processDao.insertLineResult(AllGVar.DEV092_Tab_Name.get(i), lineList.get(i));
                } else {
                    processDao.insertNodeResult(AllGVar.DEV092_Tab_Name.get(i), nodeList.get(i - dev092Info.getLineDutNum()));
                }
            }
            /* 线路负载率=单相电流/额定电流 */
            LoadRateUtil loadRateUtil = new LoadRateUtil();
            List<LineResultEntity> lineList1 = new ArrayList<>();
            // 取库中各线路的额定电流
            String lineRatedName = "capacity_line_dev092";
            String nodeRatedName = "capacity_node_dev092";
            Float[] ratedCurrents = processDao.getLineRatedCurrent(lineRatedName);
            for (int i = 0; i < dev092Info.getLineDutNum(); i++) {
                // 从库中取出设备1的最新的一次线路的三相电流、有功无功等数据
                LineResultEntity lineResultEntity = processDao.selectLineData(AllGVar.DEV092_Tab_Name.get(i));
                lineList1.add(lineResultEntity);
            }
            // 执行线路负载率的计算,把节点的编号一起传过去处理
            List<LoadRateEntity> lineLoadRatio = loadRateUtil.lineLoadRate(times,
                    AllGVar.DEV092_LINE_INFO, lineList1, ratedCurrents);
            // 更新dev1中线路负载率
            processDao.updateLineLoadRatio(lineLoadRatio);
            // 执行插入线路负载率历史记录表
            processDao.insertLineLoadRatio(lineLoadRatio);
            /* 变电站负载率=UI/额定容量 */
            // 取读取数据中的UI值
            Float[] uis = resultListsEntity.getNodeUI();
            // 取库中的变电站的额定容量
            Float[] nodeRatedCapacity = processDao.getNodeRatedCapacity(nodeRatedName);
            List<LoadRateEntity> nodeLoadRatio = loadRateUtil.nodeLoadRate(times,
                    preNodeInfos, uis, nodeRatedCapacity);
            // 更新库中的变电站负载率
            processDao.updateNodeLoadRatio(nodeLoadRatio);
            // 执行插入变电站负载率历史记录表
            processDao.insertNodeLoadRatio(nodeLoadRatio);
            System.out.println("杭州02读取完毕");
        } else {
            System.out.println("设备数据为空");
        }
    }

    @Override
    // @Async("task1")
    @Transactional
    public void dev101DataProcess() {
        Dev101Entity dev101Info = new Dev101Entity();
        DevCommonEntity devCommonEntity = new DevCommonEntity();
        BeanUtils.copyProperties(dev101Info, devCommonEntity);
        ResultListsEntity resultListsEntity = new AdsReadUtil().devStructRead(devCommonEntity);
        int size = resultListsEntity.getLineResultEntityList().size();
        if (size > 0) {
            /* 时间轴,获取时间并入库*/
            String[] times = new String[1];
            int id = 19;
            int countDev = processDao.getReadStatus(id);
            DateUtils dateUtils = new DateUtils();
            times[0] = dateUtils.timelineUtil(countDev + 1);
            processDao.updateReadStatus(id);
            List<LineResultEntity> lineList = resultListsEntity.getLineResultEntityList();
            List<NodeResultEntity> nodeList = resultListsEntity.getNodeResultEntityList();
            /* 前后端通讯：对lineList和nodeList做一下拆分 */
            // 处理设备1的线路电流与功率数据
            List<PreLineInfoEntity> preLineInfos = AllGVar.DEV101_LINE_INFO;
            List<PreNodeInfoEntity> preNodeInfos = AllGVar.DEV101_NODE_INFO;
            for (int i = 0; i < dev101Info.getLineDutNum(); i++) {
                // 放入线路的编号，如zh001
                if (i != dev101Info.getLineDutNum() - 1) {
                    for (int j = 0; j < 15; j++) {
                        ListsResEntity listsResEntity = new ListsResEntity();
                        listsResEntity.setNodeNum(preLineInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        lineDataProcessor(lineList, i, j, listsResEntity);
                    }
                } else {
                    // 最后一个表数据不满
                    int end = dev101Info.getLineNum() % 15;
                    for (int j = 0; j < end; j++) {
                        ListsResEntity listsResEntity = new ListsResEntity();
                        listsResEntity.setNodeNum(preLineInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        lineDataProcessor(lineList, i, j, listsResEntity);
                    }
                }
            }
            // 处理设备1的节点电压数据
            for (int i = 0; i < dev101Info.getNodeDutNum(); i++) {
                if (i != dev101Info.getNodeDutNum() - 1) {
                    for (int j = 0; j < 15; j++) {
                        // 返回list放入节点的编号，如zh001
                        ListsResEntity listsResEntity = new ListsResEntity();
                        // 编号位置从设备1的线路编号最后一个的位置开始
                        listsResEntity.setNodeNum(preNodeInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        nodeDataProcessor(nodeList, i, j, listsResEntity);
                    }
                } else {
                    int end = dev101Info.getNodeNum() % 15;
                    for (int j = 0; j < end; j++) {
                        // 返回list放入节点的编号，如zh001
                        ListsResEntity listsResEntity = new ListsResEntity();
                        // 编号位置从设备1的线路编号最后一个的位置开始
                        listsResEntity.setNodeNum(preNodeInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        nodeDataProcessor(nodeList, i, j, listsResEntity);
                    }
                }
            }
            /* 执行数据插入 */
            for (int i = 0; i < dev101Info.getAllDutNum(); i++) {
                if (i < dev101Info.getLineDutNum()) {
                    processDao.insertLineResult(AllGVar.DEV101_Tab_Name.get(i), lineList.get(i));
                } else {
                    processDao.insertNodeResult(AllGVar.DEV101_Tab_Name.get(i), nodeList.get(i - dev101Info.getLineDutNum()));
                }
            }
            /* 线路负载率=单相电流/额定电流 */
            LoadRateUtil loadRateUtil = new LoadRateUtil();
            List<LineResultEntity> lineList1 = new ArrayList<>();
            // 取库中各线路的额定电流
            String lineRatedName = "capacity_line_dev101";
            String nodeRatedName = "capacity_node_dev101";
            Float[] ratedCurrents = processDao.getLineRatedCurrent(lineRatedName);
            for (int i = 0; i < dev101Info.getLineDutNum(); i++) {
                // 从库中取出设备1的最新的一次线路的三相电流、有功无功等数据
                LineResultEntity lineResultEntity = processDao.selectLineData(AllGVar.DEV101_Tab_Name.get(i));
                lineList1.add(lineResultEntity);
            }
            // 执行线路负载率的计算,把节点的编号一起传过去处理
            List<LoadRateEntity> lineLoadRatio = loadRateUtil.lineLoadRate(times,
                    AllGVar.DEV101_LINE_INFO, lineList1, ratedCurrents);
            // 更新dev1中线路负载率
            processDao.updateLineLoadRatio(lineLoadRatio);
            // 执行插入线路负载率历史记录表
            processDao.insertLineLoadRatio(lineLoadRatio);
            /* 变电站负载率=UI/额定容量 */
            // 取读取数据中的UI值
            Float[] uis = resultListsEntity.getNodeUI();
            // 取库中的变电站的额定容量
            Float[] nodeRatedCapacity = processDao.getNodeRatedCapacity(nodeRatedName);
            List<LoadRateEntity> nodeLoadRatio = loadRateUtil.nodeLoadRate(times,
                    preNodeInfos, uis, nodeRatedCapacity);
            // 更新库中的变电站负载率
            processDao.updateNodeLoadRatio(nodeLoadRatio);
            // 执行插入变电站负载率历史记录表
            processDao.insertNodeLoadRatio(nodeLoadRatio);
            System.out.println("杭州03读取完毕");
        } else {
            System.out.println("设备数据为空");
        }
    }

    @Override
    // @Async("task2")
    @Transactional
    public void dev102DataProcess() {
        Dev102Entity dev102Info = new Dev102Entity();
        DevCommonEntity devCommonEntity = new DevCommonEntity();
        BeanUtils.copyProperties(dev102Info, devCommonEntity);
        ResultListsEntity resultListsEntity = new AdsReadUtil().devStructRead(devCommonEntity);
        int size = resultListsEntity.getLineResultEntityList().size();
        if (size > 0) {
            /* 时间轴,获取时间并入库*/
            String[] times = new String[1];
            int id = 20;
            int countDev = processDao.getReadStatus(id);
            DateUtils dateUtils = new DateUtils();
            times[0] = dateUtils.timelineUtil(countDev + 1);
            processDao.updateReadStatus(id);
            List<LineResultEntity> lineList = resultListsEntity.getLineResultEntityList();
            List<NodeResultEntity> nodeList = resultListsEntity.getNodeResultEntityList();
            /* 前后端通讯：对lineList和nodeList做一下拆分 */
            // 处理设备1的线路电流与功率数据
            List<PreLineInfoEntity> preLineInfos = AllGVar.DEV102_LINE_INFO;
            List<PreNodeInfoEntity> preNodeInfos = AllGVar.DEV102_NODE_INFO;
            for (int i = 0; i < dev102Info.getLineDutNum(); i++) {
                // 放入线路的编号，如zh001
                if (i != dev102Info.getLineDutNum() - 1) {
                    for (int j = 0; j < 15; j++) {
                        ListsResEntity listsResEntity = new ListsResEntity();
                        listsResEntity.setNodeNum(preLineInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        lineDataProcessor(lineList, i, j, listsResEntity);
                    }
                } else {
                    // 最后一个表数据不满
                    int end = dev102Info.getLineNum() % 15;
                    for (int j = 0; j < end; j++) {
                        ListsResEntity listsResEntity = new ListsResEntity();
                        listsResEntity.setNodeNum(preLineInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        lineDataProcessor(lineList, i, j, listsResEntity);
                    }
                }
            }
            // 处理设备1的节点电压数据
            for (int i = 0; i < dev102Info.getNodeDutNum(); i++) {
                if (i != dev102Info.getNodeDutNum() - 1) {
                    for (int j = 0; j < 15; j++) {
                        // 返回list放入节点的编号，如zh001
                        ListsResEntity listsResEntity = new ListsResEntity();
                        // 编号位置从设备1的线路编号最后一个的位置开始
                        listsResEntity.setNodeNum(preNodeInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        nodeDataProcessor(nodeList, i, j, listsResEntity);
                    }
                } else {
                    int end = dev102Info.getNodeNum() % 15;
                    for (int j = 0; j < end; j++) {
                        // 返回list放入节点的编号，如zh001
                        ListsResEntity listsResEntity = new ListsResEntity();
                        // 编号位置从设备1的线路编号最后一个的位置开始
                        listsResEntity.setNodeNum(preNodeInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        nodeDataProcessor(nodeList, i, j, listsResEntity);
                    }
                }
            }
            /* 执行数据插入 */
            for (int i = 0; i < dev102Info.getAllDutNum(); i++) {
                if (i < dev102Info.getLineDutNum()) {
                    processDao.insertLineResult(AllGVar.DEV102_Tab_Name.get(i), lineList.get(i));
                } else {
                    processDao.insertNodeResult(AllGVar.DEV102_Tab_Name.get(i), nodeList.get(i - dev102Info.getLineDutNum()));
                }
            }
            /* 线路负载率=单相电流/额定电流 */
            LoadRateUtil loadRateUtil = new LoadRateUtil();
            List<LineResultEntity> lineList1 = new ArrayList<>();
            // 取库中各线路的额定电流
            String lineRatedName = "capacity_line_dev102";
            String nodeRatedName = "capacity_node_dev102";
            Float[] ratedCurrents = processDao.getLineRatedCurrent(lineRatedName);
            for (int i = 0; i < dev102Info.getLineDutNum(); i++) {
                // 从库中取出设备1的最新的一次线路的三相电流、有功无功等数据
                LineResultEntity lineResultEntity = processDao.selectLineData(AllGVar.DEV102_Tab_Name.get(i));
                lineList1.add(lineResultEntity);
            }
            // 执行线路负载率的计算,把节点的编号一起传过去处理
            List<LoadRateEntity> lineLoadRatio = loadRateUtil.lineLoadRate(times,
                    AllGVar.DEV102_LINE_INFO, lineList1, ratedCurrents);
            // 更新dev1中线路负载率
            processDao.updateLineLoadRatio(lineLoadRatio);
            // 执行插入线路负载率历史记录表
            processDao.insertLineLoadRatio(lineLoadRatio);
            /* 变电站负载率=UI/额定容量 */
            // 取读取数据中的UI值
            Float[] uis = resultListsEntity.getNodeUI();
            // 取库中的变电站的额定容量
            Float[] nodeRatedCapacity = processDao.getNodeRatedCapacity(nodeRatedName);
            List<LoadRateEntity> nodeLoadRatio = loadRateUtil.nodeLoadRate(times,
                    preNodeInfos, uis, nodeRatedCapacity);
            // 更新库中的变电站负载率
            processDao.updateNodeLoadRatio(nodeLoadRatio);
            // 执行插入变电站负载率历史记录表
            processDao.insertNodeLoadRatio(nodeLoadRatio);
            System.out.println("杭州04读取完毕");
        } else {
            System.out.println("设备数据为空");
        }
    }

    @Override
    // @Async("task1")
    @Transactional
    public void dev111DataProcess() {
        Dev111Entity dev111Info = new Dev111Entity();
        DevCommonEntity devCommonEntity = new DevCommonEntity();
        BeanUtils.copyProperties(dev111Info, devCommonEntity);
        ResultListsEntity resultListsEntity = new AdsReadUtil().devStructRead(devCommonEntity);
        int size = resultListsEntity.getLineResultEntityList().size();
        if (size > 0) {
            /* 时间轴,获取时间并入库*/
            String[] times = new String[1];
            int id = 21;
            int countDev = processDao.getReadStatus(id);
            DateUtils dateUtils = new DateUtils();
            times[0] = dateUtils.timelineUtil(countDev + 1);
            processDao.updateReadStatus(id);
            List<LineResultEntity> lineList = resultListsEntity.getLineResultEntityList();
            List<NodeResultEntity> nodeList = resultListsEntity.getNodeResultEntityList();
            /* 前后端通讯：对lineList和nodeList做一下拆分 */
            // 处理设备1的线路电流与功率数据
            List<PreLineInfoEntity> preLineInfos = AllGVar.DEV111_LINE_INFO;
            List<PreNodeInfoEntity> preNodeInfos = AllGVar.DEV111_NODE_INFO;
            for (int i = 0; i < dev111Info.getLineDutNum(); i++) {
                // 放入线路的编号，如zh001
                if (i != dev111Info.getLineDutNum() - 1) {
                    for (int j = 0; j < 15; j++) {
                        ListsResEntity listsResEntity = new ListsResEntity();
                        listsResEntity.setNodeNum(preLineInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        lineDataProcessor(lineList, i, j, listsResEntity);
                    }
                } else {
                    // 最后一个表数据不满
                    int end = dev111Info.getLineNum() % 15;
                    for (int j = 0; j < end; j++) {
                        ListsResEntity listsResEntity = new ListsResEntity();
                        listsResEntity.setNodeNum(preLineInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        lineDataProcessor(lineList, i, j, listsResEntity);
                    }
                }
            }
            // 处理设备1的节点电压数据
            for (int i = 0; i < dev111Info.getNodeDutNum(); i++) {
                if (i != dev111Info.getNodeDutNum() - 1) {
                    for (int j = 0; j < 15; j++) {
                        // 返回list放入节点的编号，如zh001
                        ListsResEntity listsResEntity = new ListsResEntity();
                        // 编号位置从设备1的线路编号最后一个的位置开始
                        listsResEntity.setNodeNum(preNodeInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        nodeDataProcessor(nodeList, i, j, listsResEntity);
                    }
                } else {
                    int end = dev111Info.getNodeNum() % 15;
                    for (int j = 0; j < end; j++) {
                        // 返回list放入节点的编号，如zh001
                        ListsResEntity listsResEntity = new ListsResEntity();
                        // 编号位置从设备1的线路编号最后一个的位置开始
                        listsResEntity.setNodeNum(preNodeInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        nodeDataProcessor(nodeList, i, j, listsResEntity);
                    }
                }
            }
            /* 执行数据插入 */
            for (int i = 0; i < dev111Info.getAllDutNum(); i++) {
                if (i < dev111Info.getLineDutNum()) {
                    processDao.insertLineResult(AllGVar.DEV111_Tab_Name.get(i), lineList.get(i));
                } else {
                    processDao.insertNodeResult(AllGVar.DEV111_Tab_Name.get(i), nodeList.get(i - dev111Info.getLineDutNum()));
                }
            }
            /* 线路负载率=单相电流/额定电流 */
            LoadRateUtil loadRateUtil = new LoadRateUtil();
            List<LineResultEntity> lineList1 = new ArrayList<>();
            // 取库中各线路的额定电流
            String lineRatedName = "capacity_line_dev111";
            String nodeRatedName = "capacity_node_dev111";
            Float[] ratedCurrents = processDao.getLineRatedCurrent(lineRatedName);
            for (int i = 0; i < dev111Info.getLineDutNum(); i++) {
                // 从库中取出设备1的最新的一次线路的三相电流、有功无功等数据
                LineResultEntity lineResultEntity = processDao.selectLineData(AllGVar.DEV111_Tab_Name.get(i));
                lineList1.add(lineResultEntity);
            }
            // 执行线路负载率的计算,把节点的编号一起传过去处理
            List<LoadRateEntity> lineLoadRatio = loadRateUtil.lineLoadRate(times,
                    AllGVar.DEV111_LINE_INFO, lineList1, ratedCurrents);
            // 更新dev1中线路负载率
            processDao.updateLineLoadRatio(lineLoadRatio);
            // 执行插入线路负载率历史记录表
            processDao.insertLineLoadRatio(lineLoadRatio);
            /* 变电站负载率=UI/额定容量 */
            // 取读取数据中的UI值
            Float[] uis = resultListsEntity.getNodeUI();
            // 取库中的变电站的额定容量
            Float[] nodeRatedCapacity = processDao.getNodeRatedCapacity(nodeRatedName);
            List<LoadRateEntity> nodeLoadRatio = loadRateUtil.nodeLoadRate(times,
                    preNodeInfos, uis, nodeRatedCapacity);
            // 更新库中的变电站负载率
            processDao.updateNodeLoadRatio(nodeLoadRatio);
            // 执行插入变电站负载率历史记录表
            processDao.insertNodeLoadRatio(nodeLoadRatio);
            System.out.println("杭州05读取完毕");
        } else {
            System.out.println("设备数据为空");
        }
    }

    @Override
    // @Async("task2")
    @Transactional
    public void dev112DataProcess() {
        Dev112Entity dev112Info = new Dev112Entity();
        DevCommonEntity devCommonEntity = new DevCommonEntity();
        BeanUtils.copyProperties(dev112Info, devCommonEntity);
        ResultListsEntity resultListsEntity = new AdsReadUtil().devStructRead(devCommonEntity);
        int size = resultListsEntity.getLineResultEntityList().size();
        if (size > 0) {
            /* 时间轴,获取时间并入库*/
            String[] times = new String[1];
            int id = 22;
            int countDev = processDao.getReadStatus(id);
            DateUtils dateUtils = new DateUtils();
            times[0] = dateUtils.timelineUtil(countDev + 1);
            processDao.updateReadStatus(id);
            List<LineResultEntity> lineList = resultListsEntity.getLineResultEntityList();
            List<NodeResultEntity> nodeList = resultListsEntity.getNodeResultEntityList();
            /* 前后端通讯：对lineList和nodeList做一下拆分 */
            // 处理设备1的线路电流与功率数据
            List<PreLineInfoEntity> preLineInfos = AllGVar.DEV112_LINE_INFO;
            List<PreNodeInfoEntity> preNodeInfos = AllGVar.DEV112_NODE_INFO;
            for (int i = 0; i < dev112Info.getLineDutNum(); i++) {
                // 放入线路的编号，如zh001
                if (i != dev112Info.getLineDutNum() - 1) {
                    for (int j = 0; j < 15; j++) {
                        ListsResEntity listsResEntity = new ListsResEntity();
                        listsResEntity.setNodeNum(preLineInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        lineDataProcessor(lineList, i, j, listsResEntity);
                    }
                } else {
                    // 最后一个表数据不满
                    int end = dev112Info.getLineNum() % 15;
                    for (int j = 0; j < end; j++) {
                        ListsResEntity listsResEntity = new ListsResEntity();
                        listsResEntity.setNodeNum(preLineInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        lineDataProcessor(lineList, i, j, listsResEntity);
                    }
                }
            }
            // 处理设备1的节点电压数据
            for (int i = 0; i < dev112Info.getNodeDutNum(); i++) {
                if (i != dev112Info.getNodeDutNum() - 1) {
                    for (int j = 0; j < 15; j++) {
                        // 返回list放入节点的编号，如zh001
                        ListsResEntity listsResEntity = new ListsResEntity();
                        // 编号位置从设备1的线路编号最后一个的位置开始
                        listsResEntity.setNodeNum(preNodeInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        nodeDataProcessor(nodeList, i, j, listsResEntity);
                    }
                } else {
                    int end = dev112Info.getNodeNum() % 15;
                    for (int j = 0; j < end; j++) {
                        // 返回list放入节点的编号，如zh001
                        ListsResEntity listsResEntity = new ListsResEntity();
                        // 编号位置从设备1的线路编号最后一个的位置开始
                        listsResEntity.setNodeNum(preNodeInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        nodeDataProcessor(nodeList, i, j, listsResEntity);
                    }
                }
            }
            /* 执行数据插入 */
            for (int i = 0; i < dev112Info.getAllDutNum(); i++) {
                if (i < dev112Info.getLineDutNum()) {
                    processDao.insertLineResult(AllGVar.DEV112_Tab_Name.get(i), lineList.get(i));
                } else {
                    processDao.insertNodeResult(AllGVar.DEV112_Tab_Name.get(i), nodeList.get(i - dev112Info.getLineDutNum()));
                }
            }
            /* 线路负载率=单相电流/额定电流 */
            LoadRateUtil loadRateUtil = new LoadRateUtil();
            List<LineResultEntity> lineList1 = new ArrayList<>();
            // 取库中各线路的额定电流
            String lineRatedName = "capacity_line_dev112";
            String nodeRatedName = "capacity_node_dev112";
            Float[] ratedCurrents = processDao.getLineRatedCurrent(lineRatedName);
            for (int i = 0; i < dev112Info.getLineDutNum(); i++) {
                // 从库中取出设备1的最新的一次线路的三相电流、有功无功等数据
                LineResultEntity lineResultEntity = processDao.selectLineData(AllGVar.DEV112_Tab_Name.get(i));
                lineList1.add(lineResultEntity);
            }
            // 执行线路负载率的计算,把节点的编号一起传过去处理
            List<LoadRateEntity> lineLoadRatio = loadRateUtil.lineLoadRate(times,
                    AllGVar.DEV112_LINE_INFO, lineList1, ratedCurrents);
            // 更新dev1中线路负载率
            processDao.updateLineLoadRatio(lineLoadRatio);
            // 执行插入线路负载率历史记录表
            processDao.insertLineLoadRatio(lineLoadRatio);
            /* 变电站负载率=UI/额定容量 */
            // 取读取数据中的UI值
            Float[] uis = resultListsEntity.getNodeUI();
            // 取库中的变电站的额定容量
            Float[] nodeRatedCapacity = processDao.getNodeRatedCapacity(nodeRatedName);
            List<LoadRateEntity> nodeLoadRatio = loadRateUtil.nodeLoadRate(times,
                    preNodeInfos, uis, nodeRatedCapacity);
            // 更新库中的变电站负载率
            processDao.updateNodeLoadRatio(nodeLoadRatio);
            // 执行插入变电站负载率历史记录表
            processDao.insertNodeLoadRatio(nodeLoadRatio);
            System.out.println("杭州06读取完毕");
        } else {
            System.out.println("设备数据为空");
        }
    }

    @Override
    // @Async("task1")
    @Transactional
    public void dev121DataProcess() {
        Dev121Entity dev121Info = new Dev121Entity();
        DevCommonEntity devCommonEntity = new DevCommonEntity();
        BeanUtils.copyProperties(dev121Info, devCommonEntity);
        ResultListsEntity resultListsEntity = new AdsReadUtil().devStructRead(devCommonEntity);
        int size = resultListsEntity.getLineResultEntityList().size();
        if (size > 0) {
            /* 时间轴,获取时间并入库*/
            String[] times = new String[1];
            int id = 23;
            int countDev = processDao.getReadStatus(id);
            DateUtils dateUtils = new DateUtils();
            times[0] = dateUtils.timelineUtil(countDev + 1);
            processDao.updateReadStatus(id);
            List<LineResultEntity> lineList = resultListsEntity.getLineResultEntityList();
            List<NodeResultEntity> nodeList = resultListsEntity.getNodeResultEntityList();
            /* 前后端通讯：对lineList和nodeList做一下拆分 */
            // 处理设备1的线路电流与功率数据
            List<PreLineInfoEntity> preLineInfos = AllGVar.DEV121_LINE_INFO;
            List<PreNodeInfoEntity> preNodeInfos = AllGVar.DEV121_NODE_INFO;
            for (int i = 0; i < dev121Info.getLineDutNum(); i++) {
                // 放入线路的编号，如zh001
                if (i != dev121Info.getLineDutNum() - 1) {
                    for (int j = 0; j < 15; j++) {
                        ListsResEntity listsResEntity = new ListsResEntity();
                        listsResEntity.setNodeNum(preLineInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        lineDataProcessor(lineList, i, j, listsResEntity);
                    }
                } else {
                    // 最后一个表数据不满
                    int end = dev121Info.getLineNum() % 15;
                    for (int j = 0; j < end; j++) {
                        ListsResEntity listsResEntity = new ListsResEntity();
                        listsResEntity.setNodeNum(preLineInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        lineDataProcessor(lineList, i, j, listsResEntity);
                    }
                }
            }
            // 处理设备1的节点电压数据
            for (int i = 0; i < dev121Info.getNodeDutNum(); i++) {
                if (i != dev121Info.getNodeDutNum() - 1) {
                    for (int j = 0; j < 15; j++) {
                        // 返回list放入节点的编号，如zh001
                        ListsResEntity listsResEntity = new ListsResEntity();
                        // 编号位置从设备1的线路编号最后一个的位置开始
                        listsResEntity.setNodeNum(preNodeInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        nodeDataProcessor(nodeList, i, j, listsResEntity);
                    }
                } else {
                    int end = dev121Info.getNodeNum() % 15;
                    for (int j = 0; j < end; j++) {
                        // 返回list放入节点的编号，如zh001
                        ListsResEntity listsResEntity = new ListsResEntity();
                        // 编号位置从设备1的线路编号最后一个的位置开始
                        listsResEntity.setNodeNum(preNodeInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        nodeDataProcessor(nodeList, i, j, listsResEntity);
                    }
                }
            }
            /* 执行数据插入 */
            for (int i = 0; i < dev121Info.getAllDutNum(); i++) {
                if (i < dev121Info.getLineDutNum()) {
                    processDao.insertLineResult(AllGVar.DEV121_Tab_Name.get(i), lineList.get(i));
                } else {
                    processDao.insertNodeResult(AllGVar.DEV121_Tab_Name.get(i), nodeList.get(i - dev121Info.getLineDutNum()));
                }
            }
            /* 线路负载率=单相电流/额定电流 */
            LoadRateUtil loadRateUtil = new LoadRateUtil();
            List<LineResultEntity> lineList1 = new ArrayList<>();
            // 取库中各线路的额定电流
            String lineRatedName = "capacity_line_dev121";
            String nodeRatedName = "capacity_node_dev121";
            Float[] ratedCurrents = processDao.getLineRatedCurrent(lineRatedName);
            for (int i = 0; i < dev121Info.getLineDutNum(); i++) {
                // 从库中取出设备1的最新的一次线路的三相电流、有功无功等数据
                LineResultEntity lineResultEntity = processDao.selectLineData(AllGVar.DEV121_Tab_Name.get(i));
                lineList1.add(lineResultEntity);
            }
            // 执行线路负载率的计算,把节点的编号一起传过去处理
            List<LoadRateEntity> lineLoadRatio = loadRateUtil.lineLoadRate(times,
                    AllGVar.DEV121_LINE_INFO, lineList1, ratedCurrents);
            // 更新dev1中线路负载率
            processDao.updateLineLoadRatio(lineLoadRatio);
            // 执行插入线路负载率历史记录表
            processDao.insertLineLoadRatio(lineLoadRatio);
            /* 变电站负载率=UI/额定容量 */
            // 取读取数据中的UI值
            Float[] uis = resultListsEntity.getNodeUI();
            // 取库中的变电站的额定容量
            Float[] nodeRatedCapacity = processDao.getNodeRatedCapacity(nodeRatedName);
            List<LoadRateEntity> nodeLoadRatio = loadRateUtil.nodeLoadRate(times,
                    preNodeInfos, uis, nodeRatedCapacity);
            // 更新库中的变电站负载率
            processDao.updateNodeLoadRatio(nodeLoadRatio);
            // 执行插入变电站负载率历史记录表
            processDao.insertNodeLoadRatio(nodeLoadRatio);
            System.out.println("杭州07读取完毕");
        } else {
            System.out.println("设备数据为空");
        }
    }

    @Override
    // @Async("task1")
    @Transactional
    public void dev131DataProcess() {
        Dev131Entity dev131Info = new Dev131Entity(); //修改实体 dev0162Info
        DevCommonEntity devCommonEntity = new DevCommonEntity();
        BeanUtils.copyProperties(dev131Info, devCommonEntity);
        ResultListsEntity resultListsEntity = new AdsReadUtil().devStructRead(devCommonEntity);
        int size = resultListsEntity.getLineResultEntityList().size();
        if (size > 0) {
            /* 时间轴,获取时间并入库*/
            String[] times = new String[1];
            int id = 25;
            int countDev = processDao.getReadStatus(id);
            DateUtils dateUtils = new DateUtils();
            times[0] = dateUtils.timelineUtil(countDev + 1);
            processDao.updateReadStatus(id);
            List<LineResultEntity> lineList = resultListsEntity.getLineResultEntityList();
            List<NodeResultEntity> nodeList = resultListsEntity.getNodeResultEntityList();
            /* 前后端通讯：对lineList和nodeList做一下拆分 */
            // 处理设备1的线路电流与功率数据
            List<PreLineInfoEntity> preLineInfos = AllGVar.DEV131_LINE_INFO;
            List<PreNodeInfoEntity> preNodeInfos = AllGVar.DEV131_NODE_INFO;
            for (int i = 0; i < dev131Info.getLineDutNum(); i++) {
                // 放入线路的编号，如zh001
                if (i != dev131Info.getLineDutNum() - 1) {
                    for (int j = 0; j < 15; j++) {
                        ListsResEntity listsResEntity = new ListsResEntity();
                        listsResEntity.setNodeNum(preLineInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        lineDataProcessor(lineList, i, j, listsResEntity);
                    }
                } else {
                    // 最后一个表数据不满
                    int end = dev131Info.getLineNum() % 15;
                    for (int j = 0; j < end; j++) {
                        ListsResEntity listsResEntity = new ListsResEntity();
                        listsResEntity.setNodeNum(preLineInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        lineDataProcessor(lineList, i, j, listsResEntity);
                    }
                }
            }
            // 处理设备1的节点电压数据
            for (int i = 0; i < dev131Info.getNodeDutNum(); i++) {
                if (i != dev131Info.getNodeDutNum() - 1) {
                    for (int j = 0; j < 15; j++) {
                        // 返回list放入节点的编号，如zh001
                        ListsResEntity listsResEntity = new ListsResEntity();
                        // 编号位置从设备1的线路编号最后一个的位置开始
                        listsResEntity.setNodeNum(preNodeInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        nodeDataProcessor(nodeList, i, j, listsResEntity);
                    }
                } else {
                    int end = dev131Info.getNodeNum() % 15;
                    for (int j = 0; j < end; j++) {
                        // 返回list放入节点的编号，如zh001
                        ListsResEntity listsResEntity = new ListsResEntity();
                        // 编号位置从设备1的线路编号最后一个的位置开始
                        listsResEntity.setNodeNum(preNodeInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        nodeDataProcessor(nodeList, i, j, listsResEntity);
                    }
                }
            }
            /* 执行数据插入 */
            for (int i = 0; i < dev131Info.getAllDutNum(); i++) {
                if (i < dev131Info.getLineDutNum()) {
                    processDao.insertLineResult(AllGVar.DEV131_Tab_Name.get(i), lineList.get(i));
                } else {
                    processDao.insertNodeResult(AllGVar.DEV131_Tab_Name.get(i), nodeList.get(i - dev131Info.getLineDutNum()));
                }
            }
            /* 线路负载率=单相电流/额定电流 */
            LoadRateUtil loadRateUtil = new LoadRateUtil();
            List<LineResultEntity> lineList1 = new ArrayList<>();
            // 取库中各线路的额定电流
            String lineRatedName = "capacity_line_dev131";
            String nodeRatedName = "capacity_node_dev131";
            Float[] ratedCurrents = processDao.getLineRatedCurrent(lineRatedName);
            for (int i = 0; i < dev131Info.getLineDutNum(); i++) {
                // 从库中取出设备1的最新的一次线路的三相电流、有功无功等数据
                LineResultEntity lineResultEntity = processDao.selectLineData(AllGVar.DEV131_Tab_Name.get(i));
                lineList1.add(lineResultEntity);
            }
            // 执行线路负载率的计算,把节点的编号一起传过去处理
            List<LoadRateEntity> lineLoadRatio = loadRateUtil.lineLoadRate(times,
                    AllGVar.DEV131_LINE_INFO, lineList1, ratedCurrents);
            // 更新dev1中线路负载率
            processDao.updateLineLoadRatio(lineLoadRatio);
            // 执行插入线路负载率历史记录表
            processDao.insertLineLoadRatio(lineLoadRatio);
            /* 变电站负载率=UI/额定容量 */
            // 取读取数据中的UI值
            Float[] uis = resultListsEntity.getNodeUI();
            // 取库中的变电站的额定容量
            Float[] nodeRatedCapacity = processDao.getNodeRatedCapacity(nodeRatedName);
            List<LoadRateEntity> nodeLoadRatio = loadRateUtil.nodeLoadRate(times,
                    preNodeInfos, uis, nodeRatedCapacity);
            // 更新库中的变电站负载率
            processDao.updateNodeLoadRatio(nodeLoadRatio);
            // 执行插入变电站负载率历史记录表
            processDao.insertNodeLoadRatio(nodeLoadRatio);
            /* 查询一次所有的负载率，发送给前端 */
            ListsResEntity listsResEntity1 = dataReadServiceImpl.loadRateRead();
            webSocketService.sendTextMsg("/response/getLoadRate", listsResEntity1);
            System.out.println("金华01读取结束");
        } else {
            System.out.println("设备数据为空");
        }
    }

    @Override
    // @Async("task2")
    @Transactional
    public void dev132DataProcess() {
        Dev132Entity dev132Info = new Dev132Entity(); //修改实体 dev0162Info
        DevCommonEntity devCommonEntity = new DevCommonEntity();
        BeanUtils.copyProperties(dev132Info, devCommonEntity);
        ResultListsEntity resultListsEntity = new AdsReadUtil().devStructRead(devCommonEntity);
        int size = resultListsEntity.getLineResultEntityList().size();
        if (size > 0) {
            /* 时间轴,获取时间并入库*/
            String[] times = new String[1];
            int id = 26;
            int countDev = processDao.getReadStatus(id);
            DateUtils dateUtils = new DateUtils();
            times[0] = dateUtils.timelineUtil(countDev + 1);
            processDao.updateReadStatus(id);
            List<LineResultEntity> lineList = resultListsEntity.getLineResultEntityList();
            List<NodeResultEntity> nodeList = resultListsEntity.getNodeResultEntityList();
            /* 前后端通讯：对lineList和nodeList做一下拆分 */
            // 处理设备1的线路电流与功率数据
            List<PreLineInfoEntity> preLineInfos = AllGVar.DEV132_LINE_INFO;
            List<PreNodeInfoEntity> preNodeInfos = AllGVar.DEV132_NODE_INFO;
            for (int i = 0; i < dev132Info.getLineDutNum(); i++) {
                // 放入线路的编号，如zh001
                if (i != dev132Info.getLineDutNum() - 1) {
                    for (int j = 0; j < 15; j++) {
                        ListsResEntity listsResEntity = new ListsResEntity();
                        listsResEntity.setNodeNum(preLineInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        lineDataProcessor(lineList, i, j, listsResEntity);
                    }
                } else {
                    // 最后一个表数据不满
                    int end = dev132Info.getLineNum() % 15;
                    for (int j = 0; j < end; j++) {
                        ListsResEntity listsResEntity = new ListsResEntity();
                        listsResEntity.setNodeNum(preLineInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        lineDataProcessor(lineList, i, j, listsResEntity);
                    }
                }
            }
            // 处理设备1的节点电压数据
            for (int i = 0; i < dev132Info.getNodeDutNum(); i++) {
                if (i != dev132Info.getNodeDutNum() - 1) {
                    for (int j = 0; j < 15; j++) {
                        // 返回list放入节点的编号，如zh001
                        ListsResEntity listsResEntity = new ListsResEntity();
                        // 编号位置从设备1的线路编号最后一个的位置开始
                        listsResEntity.setNodeNum(preNodeInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        nodeDataProcessor(nodeList, i, j, listsResEntity);
                    }
                } else {
                    int end = dev132Info.getNodeNum() % 15;
                    for (int j = 0; j < end; j++) {
                        // 返回list放入节点的编号，如zh001
                        ListsResEntity listsResEntity = new ListsResEntity();
                        // 编号位置从设备1的线路编号最后一个的位置开始
                        listsResEntity.setNodeNum(preNodeInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        nodeDataProcessor(nodeList, i, j, listsResEntity);
                    }
                }
            }
            /* 执行数据插入 */
            for (int i = 0; i < dev132Info.getAllDutNum(); i++) {
                if (i < dev132Info.getLineDutNum()) {
                    processDao.insertLineResult(AllGVar.DEV132_Tab_Name.get(i), lineList.get(i));
                } else {
                    processDao.insertNodeResult(AllGVar.DEV132_Tab_Name.get(i), nodeList.get(i - dev132Info.getLineDutNum()));
                }
            }
            /* 线路负载率=单相电流/额定电流 */
            LoadRateUtil loadRateUtil = new LoadRateUtil();
            List<LineResultEntity> lineList1 = new ArrayList<>();
            // 取库中各线路的额定电流
            String lineRatedName = "capacity_line_dev132";
            String nodeRatedName = "capacity_node_dev132";
            Float[] ratedCurrents = processDao.getLineRatedCurrent(lineRatedName);
            for (int i = 0; i < dev132Info.getLineDutNum(); i++) {
                // 从库中取出设备1的最新的一次线路的三相电流、有功无功等数据
                LineResultEntity lineResultEntity = processDao.selectLineData(AllGVar.DEV132_Tab_Name.get(i));
                lineList1.add(lineResultEntity);
            }
            // 执行线路负载率的计算,把节点的编号一起传过去处理
            List<LoadRateEntity> lineLoadRatio = loadRateUtil.lineLoadRate(times,
                    AllGVar.DEV132_LINE_INFO, lineList1, ratedCurrents);
            // 更新dev1中线路负载率
            processDao.updateLineLoadRatio(lineLoadRatio);
            // 执行插入线路负载率历史记录表
            processDao.insertLineLoadRatio(lineLoadRatio);
            /* 变电站负载率=UI/额定容量 */
            // 取读取数据中的UI值
            Float[] uis = resultListsEntity.getNodeUI();
            // 取库中的变电站的额定容量
            Float[] nodeRatedCapacity = processDao.getNodeRatedCapacity(nodeRatedName);
            List<LoadRateEntity> nodeLoadRatio = loadRateUtil.nodeLoadRate(times,
                    preNodeInfos, uis, nodeRatedCapacity);
            // 更新库中的变电站负载率
            processDao.updateNodeLoadRatio(nodeLoadRatio);
            // 执行插入变电站负载率历史记录表
            processDao.insertNodeLoadRatio(nodeLoadRatio);
            /* 查询一次所有的负载率，发送给前端 */
            ListsResEntity listsResEntity1 = dataReadServiceImpl.loadRateRead();
            webSocketService.sendTextMsg("/response/getLoadRate", listsResEntity1);
            System.out.println("金华02读取结束");
        } else {
            System.out.println("设备数据为空");
        }
    }

    @Override
    // @Async("task1")
    @Transactional
    public void dev141DataProcess() {
        Dev141Entity dev141Info = new Dev141Entity();
        DevCommonEntity devCommonEntity = new DevCommonEntity();
        BeanUtils.copyProperties(dev141Info, devCommonEntity);
        ResultListsEntity resultListsEntity = new AdsReadUtil().devStructRead(devCommonEntity);
        int size = resultListsEntity.getLineResultEntityList().size();
        if (size > 0) {
            /* 时间轴,获取时间并入库*/
            String[] times = new String[1];
            int id = 27;
            int countDev = processDao.getReadStatus(id);
            DateUtils dateUtils = new DateUtils();
            times[0] = dateUtils.timelineUtil(countDev + 1);
            processDao.updateReadStatus(id);
            List<LineResultEntity> lineList = resultListsEntity.getLineResultEntityList();
            List<NodeResultEntity> nodeList = resultListsEntity.getNodeResultEntityList();
            /* 前后端通讯：对lineList和nodeList做一下拆分 */
            // 处理设备1的线路电流与功率数据
            List<PreLineInfoEntity> preLineInfos = AllGVar.DEV141_LINE_INFO;
            List<PreNodeInfoEntity> preNodeInfos = AllGVar.DEV141_NODE_INFO;
            for (int i = 0; i < dev141Info.getLineDutNum(); i++) {
                // 放入线路的编号，如zh001
                if (i != dev141Info.getLineDutNum() - 1) {
                    for (int j = 0; j < 15; j++) {
                        ListsResEntity listsResEntity = new ListsResEntity();
                        listsResEntity.setNodeNum(preLineInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        lineDataProcessor(lineList, i, j, listsResEntity);
                    }
                } else {
                    // 最后一个表数据不满
                    int end = dev141Info.getLineNum() % 15;
                    for (int j = 0; j < end; j++) {
                        ListsResEntity listsResEntity = new ListsResEntity();
                        listsResEntity.setNodeNum(preLineInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        lineDataProcessor(lineList, i, j, listsResEntity);
                    }
                }
            }
            // 处理设备1的节点电压数据
            for (int i = 0; i < dev141Info.getNodeDutNum(); i++) {
                if (i != dev141Info.getNodeDutNum() - 1) {
                    for (int j = 0; j < 15; j++) {
                        // 返回list放入节点的编号，如zh001
                        ListsResEntity listsResEntity = new ListsResEntity();
                        // 编号位置从设备1的线路编号最后一个的位置开始
                        listsResEntity.setNodeNum(preNodeInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        nodeDataProcessor(nodeList, i, j, listsResEntity);
                    }
                } else {
                    int end = dev141Info.getNodeNum() % 15;
                    for (int j = 0; j < end; j++) {
                        // 返回list放入节点的编号，如zh001
                        ListsResEntity listsResEntity = new ListsResEntity();
                        // 编号位置从设备1的线路编号最后一个的位置开始
                        listsResEntity.setNodeNum(preNodeInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        nodeDataProcessor(nodeList, i, j, listsResEntity);
                    }
                }
            }
            /* 执行数据插入 */
            for (int i = 0; i < dev141Info.getAllDutNum(); i++) {
                if (i < dev141Info.getLineDutNum()) {
                    processDao.insertLineResult(AllGVar.DEV141_Tab_Name.get(i), lineList.get(i));
                } else {
                    processDao.insertNodeResult(AllGVar.DEV141_Tab_Name.get(i), nodeList.get(i - dev141Info.getLineDutNum()));
                }
            }
            /* 线路负载率=单相电流/额定电流 */
            LoadRateUtil loadRateUtil = new LoadRateUtil();
            List<LineResultEntity> lineList1 = new ArrayList<>();
            // 取库中各线路的额定电流
            String lineRatedName = "capacity_line_dev141";
            String nodeRatedName = "capacity_node_dev141";
            Float[] ratedCurrents = processDao.getLineRatedCurrent(lineRatedName);
            for (int i = 0; i < dev141Info.getLineDutNum(); i++) {
                // 从库中取出设备1的最新的一次线路的三相电流、有功无功等数据
                LineResultEntity lineResultEntity = processDao.selectLineData(AllGVar.DEV141_Tab_Name.get(i));
                lineList1.add(lineResultEntity);
            }
            // 执行线路负载率的计算,把节点的编号一起传过去处理
            List<LoadRateEntity> lineLoadRatio = loadRateUtil.lineLoadRate(times,
                    AllGVar.DEV141_LINE_INFO, lineList1, ratedCurrents);
            // 更新dev1中线路负载率
            processDao.updateLineLoadRatio(lineLoadRatio);
            // 执行插入线路负载率历史记录表
            processDao.insertLineLoadRatio(lineLoadRatio);
            /* 变电站负载率=UI/额定容量 */
            // 取读取数据中的UI值
            Float[] uis = resultListsEntity.getNodeUI();
            // 取库中的变电站的额定容量
            Float[] nodeRatedCapacity = processDao.getNodeRatedCapacity(nodeRatedName);
            List<LoadRateEntity> nodeLoadRatio = loadRateUtil.nodeLoadRate(times,
                    preNodeInfos, uis, nodeRatedCapacity);
            // 更新库中的变电站负载率
            processDao.updateNodeLoadRatio(nodeLoadRatio);
            // 执行插入变电站负载率历史记录表
            processDao.insertNodeLoadRatio(nodeLoadRatio);
            /* 查询一次所有的负载率，发送给前端 */
            ListsResEntity listsResEntity1 = dataReadServiceImpl.loadRateRead();
            webSocketService.sendTextMsg("/response/getLoadRate", listsResEntity1);
            System.out.println("丽衢01读取结束");
        } else {
            System.out.println("设备数据为空");
        }
    }

    @Override
    // @Async("task2")
    @Transactional
    public void dev142DataProcess() {
        Dev142Entity dev142Info = new Dev142Entity(); //修改实体 dev0162Info
        DevCommonEntity devCommonEntity = new DevCommonEntity();
        BeanUtils.copyProperties(dev142Info, devCommonEntity);
        ResultListsEntity resultListsEntity = new AdsReadUtil().devStructRead(devCommonEntity);
        int size = resultListsEntity.getLineResultEntityList().size();
        if (size > 0) {
            /* 时间轴,获取时间并入库*/
            String[] times = new String[1];
            int id = 28;
            int countDev = processDao.getReadStatus(id);
            DateUtils dateUtils = new DateUtils();
            times[0] = dateUtils.timelineUtil(countDev + 1);
            processDao.updateReadStatus(id);
            List<LineResultEntity> lineList = resultListsEntity.getLineResultEntityList();
            List<NodeResultEntity> nodeList = resultListsEntity.getNodeResultEntityList();
            /* 前后端通讯：对lineList和nodeList做一下拆分 */
            // 处理设备1的线路电流与功率数据
            List<PreLineInfoEntity> preLineInfos = AllGVar.DEV142_LINE_INFO;
            List<PreNodeInfoEntity> preNodeInfos = AllGVar.DEV142_NODE_INFO;
            for (int i = 0; i < dev142Info.getLineDutNum(); i++) {
                // 放入线路的编号，如zh001
                if (i != dev142Info.getLineDutNum() - 1) {
                    for (int j = 0; j < 15; j++) {
                        ListsResEntity listsResEntity = new ListsResEntity();
                        listsResEntity.setNodeNum(preLineInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        lineDataProcessor(lineList, i, j, listsResEntity);
                    }
                } else {
                    // 最后一个表数据不满
                    int end = dev142Info.getLineNum() % 15;
                    for (int j = 0; j < end; j++) {
                        ListsResEntity listsResEntity = new ListsResEntity();
                        listsResEntity.setNodeNum(preLineInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        lineDataProcessor(lineList, i, j, listsResEntity);
                    }
                }
            }
            // 处理设备1的节点电压数据
            for (int i = 0; i < dev142Info.getNodeDutNum(); i++) {
                if (i != dev142Info.getNodeDutNum() - 1) {
                    for (int j = 0; j < 15; j++) {
                        // 返回list放入节点的编号，如zh001
                        ListsResEntity listsResEntity = new ListsResEntity();
                        // 编号位置从设备1的线路编号最后一个的位置开始
                        listsResEntity.setNodeNum(preNodeInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        nodeDataProcessor(nodeList, i, j, listsResEntity);
                    }
                } else {
                    int end = dev142Info.getNodeNum() % 15;
                    for (int j = 0; j < end; j++) {
                        // 返回list放入节点的编号，如zh001
                        ListsResEntity listsResEntity = new ListsResEntity();
                        // 编号位置从设备1的线路编号最后一个的位置开始
                        listsResEntity.setNodeNum(preNodeInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        nodeDataProcessor(nodeList, i, j, listsResEntity);
                    }
                }
            }
            /* 执行数据插入 */
            for (int i = 0; i < dev142Info.getAllDutNum(); i++) {
                if (i < dev142Info.getLineDutNum()) {
                    processDao.insertLineResult(AllGVar.DEV142_Tab_Name.get(i), lineList.get(i));
                } else {
                    processDao.insertNodeResult(AllGVar.DEV142_Tab_Name.get(i), nodeList.get(i - dev142Info.getLineDutNum()));
                }
            }
            /* 线路负载率=单相电流/额定电流 */
            LoadRateUtil loadRateUtil = new LoadRateUtil();
            List<LineResultEntity> lineList1 = new ArrayList<>();
            // 取库中各线路的额定电流
            String lineRatedName = "capacity_line_dev142";
            String nodeRatedName = "capacity_node_dev142";
            Float[] ratedCurrents = processDao.getLineRatedCurrent(lineRatedName);
            for (int i = 0; i < dev142Info.getLineDutNum(); i++) {
                // 从库中取出设备1的最新的一次线路的三相电流、有功无功等数据
                LineResultEntity lineResultEntity = processDao.selectLineData(AllGVar.DEV142_Tab_Name.get(i));
                lineList1.add(lineResultEntity);
            }
            // 执行线路负载率的计算,把节点的编号一起传过去处理
            List<LoadRateEntity> lineLoadRatio = loadRateUtil.lineLoadRate(times,
                    AllGVar.DEV142_LINE_INFO, lineList1, ratedCurrents);
            // 更新dev1中线路负载率
            processDao.updateLineLoadRatio(lineLoadRatio);
            // 执行插入线路负载率历史记录表
            processDao.insertLineLoadRatio(lineLoadRatio);
            /* 变电站负载率=UI/额定容量 */
            // 取读取数据中的UI值
            Float[] uis = resultListsEntity.getNodeUI();
            // 取库中的变电站的额定容量
            Float[] nodeRatedCapacity = processDao.getNodeRatedCapacity(nodeRatedName);
            List<LoadRateEntity> nodeLoadRatio = loadRateUtil.nodeLoadRate(times,
                    preNodeInfos, uis, nodeRatedCapacity);
            // 更新库中的变电站负载率
            processDao.updateNodeLoadRatio(nodeLoadRatio);
            // 执行插入变电站负载率历史记录表
            processDao.insertNodeLoadRatio(nodeLoadRatio);
            /* 查询一次所有的负载率，发送给前端 */
            System.out.println("丽衢02读取结束");
        } else {
            System.out.println("设备数据为空");
        }
    }

    @Override
    public void dev151DataProcess() {
        Dev151Entity dev151Info = new Dev151Entity(); //修改实体 dev0162Info
        DevCommonEntity devCommonEntity = new DevCommonEntity();
        BeanUtils.copyProperties(dev151Info, devCommonEntity);
        ResultListsEntity resultListsEntity = new AdsReadUtil().devStructRead(devCommonEntity);
        int size = resultListsEntity.getLineResultEntityList().size();
        if (size > 0) {
            /* 时间轴,获取时间并入库*/
            String[] times = new String[1];
            int id = 28;
            int countDev = processDao.getReadStatus(id);
            DateUtils dateUtils = new DateUtils();
            times[0] = dateUtils.timelineUtil(countDev + 1);
            processDao.updateReadStatus(id);
            List<LineResultEntity> lineList = resultListsEntity.getLineResultEntityList();
            List<NodeResultEntity> nodeList = resultListsEntity.getNodeResultEntityList();
            /* 前后端通讯：对lineList和nodeList做一下拆分 */
            // 处理设备1的线路电流与功率数据
            List<PreLineInfoEntity> preLineInfos = AllGVar.DEV151_LINE_INFO;
            List<PreNodeInfoEntity> preNodeInfos = AllGVar.DEV151_NODE_INFO;
            for (int i = 0; i < dev151Info.getLineDutNum(); i++) {
                // 放入线路的编号，如zh001
                if (i != dev151Info.getLineDutNum() - 1) {
                    for (int j = 0; j < 15; j++) {
                        ListsResEntity listsResEntity = new ListsResEntity();
                        listsResEntity.setNodeNum(preLineInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        lineDataProcessor(lineList, i, j, listsResEntity);
                    }
                } else {
                    // 最后一个表数据不满
                    int end = dev151Info.getLineNum() % 15;
                    for (int j = 0; j < end; j++) {
                        ListsResEntity listsResEntity = new ListsResEntity();
                        listsResEntity.setNodeNum(preLineInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        lineDataProcessor(lineList, i, j, listsResEntity);
                    }
                }
            }
            // 处理设备1的节点电压数据
            for (int i = 0; i < dev151Info.getNodeDutNum(); i++) {
                if (i != dev151Info.getNodeDutNum() - 1) {
                    for (int j = 0; j < 15; j++) {
                        // 返回list放入节点的编号，如zh001
                        ListsResEntity listsResEntity = new ListsResEntity();
                        // 编号位置从设备1的线路编号最后一个的位置开始
                        listsResEntity.setNodeNum(preNodeInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        nodeDataProcessor(nodeList, i, j, listsResEntity);
                    }
                } else {
                    int end = dev151Info.getNodeNum() % 15;
                    for (int j = 0; j < end; j++) {
                        // 返回list放入节点的编号，如zh001
                        ListsResEntity listsResEntity = new ListsResEntity();
                        // 编号位置从设备1的线路编号最后一个的位置开始
                        listsResEntity.setNodeNum(preNodeInfos.get(i*15+j).getNodeNum());
                        listsResEntity.setTimes(times);
                        nodeDataProcessor(nodeList, i, j, listsResEntity);
                    }
                }
            }
            /* 执行数据插入 */
            for (int i = 0; i < dev151Info.getAllDutNum(); i++) {
                if (i < dev151Info.getLineDutNum()) {
                    processDao.insertLineResult(AllGVar.DEV151_Tab_Name.get(i), lineList.get(i));
                } else {
                    processDao.insertNodeResult(AllGVar.DEV151_Tab_Name.get(i), nodeList.get(i - dev151Info.getLineDutNum()));
                }
            }
            /* 线路负载率=单相电流/额定电流 */
            LoadRateUtil loadRateUtil = new LoadRateUtil();
            List<LineResultEntity> lineList1 = new ArrayList<>();
            // 取库中各线路的额定电流
            String lineRatedName = "capacity_line_dev151";
            String nodeRatedName = "capacity_node_dev151";
            Float[] ratedCurrents = processDao.getLineRatedCurrent(lineRatedName);
            for (int i = 0; i < dev151Info.getLineDutNum(); i++) {
                // 从库中取出设备1的最新的一次线路的三相电流、有功无功等数据
                LineResultEntity lineResultEntity = processDao.selectLineData(AllGVar.DEV151_Tab_Name.get(i));
                lineList1.add(lineResultEntity);
            }
            // 执行线路负载率的计算,把节点的编号一起传过去处理
            List<LoadRateEntity> lineLoadRatio = loadRateUtil.lineLoadRate(times,
                    AllGVar.DEV151_LINE_INFO, lineList1, ratedCurrents);
            // 更新dev1中线路负载率
            processDao.updateLineLoadRatio(lineLoadRatio);
            // 执行插入线路负载率历史记录表
            processDao.insertLineLoadRatio(lineLoadRatio);
            /* 变电站负载率=UI/额定容量 */
            // 取读取数据中的UI值
            Float[] uis = resultListsEntity.getNodeUI();
            // 取库中的变电站的额定容量
            Float[] nodeRatedCapacity = processDao.getNodeRatedCapacity(nodeRatedName);
            List<LoadRateEntity> nodeLoadRatio = loadRateUtil.nodeLoadRate(times,
                    preNodeInfos, uis, nodeRatedCapacity);
            // 更新库中的变电站负载率
            processDao.updateNodeLoadRatio(nodeLoadRatio);
            // 执行插入变电站负载率历史记录表
            processDao.insertNodeLoadRatio(nodeLoadRatio);
            /* 查询一次所有的负载率，发送给前端 */
            System.out.println("特高压01读取结束");
        } else {
            System.out.println("设备数据为空");
        }
    }


    /**
     * 节点的数据处理与解析方法，每一个节点的数据分别传给前端
     *
     * @param nodeList
     * @param i
     * @param j
     * @param listsResEntity
     */
    private void nodeDataProcessor(List<NodeResultEntity> nodeList, int i, int j, ListsResEntity listsResEntity) {
        if (nodeList != null) {
            listsResEntity.setStatus(ResStatus.SUCCESS);
            List<SsVolResEntity> ssVolResEntityList = new ArrayList<>();
            SsVolResEntity ssVolResEntity = new SsVolResEntity();
            try {
                Field[] fields = NodeResultEntity.class.getDeclaredFields();
                for (Field field : fields) {
                    field.setAccessible(true);
                }
                ssVolResEntity.setVoltageA((Float) fields[j].get(nodeList.get(i)));
                ssVolResEntityList.add(ssVolResEntity);
                listsResEntity.setSsVolResEntityList(ssVolResEntityList);
                webSocketService.sendTextMsg("/response/nodeResult", listsResEntity);
            } catch (Exception e) {
                e.printStackTrace();
            }
            // 以下内容可以改为映射，循环赋值
        } else {
            listsResEntity.setStatus(ResStatus.FAILED);
        }
    }

    /**
     * 线路的数据处理与解析方法，每一个线路的数据分别传给前端
     *
     * @param lineList
     * @param i
     * @param j
     * @param listsResEntity
     */
    private void lineDataProcessor(List<LineResultEntity> lineList, int i, int j, ListsResEntity listsResEntity) {
        if (lineList != null) {
            listsResEntity.setStatus(ResStatus.SUCCESS);
            List<LineCtAndPResEntity> lineCtAndPResEntityList = new ArrayList<>();
            LineCtAndPResEntity lineCtAndPResEntity = new LineCtAndPResEntity();
            // 通过反射机制取到LineResultEntity中所有属性的值，并且赋值给lineCtAndPResEntity
            try {
                Field[] fields = LineResultEntity.class.getDeclaredFields();
                for (Field field : fields) {
                    field.setAccessible(true);
                }
                lineCtAndPResEntity.setCurrentA((Float) fields[j * 3].get(lineList.get(i)));
                lineCtAndPResEntity.setPowerActive((Float) fields[j * 3 + 1].get(lineList.get(i)));
                lineCtAndPResEntity.setPowerReactive((Float) fields[j + 3 + 1].get(lineList.get(i)));
                lineCtAndPResEntityList.add(lineCtAndPResEntity);
                listsResEntity.setLineCtAndPResEntityList(lineCtAndPResEntityList);
                webSocketService.sendTextMsg("/response/lineResult", listsResEntity);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            listsResEntity.setStatus(ResStatus.FAILED);
        }
    }
}
