package com.sx.server0.service.read;


import com.alibaba.fastjson.JSONObject;
import com.sx.server0.entity.common.BaseRes;
import com.sx.server0.entity.data.FaultsStandardEntity;
import com.sx.server0.entity.req.LineReqEntity;
import com.sx.server0.entity.req.NodeInfoReqEntity;
import com.sx.server0.entity.req.StationReqEntity;
import com.sx.server0.entity.res.FaultsListResEntity;
import com.sx.server0.entity.res.ListsResEntity;
import com.sx.server0.entity.res.ResMonitorEntity;
import com.sx.server0.entity.result.GridInfoRes;


/**
 * @Author: ZhangQi
 * @Date: 2023/7/21 10:48
 * @Description:
 */
public interface DataReadService {

    ListsResEntity ssVoltageRead(NodeInfoReqEntity nodeInfoReqEntity);

    ListsResEntity lineCtAndPRead(NodeInfoReqEntity nodeInfoReqEntity);

    ListsResEntity loadRateRead();

    FaultsListResEntity faultsRead();

    ResMonitorEntity volAndFreqRead();

    ListsResEntity nodeInfoQueryByStation(StationReqEntity stationReqEntity);

    ListsResEntity nodeInfoQueryByLine(LineReqEntity lineReqEntity);

    GridInfoRes getGridInfo();

    ListsResEntity nodeInfoQueryByStation1(JSONObject nameInfo);

    ListsResEntity nodeInfoQueryByLine1(JSONObject nameInfo);

    ListsResEntity setFaultsConfig(FaultsStandardEntity faultsStandardEntity);

    ListsResEntity getFaultsConfig();
}
