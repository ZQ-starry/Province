package com.sx.server0.dao.process;

import com.sx.server0.entity.res.LoadRateEntity;
import com.sx.server0.entity.result.LineResultEntity;
import com.sx.server0.entity.result.NodeResultEntity;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @Author: ZhangQi
 * @Date: 2023/7/31 10:38
 * @Description:
 */
@Mapper
@Repository
public interface ProcessDao {


    // 查询线路的额定电流，用于计算负载率
    Float[] getLineRatedCurrent(String lineRatedName);
    // 查询变电站的额定容量，用于计算负载率
    Float[] getNodeRatedCapacity(String nodeRatedName);
    // 线路负载率更新
    int updateLineLoadRatio(List<LoadRateEntity> updateList);
    // 变电站负载率更新
    int updateNodeLoadRatio(List<LoadRateEntity> updateList);
    // 获取线路结果表中最新的一行数据，用于计算负载率
    LineResultEntity selectLineData(String tableName);
    // ADS读取的线路结果插入表
    void insertLineResult(@Param("tableName") String name, @Param("line") LineResultEntity lineResultEntity);
    // ADS读取的节点结果插入表
    void insertNodeResult(@Param("tableName") String name, @Param("node") NodeResultEntity nodeResultEntity);
    // 时间轴的插入
    void insertTime(String time);
    // 查询各设备的数据读取次数
    int getReadStatus(int id);
    // 更新各设备的数据读取次数
    void updateReadStatus(int id);
    // 线路负载率插入历史记录表，faults_load_rate_line
    void insertLineLoadRatio(List<LoadRateEntity> lineLoadRatio);
    // 变电站负载率插入历史记录表，faults_load_rate_node
    void insertNodeLoadRatio(List<LoadRateEntity> lineLoadRatio);
    // 全网频率插入历史记录表，monitor_freq
    void insertFreq(String time, float gridFreq);
}
