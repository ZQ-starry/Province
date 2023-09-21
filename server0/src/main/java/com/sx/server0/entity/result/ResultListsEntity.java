package com.sx.server0.entity.result;

import lombok.Data;

import java.util.List;

/**
 * @Author: ZhangQi
 * @Date: 2023/7/31 10:28
 * @Description: 所有数据结果的集合实体类
 */
@Data
public class ResultListsEntity {
    private List<LineResultEntity> lineResultEntityList;
    private List<NodeResultEntity> nodeResultEntityList;
    private Float[] nodeUI;
}
