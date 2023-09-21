package com.sx.server0.entity.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * @Author: ZhangQi
 * @Date: 2023/7/24 11:04
 * @Description:
 */
@Data
public class TabAndColEntity {
    // 节点计算结果所在表的表名
    @JsonInclude(JsonInclude.Include.NON_NULL)
    protected String nodeTableName;
    // 节点计算结果在表中的字段编号
    @JsonInclude(JsonInclude.Include.NON_NULL)
    protected String nodeColumnNum;
    // 节点计算结果在表中的字段名
    @JsonInclude(JsonInclude.Include.NON_NULL)
    protected String nodeColumnName1;
    // 节点计算结果在表中的字段名
    @JsonInclude(JsonInclude.Include.NON_NULL)
    protected String nodeColumnName2;
    // 节点计算结果在表中的字段名
    @JsonInclude(JsonInclude.Include.NON_NULL)
    protected String nodeColumnName3;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    protected Integer orderNum;
}
