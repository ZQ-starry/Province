package com.sx.server0.entity.result;

import com.sx.server0.entity.common.BaseRes;
import lombok.Data;

import java.util.List;

/**
 * @Author: ZhangQi
 * @Date: 2023/9/18 8:57
 * @Description:
 */
@Data
public class GridInfoRes extends BaseRes {
    private List<GridInfoEntity> gridCapacity;
    private List<GridInfoEntity> installCapacity;
}
