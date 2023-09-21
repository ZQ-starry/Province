package com.sx.server0.entity.req;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @Author: ZhangQi
 * @Date: 2023/7/24 9:31
 * @Description: 查询节点信息的请求体接收实体类
 */
@Data
public class NodeInfoReqEntity {
    @ApiModelProperty(name = "nodeNum",value = "节点编号",example = "za001")
    private String nodeNum;
}
