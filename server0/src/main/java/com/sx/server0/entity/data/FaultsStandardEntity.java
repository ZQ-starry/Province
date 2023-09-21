package com.sx.server0.entity.data;

import com.sx.server0.entity.common.BaseRes;
import lombok.Data;

/**
 * @Author: ZhangQi
 * @Date: 2023/9/12 10:53
 * @Description: 故障标准值实体
 */
@Data
public class FaultsStandardEntity {
    // 变电站负载率报警标准
    private float nodeFaultValue;
    // 线路负载率报警标准
    private float lineFaultValue;
    // 1000kv变电站电压越限上限
    private float volFaultValueMax1000;
    // 1000kv变电站电压越限下限
    private float volFaultValueMin1000;
    // 500kv变电站电压越限上限
    private float volFaultValueMax500;
    // 500kv变电站电压越限下限
    private float volFaultValueMin500;
    // 220kv变电站电压上限
    private float volFaultValueMax220;
    // 220kv变电站电压下限
    private float volFaultValueMin220;
    // 频率告警上限
    private float freqFaultValueMax;
    // 频率告警下限
    private float freqFaultValueMin;
}
