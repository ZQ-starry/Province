package com.sx.server0.controller.read;


import com.alibaba.fastjson.JSONObject;
import com.sx.server0.entity.data.FaultsStandardEntity;
import com.sx.server0.entity.req.LineReqEntity;
import com.sx.server0.entity.req.NodeInfoReqEntity;
import com.sx.server0.entity.req.StationReqEntity;
import com.sx.server0.entity.res.FaultsListResEntity;
import com.sx.server0.entity.res.ListsResEntity;
import com.sx.server0.entity.res.ResMonitorEntity;
import com.sx.server0.entity.result.GridInfoRes;
import com.sx.server0.service.read.DataReadService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @Author: ZhangQi
 * @Date: 2023/7/18 13:35
 * @Description: 数据查询控制层
 */
@Controller
@Api(tags = "数据查询模块")
@CrossOrigin("*")
public class DataReadController {

    @Autowired
    private DataReadService dataReadService;


    @PostMapping("/getGridInfo")
    @ResponseBody
    @ApiOperation(value = "电网信息查询")
    public GridInfoRes getGridInfo(){
        return dataReadService.getGridInfo();
    }


    /**
     * 变电站的各相的等效电压查询
     * @param nodeInfoReqEntity
     * @return
     */
    // @GetMapping("/getSsVol")
    // @ResponseBody
    // @ApiImplicitParam(required = true)
    // @ApiOperation(value = "变电站的各相的等效电压查询")
    @MessageMapping("/getSsVol")
    @SendToUser("/response/getSsVol")
    public ListsResEntity ssVoltageRead(NodeInfoReqEntity nodeInfoReqEntity){
        return dataReadService.ssVoltageRead(nodeInfoReqEntity);
    }

    /**
     * 线路的各相的等效电流查询及功率查询
     * @param nodeInfoReqEntity
     * @return
     */
    // @GetMapping("/getLineCtAndP")
    // @ResponseBody
    // @ApiImplicitParam(required = true)
    // @ApiOperation(value = "线路的各相的电流查询及功率查询")
    @MessageMapping("/getLineCtAndP")
    @SendToUser("/response/getLineCtAndP")
    public ListsResEntity lineCtAndPRead(NodeInfoReqEntity nodeInfoReqEntity){
        return dataReadService.lineCtAndPRead(nodeInfoReqEntity);
    }


    @MessageMapping("/getLoadRate")
    @SendTo("/response/getLoadRate")
    public ListsResEntity getLoadRate(){
        return dataReadService.loadRateRead();
    }

    // @PostMapping("/getFaults")
    // @ResponseBody
    // @ApiImplicitParam(required = true)
    // @ApiOperation(value = "薄弱环节诊断数据查询")
    @MessageMapping("/getFaults")
    @SendToUser("/response/getFaults")
    public FaultsListResEntity faultsRead(){
        /* 返回变电站负载率异常，线路负载率异常，电压越限，频率异常*/
       return dataReadService.faultsRead();
    }


    /**
     * 电压、频率监测
     * @return
     */
    // @PostMapping("/getVolAndFreq")
    // @ResponseBody
    // @ApiImplicitParam(required = true)
    // @ApiOperation(value = "电压、频率监测")
    @MessageMapping("/getVolAndFreq")
    @SendToUser("/response/getVolAndFreq")
    public ResMonitorEntity volAndFreqRead(){
        /*
        * 分别返回每个时间点220kv 500kv 和1000kv等级变电站的最低电压：时间点+电压值
        * 每个时间点电网的频率：时间+频率值
        */
       return dataReadService.volAndFreqRead();
    }

    /**
     * 站点信息的条件查询
     * @param stationReqEntity
     * @return
     */
    @PostMapping("/getNodeInfoStation")
    @ResponseBody
    @ApiImplicitParam(required = true)
    @ApiOperation(value = "站点的条件查询")
    public ListsResEntity nodeInfoQueryByStation(@RequestBody StationReqEntity stationReqEntity){
        return dataReadService.nodeInfoQueryByStation(stationReqEntity);
    }


    /**
     * 站点信息的精确搜索
     * @param nameInfo
     * @return
     */
    @PostMapping("/getNodeInfoStation1")
    @ResponseBody
    @ApiImplicitParam(required = true)
    @ApiOperation(value = "站点信息的精确搜索")
    public ListsResEntity nodeInfoQueryByStation1(@RequestBody JSONObject nameInfo){
        return dataReadService.nodeInfoQueryByStation1(nameInfo);
    }

    /**
     * 线路信息的精确搜索
     * @param nameInfo
     * @return
     */
    @PostMapping("/getNodeInfoLine1")
    @ResponseBody
    @ApiImplicitParam(required = true)
    @ApiOperation(value = "线路信息的精确搜索")
    public ListsResEntity nodeInfoQueryByLine1(@RequestBody JSONObject nameInfo){
        return dataReadService.nodeInfoQueryByLine1(nameInfo);
    }

    /**
     * 线路信息的条件查询
     * @param lineReqEntity
     * @return
     */
    @PostMapping("/getNodeInfoLine")
    @ResponseBody
    @ApiImplicitParam(required = true)
    @ApiOperation(value = "线路的条件查询")
    public ListsResEntity nodeInfoQueryByLine(@RequestBody LineReqEntity lineReqEntity){
        return dataReadService.nodeInfoQueryByLine(lineReqEntity);
    }


    /**
     *
     */
    @PostMapping("/getSimResult")
    @ResponseBody
    @ApiImplicitParam(required = true)
    @ApiOperation(value = "模拟仿真数据主动读取")
    public ListsResEntity simResultRead(){
        /*
         *返回数据包括：新能源出力、负荷、传统能源出力、百分比进度
         */
        ListsResEntity listsResEntity = new ListsResEntity();
        return listsResEntity;
    }


    @PostMapping("/setFaultsConfig")
    @ResponseBody
    @ApiImplicitParam(required = true)
    @ApiOperation(value = "设置故障配置信息")
    public ListsResEntity setFaultsConfig(@RequestBody FaultsStandardEntity faultsStandardEntity){
        return dataReadService.setFaultsConfig(faultsStandardEntity);
    }

    @PostMapping("/getFaultsConfig")
    @ResponseBody
    @ApiImplicitParam(required = true)
    @ApiOperation(value = "获取故障配置信息")
    public ListsResEntity getFaultsConfig(){
        return dataReadService.getFaultsConfig();
    }
}
