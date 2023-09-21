package com.sx.server0.entity.res;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sx.server0.entity.common.BaseRes;
import com.sx.server0.entity.data.FaultsStandardEntity;
import com.sx.server0.entity.req.NodeInfoEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * @Author: ZhangQi
 * @Date: 2023/7/24 10:11
 * @Description: 给前端返回的所有数据实体
 */
@Data
public class ListsResEntity extends BaseRes {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @ApiModelProperty(name = "times", value = "时间轴")
    private String[] times;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @ApiModelProperty(name = "ssVolResEntityList", value = "查询电压时仅返回该集合")
    private List<SsVolResEntity> ssVolResEntityList;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @ApiModelProperty(name = "lineCtAndPResEntityList", value = "查询电流及功率时仅返回该集合")
    private List<LineCtAndPResEntity> lineCtAndPResEntityList;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @ApiModelProperty(name = "loadRateEntityList", value = "查询负载率时仅返回该集合")
    private List<LoadRateEntity> loadRateEntityList;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @ApiModelProperty(name = "NodeInfoEntity", value = "站点条件查询")
    private List<NodeInfoEntity> nodeInfoEntityList;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @ApiModelProperty(name = "faultsStandardEntity", value = "薄弱环节诊断配置信息")
    private FaultsStandardEntity faultsStandardEntity;

}