package com.sx.server0.entity.req;

import com.sx.server0.entity.data.TabAndColEntity;
import lombok.Data;

/**
 * @Author: ZhangQi
 * @Date: 2023/7/21 11:02
 * @Description: 节点信息实体类
 */
@Data
public class NodeInfoEntity extends TabAndColEntity {
    private Integer id;
    // 节点所在城市
    private String city;
    // 节点名称
    private String nodeNameStart;

    private String nodeNameEnd;
    // 节点类型
    private String nodeVariable;
    // 节点电压等级
    private Integer nodeVolLevel;
    // 节点编号
    private String nodeNum;
}
