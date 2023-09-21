package com.sx.server0.component;

import com.sx.server0.entity.data.PreLineInfoEntity;
import com.sx.server0.entity.data.PreNodeInfoEntity;
import com.sx.server0.service.read.PreReadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author: ZhangQi
 * @Date: 2023/8/24 11:00
 * @Description: 关于地市信息的一些全局变量的定义
 */
@Component
public class AllGVar {

    @Autowired
    private PreReadService preReadService;


    public static final int LINE_EN_LEN = 180; // 线路结果的实体类长度 4*3*15
    public static final int NODE_EN_LEN = 60; // 站点结果的结构体长度 4*1*15

    /* 一个设备中的数据结果表的表名*/
    public static List<String> DEV011_Tab_Name = new ArrayList<>(11);
    public static List<String> DEV012_Tab_Name = new ArrayList<>(11);
    public static List<String> DEV021_Tab_Name = new ArrayList<>(11);
    public static List<String> DEV022_Tab_Name = new ArrayList<>(11);
    public static List<String> DEV031_Tab_Name = new ArrayList<>(11);
    public static List<String> DEV032_Tab_Name = new ArrayList<>(11);
    public static List<String> DEV041_Tab_Name = new ArrayList<>(11);
    public static List<String> DEV042_Tab_Name = new ArrayList<>(11);
    public static List<String> DEV051_Tab_Name = new ArrayList<>(11);
    public static List<String> DEV052_Tab_Name = new ArrayList<>(11);
    public static List<String> DEV061_Tab_Name = new ArrayList<>(11);
    public static List<String> DEV062_Tab_Name = new ArrayList<>(11);
    public static List<String> DEV071_Tab_Name = new ArrayList<>(11);
    public static List<String> DEV072_Tab_Name = new ArrayList<>(11);
    public static List<String> DEV081_Tab_Name = new ArrayList<>(11);
    public static List<String> DEV082_Tab_Name = new ArrayList<>(11);
    public static List<String> DEV091_Tab_Name = new ArrayList<>(11);
    public static List<String> DEV092_Tab_Name = new ArrayList<>(11);
    public static List<String> DEV101_Tab_Name = new ArrayList<>(11);
    public static List<String> DEV102_Tab_Name = new ArrayList<>(11);
    public static List<String> DEV111_Tab_Name = new ArrayList<>(11);
    public static List<String> DEV112_Tab_Name = new ArrayList<>(11);
    public static List<String> DEV121_Tab_Name = new ArrayList<>(11);
    public static List<String> DEV131_Tab_Name = new ArrayList<>(11);
    public static List<String> DEV132_Tab_Name = new ArrayList<>(11);
    public static List<String> DEV141_Tab_Name = new ArrayList<>(11);
    public static List<String> DEV142_Tab_Name = new ArrayList<>(11);
    public static List<String> DEV151_Tab_Name = new ArrayList<>(11);
    /* 一个设备中的线路对应的编号node_num */
    // public static String[] DEV011_LINE_NUM = new String[46];
    // public static String[] DEV012_LINE_NUM = new String[12];
    // public static String[] DEV021_LINE_NUM = new String[64];
    // public static String[] DEV022_LINE_NUM = new String[14];
    // public static String[] DEV031_LINE_NUM = new String[65];
    // public static String[] DEV032_LINE_NUM = new String[66];
    // public static String[] DEV041_LINE_NUM = new String[21];
    // public static String[] DEV042_LINE_NUM = new String[21];
    // public static String[] DEV051_LINE_NUM = new String[64];
    // public static String[] DEV052_LINE_NUM = new String[39];
    // public static String[] DEV061_LINE_NUM = new String[60];
    // public static String[] DEV062_LINE_NUM = new String[63];
    // public static String[] DEV071_LINE_NUM = new String[49];
    // public static String[] DEV072_LINE_NUM = new String[48];
    // public static String[] DEV081_LINE_NUM = new String[27];
    // public static String[] DEV082_LINE_NUM = new String[37];
    // public static String[] DEV091_LINE_NUM = new String[37];
    // public static String[] DEV092_LINE_NUM = new String[22];
    // public static String[] DEV101_LINE_NUM = new String[32];
    // public static String[] DEV102_LINE_NUM = new String[37];
    // public static String[] DEV111_LINE_NUM = new String[40];
    // public static String[] DEV112_LINE_NUM = new String[19];
    // public static String[] DEV121_LINE_NUM = new String[35];
    // public static String[] DEV131_LINE_NUM = new String[42];
    // public static String[] DEV132_LINE_NUM = new String[66];
    // public static String[] DEV141_LINE_NUM = new String[83];
    // public static String[] DEV142_LINE_NUM = new String[38];

    /* 一个设备中的站点对应的编号node_num */
    // public static String[] DEV011_NODE_NUM = new String[37];
    // public static String[] DEV012_NODE_NUM = new String[9];
    // public static String[] DEV021_NODE_NUM = new String[47];
    // public static String[] DEV022_NODE_NUM = new String[11];
    // public static String[] DEV031_NODE_NUM = new String[48];
    // public static String[] DEV032_NODE_NUM = new String[48];
    // public static String[] DEV041_NODE_NUM = new String[17];
    // public static String[] DEV042_NODE_NUM = new String[17];
    // public static String[] DEV051_NODE_NUM = new String[45];
    // public static String[] DEV052_NODE_NUM = new String[43];
    // public static String[] DEV061_NODE_NUM = new String[30];
    // public static String[] DEV062_NODE_NUM = new String[39];
    // public static String[] DEV071_NODE_NUM = new String[49];
    // public static String[] DEV072_NODE_NUM = new String[41];
    // public static String[] DEV081_NODE_NUM = new String[35];
    // public static String[] DEV082_NODE_NUM = new String[12];
    // public static String[] DEV091_NODE_NUM = new String[26];
    // public static String[] DEV092_NODE_NUM = new String[15];
    // public static String[] DEV101_NODE_NUM = new String[25];
    // public static String[] DEV102_NODE_NUM = new String[23];
    // public static String[] DEV111_NODE_NUM = new String[27];
    // public static String[] DEV112_NODE_NUM = new String[13];
    // public static String[] DEV121_NODE_NUM = new String[27];
    // public static String[] DEV131_NODE_NUM = new String[32];
    // public static String[] DEV132_NODE_NUM = new String[51];
    // public static String[] DEV141_NODE_NUM = new String[75];
    // public static String[] DEV142_NODE_NUM = new String[35];

    public static List<PreLineInfoEntity> DEV011_LINE_INFO = new ArrayList<>();
    public static List<PreLineInfoEntity> DEV012_LINE_INFO = new ArrayList<>();
    public static List<PreLineInfoEntity> DEV021_LINE_INFO = new ArrayList<>();
    public static List<PreLineInfoEntity> DEV022_LINE_INFO = new ArrayList<>();
    public static List<PreLineInfoEntity> DEV031_LINE_INFO = new ArrayList<>();
    public static List<PreLineInfoEntity> DEV032_LINE_INFO = new ArrayList<>();
    public static List<PreLineInfoEntity> DEV041_LINE_INFO = new ArrayList<>();
    public static List<PreLineInfoEntity> DEV042_LINE_INFO = new ArrayList<>();
    public static List<PreLineInfoEntity> DEV051_LINE_INFO = new ArrayList<>();
    public static List<PreLineInfoEntity> DEV052_LINE_INFO = new ArrayList<>();
    public static List<PreLineInfoEntity> DEV061_LINE_INFO = new ArrayList<>();
    public static List<PreLineInfoEntity> DEV062_LINE_INFO = new ArrayList<>();
    public static List<PreLineInfoEntity> DEV071_LINE_INFO = new ArrayList<>();
    public static List<PreLineInfoEntity> DEV072_LINE_INFO = new ArrayList<>();
    public static List<PreLineInfoEntity> DEV081_LINE_INFO = new ArrayList<>();
    public static List<PreLineInfoEntity> DEV082_LINE_INFO = new ArrayList<>();
    public static List<PreLineInfoEntity> DEV091_LINE_INFO = new ArrayList<>();
    public static List<PreLineInfoEntity> DEV092_LINE_INFO = new ArrayList<>();
    public static List<PreLineInfoEntity> DEV101_LINE_INFO = new ArrayList<>();
    public static List<PreLineInfoEntity> DEV102_LINE_INFO = new ArrayList<>();
    public static List<PreLineInfoEntity> DEV111_LINE_INFO = new ArrayList<>();
    public static List<PreLineInfoEntity> DEV112_LINE_INFO = new ArrayList<>();
    public static List<PreLineInfoEntity> DEV121_LINE_INFO = new ArrayList<>();
    public static List<PreLineInfoEntity> DEV131_LINE_INFO = new ArrayList<>();
    public static List<PreLineInfoEntity> DEV132_LINE_INFO = new ArrayList<>();
    public static List<PreLineInfoEntity> DEV141_LINE_INFO = new ArrayList<>();
    public static List<PreLineInfoEntity> DEV142_LINE_INFO = new ArrayList<>();
    public static List<PreLineInfoEntity> DEV151_LINE_INFO = new ArrayList<>();

    /* 站点的信息 */
    public static List<PreNodeInfoEntity> DEV011_NODE_INFO = new ArrayList<>();
    public static List<PreNodeInfoEntity> DEV012_NODE_INFO = new ArrayList<>();
    public static List<PreNodeInfoEntity> DEV021_NODE_INFO = new ArrayList<>();
    public static List<PreNodeInfoEntity> DEV022_NODE_INFO = new ArrayList<>();
    public static List<PreNodeInfoEntity> DEV031_NODE_INFO = new ArrayList<>();
    public static List<PreNodeInfoEntity> DEV032_NODE_INFO = new ArrayList<>();
    public static List<PreNodeInfoEntity> DEV041_NODE_INFO = new ArrayList<>();
    public static List<PreNodeInfoEntity> DEV042_NODE_INFO = new ArrayList<>();
    public static List<PreNodeInfoEntity> DEV051_NODE_INFO = new ArrayList<>();
    public static List<PreNodeInfoEntity> DEV052_NODE_INFO = new ArrayList<>();
    public static List<PreNodeInfoEntity> DEV061_NODE_INFO = new ArrayList<>();
    public static List<PreNodeInfoEntity> DEV062_NODE_INFO = new ArrayList<>();
    public static List<PreNodeInfoEntity> DEV071_NODE_INFO = new ArrayList<>();
    public static List<PreNodeInfoEntity> DEV072_NODE_INFO = new ArrayList<>();
    public static List<PreNodeInfoEntity> DEV081_NODE_INFO = new ArrayList<>();
    public static List<PreNodeInfoEntity> DEV082_NODE_INFO = new ArrayList<>();
    public static List<PreNodeInfoEntity> DEV091_NODE_INFO = new ArrayList<>();
    public static List<PreNodeInfoEntity> DEV092_NODE_INFO = new ArrayList<>();
    public static List<PreNodeInfoEntity> DEV101_NODE_INFO = new ArrayList<>();
    public static List<PreNodeInfoEntity> DEV102_NODE_INFO = new ArrayList<>();
    public static List<PreNodeInfoEntity> DEV111_NODE_INFO = new ArrayList<>();
    public static List<PreNodeInfoEntity> DEV112_NODE_INFO = new ArrayList<>();
    public static List<PreNodeInfoEntity> DEV121_NODE_INFO = new ArrayList<>();
    public static List<PreNodeInfoEntity> DEV131_NODE_INFO = new ArrayList<>();
    public static List<PreNodeInfoEntity> DEV132_NODE_INFO = new ArrayList<>();
    public static List<PreNodeInfoEntity> DEV141_NODE_INFO = new ArrayList<>();
    public static List<PreNodeInfoEntity> DEV142_NODE_INFO = new ArrayList<>();
    public static List<PreNodeInfoEntity> DEV151_NODE_INFO = new ArrayList<>();

    /*
    项目启动后执行
    1、查询结果表的表名
    2、查询各个节点对应的编号node_num
     */
    @PostConstruct
    private void preDataRead(){
        preReadService.getPreInfoForRead();
    }

}
