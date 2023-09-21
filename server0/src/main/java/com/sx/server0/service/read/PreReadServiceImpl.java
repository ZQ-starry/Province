package com.sx.server0.service.read;

import com.sx.server0.component.AllGVar;
import com.sx.server0.dao.read.PreReadDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @Author: ZhangQi
 * @Date: 2023/9/3 9:36
 * @Description:
 */
@Service
public class PreReadServiceImpl implements PreReadService {

    @Autowired
    private PreReadDao preReadDao;

    @Override
    public void getPreInfoForRead() {
        AllGVar.DEV011_Tab_Name = preReadDao.getTableNames("table_name_011");
        AllGVar.DEV012_Tab_Name = preReadDao.getTableNames("table_name_012");
        AllGVar.DEV021_Tab_Name = preReadDao.getTableNames("table_name_021");
        AllGVar.DEV022_Tab_Name = preReadDao.getTableNames("table_name_022");
        AllGVar.DEV031_Tab_Name = preReadDao.getTableNames("table_name_031");
        AllGVar.DEV032_Tab_Name = preReadDao.getTableNames("table_name_032");
        AllGVar.DEV041_Tab_Name = preReadDao.getTableNames("table_name_041");
        AllGVar.DEV042_Tab_Name = preReadDao.getTableNames("table_name_042");
        AllGVar.DEV051_Tab_Name = preReadDao.getTableNames("table_name_051");
        AllGVar.DEV052_Tab_Name = preReadDao.getTableNames("table_name_052");
        AllGVar.DEV061_Tab_Name = preReadDao.getTableNames("table_name_061");
        AllGVar.DEV062_Tab_Name = preReadDao.getTableNames("table_name_062");
        AllGVar.DEV071_Tab_Name = preReadDao.getTableNames("table_name_071");
        AllGVar.DEV072_Tab_Name = preReadDao.getTableNames("table_name_072");
        AllGVar.DEV081_Tab_Name = preReadDao.getTableNames("table_name_081");
        AllGVar.DEV082_Tab_Name = preReadDao.getTableNames("table_name_082");
        AllGVar.DEV091_Tab_Name = preReadDao.getTableNames("table_name_091");
        AllGVar.DEV092_Tab_Name = preReadDao.getTableNames("table_name_092");
        AllGVar.DEV101_Tab_Name = preReadDao.getTableNames("table_name_101");
        AllGVar.DEV102_Tab_Name = preReadDao.getTableNames("table_name_102");
        AllGVar.DEV111_Tab_Name = preReadDao.getTableNames("table_name_111");
        AllGVar.DEV112_Tab_Name = preReadDao.getTableNames("table_name_112");
        AllGVar.DEV121_Tab_Name = preReadDao.getTableNames("table_name_121");
        AllGVar.DEV131_Tab_Name = preReadDao.getTableNames("table_name_131");
        AllGVar.DEV132_Tab_Name = preReadDao.getTableNames("table_name_132");
        AllGVar.DEV141_Tab_Name = preReadDao.getTableNames("table_name_141");
        AllGVar.DEV142_Tab_Name = preReadDao.getTableNames("table_name_142");
        AllGVar.DEV151_Tab_Name = preReadDao.getTableNames("table_name_151");
        // 查询线路的编号
        String nodeVariable1 = "线路";
        AllGVar.DEV011_LINE_INFO = preReadDao.getLineInfo("node_info_011", nodeVariable1);
        AllGVar.DEV012_LINE_INFO = preReadDao.getLineInfo("node_info_012", nodeVariable1);
        AllGVar.DEV021_LINE_INFO = preReadDao.getLineInfo("node_info_021", nodeVariable1);
        AllGVar.DEV022_LINE_INFO = preReadDao.getLineInfo("node_info_022", nodeVariable1);
        AllGVar.DEV031_LINE_INFO = preReadDao.getLineInfo("node_info_031", nodeVariable1);
        AllGVar.DEV032_LINE_INFO = preReadDao.getLineInfo("node_info_032", nodeVariable1);
        AllGVar.DEV041_LINE_INFO = preReadDao.getLineInfo("node_info_041", nodeVariable1);
        AllGVar.DEV042_LINE_INFO = preReadDao.getLineInfo("node_info_042", nodeVariable1);
        AllGVar.DEV051_LINE_INFO = preReadDao.getLineInfo("node_info_051", nodeVariable1);
        AllGVar.DEV052_LINE_INFO = preReadDao.getLineInfo("node_info_052", nodeVariable1);
        AllGVar.DEV061_LINE_INFO = preReadDao.getLineInfo("node_info_061", nodeVariable1);
        AllGVar.DEV062_LINE_INFO = preReadDao.getLineInfo("node_info_062", nodeVariable1);
        AllGVar.DEV071_LINE_INFO = preReadDao.getLineInfo("node_info_071", nodeVariable1);
        AllGVar.DEV072_LINE_INFO = preReadDao.getLineInfo("node_info_072", nodeVariable1);
        AllGVar.DEV081_LINE_INFO = preReadDao.getLineInfo("node_info_081", nodeVariable1);
        AllGVar.DEV082_LINE_INFO = preReadDao.getLineInfo("node_info_082", nodeVariable1);
        AllGVar.DEV091_LINE_INFO = preReadDao.getLineInfo("node_info_091", nodeVariable1);
        AllGVar.DEV092_LINE_INFO = preReadDao.getLineInfo("node_info_092", nodeVariable1);
        AllGVar.DEV101_LINE_INFO = preReadDao.getLineInfo("node_info_101", nodeVariable1);
        AllGVar.DEV102_LINE_INFO = preReadDao.getLineInfo("node_info_102", nodeVariable1);
        AllGVar.DEV111_LINE_INFO = preReadDao.getLineInfo("node_info_111", nodeVariable1);
        AllGVar.DEV112_LINE_INFO = preReadDao.getLineInfo("node_info_112", nodeVariable1);
        AllGVar.DEV121_LINE_INFO = preReadDao.getLineInfo("node_info_121", nodeVariable1);
        AllGVar.DEV131_LINE_INFO = preReadDao.getLineInfo("node_info_131", nodeVariable1);
        AllGVar.DEV132_LINE_INFO = preReadDao.getLineInfo("node_info_132", nodeVariable1);
        AllGVar.DEV141_LINE_INFO = preReadDao.getLineInfo("node_info_141", nodeVariable1);
        AllGVar.DEV142_LINE_INFO = preReadDao.getLineInfo("node_info_142", nodeVariable1);
        AllGVar.DEV151_LINE_INFO = preReadDao.getLineInfo("node_info_151", nodeVariable1);

        AllGVar.DEV011_NODE_INFO = preReadDao.getNodeInfo("node_info_011", nodeVariable1);
        AllGVar.DEV012_NODE_INFO = preReadDao.getNodeInfo("node_info_012", nodeVariable1);
        AllGVar.DEV021_NODE_INFO = preReadDao.getNodeInfo("node_info_021", nodeVariable1);
        AllGVar.DEV022_NODE_INFO = preReadDao.getNodeInfo("node_info_022", nodeVariable1);
        AllGVar.DEV031_NODE_INFO = preReadDao.getNodeInfo("node_info_031", nodeVariable1);
        AllGVar.DEV032_NODE_INFO = preReadDao.getNodeInfo("node_info_032", nodeVariable1);
        AllGVar.DEV041_NODE_INFO = preReadDao.getNodeInfo("node_info_041", nodeVariable1);
        AllGVar.DEV042_NODE_INFO = preReadDao.getNodeInfo("node_info_042", nodeVariable1);
        AllGVar.DEV051_NODE_INFO = preReadDao.getNodeInfo("node_info_051", nodeVariable1);
        AllGVar.DEV052_NODE_INFO = preReadDao.getNodeInfo("node_info_052", nodeVariable1);
        AllGVar.DEV061_NODE_INFO = preReadDao.getNodeInfo("node_info_061", nodeVariable1);
        AllGVar.DEV062_NODE_INFO = preReadDao.getNodeInfo("node_info_062", nodeVariable1);
        AllGVar.DEV071_NODE_INFO = preReadDao.getNodeInfo("node_info_071", nodeVariable1);
        AllGVar.DEV072_NODE_INFO = preReadDao.getNodeInfo("node_info_072", nodeVariable1);
        AllGVar.DEV081_NODE_INFO = preReadDao.getNodeInfo("node_info_081", nodeVariable1);
        AllGVar.DEV082_NODE_INFO = preReadDao.getNodeInfo("node_info_082", nodeVariable1);
        AllGVar.DEV091_NODE_INFO = preReadDao.getNodeInfo("node_info_091", nodeVariable1);
        AllGVar.DEV092_NODE_INFO = preReadDao.getNodeInfo("node_info_092", nodeVariable1);
        AllGVar.DEV101_NODE_INFO = preReadDao.getNodeInfo("node_info_101", nodeVariable1);
        AllGVar.DEV102_NODE_INFO = preReadDao.getNodeInfo("node_info_102", nodeVariable1);
        AllGVar.DEV111_NODE_INFO = preReadDao.getNodeInfo("node_info_111", nodeVariable1);
        AllGVar.DEV112_NODE_INFO = preReadDao.getNodeInfo("node_info_112", nodeVariable1);
        AllGVar.DEV121_NODE_INFO = preReadDao.getNodeInfo("node_info_121", nodeVariable1);
        AllGVar.DEV131_NODE_INFO = preReadDao.getNodeInfo("node_info_131", nodeVariable1);
        AllGVar.DEV132_NODE_INFO = preReadDao.getNodeInfo("node_info_132", nodeVariable1);
        AllGVar.DEV141_NODE_INFO = preReadDao.getNodeInfo("node_info_141", nodeVariable1);
        AllGVar.DEV142_NODE_INFO = preReadDao.getNodeInfo("node_info_142", nodeVariable1);
        AllGVar.DEV151_NODE_INFO = preReadDao.getNodeInfo("node_info_151", nodeVariable1);
        System.out.println("---------------基础数据准备完毕------------------");

        // 截断所有结果表
        // for (String tableName:tableNames){
        //     processDao.truncateTable(tableName);
        // }
    }
}
