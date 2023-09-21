package com.sx.server0.entity.res;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sx.server0.entity.common.BaseRes;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class FaultsListResEntity extends BaseRes {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @ApiModelProperty(name = "nodeLoadRateFaults", value = "变电站负载率异常")
    private List<LoadRateFaultEntity> nodeLoadRateFaults;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @ApiModelProperty(name = "lineLoadRateFaults", value = "线路负载率异常")
    private List<LoadRateFaultEntity> lineLoadRateFaults;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @ApiModelProperty(name = "volFaults1000", value = "站点电压越限异常")
    private List<VolMonitorEntity> volFaults;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @ApiModelProperty(name = "freqFaults", value = "电网频率异常")
    private List<FreqMonitorEntity> freqFaults;
}
