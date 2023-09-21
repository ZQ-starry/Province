package com.sx.server0.entity.req;

import lombok.Data;

import java.util.List;

/**
 * @Author: ZhangQi
 * @Date: 2023/8/31 9:40
 * @Description: 站点的条件查询通讯接收实体类
 */
@Data
public class StationReqEntity {
    private List<String> city; //城市
    private List<String> nodeVariable;//类型
    private List<Integer> nodeVolLevel;//电压等级
}
