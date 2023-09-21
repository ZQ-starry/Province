package com.sx.server0.service.process;

import com.sx.server0.entity.common.BaseRes;
import com.sx.server0.entity.res.SimOperateEntity;

/**
 * @Author: ZhangQi
 * @Date: 2023/9/11 16:40
 * @Description:
 */
public interface AdsWriteService {
    SimOperateEntity simOperateRead();

    void simStartOrStop();

    BaseRes setSimStop();

    boolean getButtonStatus();

    void testDrop();

    BaseRes setSimConfig(float pvCapacity);
}
