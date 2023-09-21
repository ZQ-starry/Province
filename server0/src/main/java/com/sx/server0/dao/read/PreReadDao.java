package com.sx.server0.dao.read;

import com.sx.server0.entity.data.PreLineInfoEntity;
import com.sx.server0.entity.data.PreNodeInfoEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @Author: ZhangQi
 * @Date: 2023/9/3 9:38
 * @Description:
 */
@Mapper
@Repository
public interface PreReadDao {
    // 查询各个设备的仿真数据存放表的表名
    List<String> getTableNames(String s);
    // 各设备中线路的devXXX编号查询
    String[] getNodeNumOfLine(@Param("s") String s, @Param("nodeVariable") String nodeVariable);
    // 各设备中节点的devXXX编号查询，包含变电站、电厂等非线路的节点
    String[] getNodeNumOfNode(@Param("s") String s, @Param("nodeVariable") String nodeVariable);
    // 获取线路的基础信息，包括首末端名称和编号
    List<PreLineInfoEntity> getLineInfo(@Param("s") String s, @Param("nodeVariable") String nodeVariable);
    // 获取节点的基础信息，包括首端名称、编号和电压等级
    List<PreNodeInfoEntity> getNodeInfo(@Param("s") String s, @Param("nodeVariable") String nodeVariable);
}
