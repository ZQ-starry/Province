package com.sx.server0.entity.res;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sx.server0.entity.common.BaseRes;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * @Author: ZhangQi
 * @Date: 2023/9/12 14:28
 * @Description:
 */
@Data
public class ResMonitorEntity extends BaseRes {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @ApiModelProperty(name = "volMonitorEntityList1000", value = "电压监测-1000")
    private List<VolMonitorEntity> volMonitorEntityList1000;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @ApiModelProperty(name = "volMonitorEntityList500", value = "电压监测-500")
    private List<VolMonitorEntity> volMonitorEntityList500;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @ApiModelProperty(name = "volMonitorEntityList220", value = "电压监测-220")
    private List<VolMonitorEntity> volMonitorEntityList220;


    @JsonInclude(JsonInclude.Include.NON_NULL)
    @ApiModelProperty(name = "freqMonitorEntityList", value = "电网频率监测")
    private List<FreqMonitorEntity> freqMonitorEntityList;
}