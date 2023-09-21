package com.sx.server0.entity.req;

import lombok.Data;

import java.util.List;

/**
 * @Author: ZhangQi
 * @Date: 2023/9/1 8:56
 * @Description:
 */
@Data
public class LineReqEntity {
    private List<String> city;  //城市
    private List<Integer> nodeVolLevel; //电压等级
    private String nodeNameStart; //首端名称
    private String nodeNameEnd; //末端名称
}
