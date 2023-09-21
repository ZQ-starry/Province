package com.sx.server0.controller.ads;

import com.sx.server0.entity.common.BaseRes;
import com.sx.server0.entity.res.SimOperateEntity;
import com.sx.server0.service.process.AdsWriteService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Timer;
import java.util.TimerTask;

/**
 * @Author: ZhangQi
 * @Date: 2023/9/11 16:39
 * @Description: ADS写入控制层
 */
@RestController
@Api(tags = "仿真启停控制")
@CrossOrigin("*")
public class AdsWriteController {

    @Autowired
    private AdsWriteService adsWriteService;

    /**
     * 模拟仿真配置参数查询
     * @return
     */
    @PostMapping("/getSimOperate")
    @ResponseBody
    @ApiOperation(value = "模拟仿真配置参数查询")
    public SimOperateEntity simOperateRead() {
        return adsWriteService.simOperateRead();
    }

    @PostMapping("/setSimConfig")
    @ResponseBody
    @ApiOperation(value = "模拟仿真参数配置")
    public BaseRes setSimConfig(@RequestBody float pvCapacity) {
        return adsWriteService.setSimConfig(pvCapacity);
    }

    @PostMapping("/simStop")
    @ResponseBody
    @ApiOperation(value = "停止仿真")
    public BaseRes setSimStop() {
        return adsWriteService.setSimStop();
    }


    @PostMapping("/simStart")
    @ResponseBody
    @ApiOperation(value = "开始仿真")
    public void simStartOrStop() {
        adsWriteService.simStartOrStop();
    }

    @PostMapping("/getButtonStatus")
    @ResponseBody
    @ApiOperation(value = "按钮状态查询")
    public boolean getButtonStatus() {
        return adsWriteService.getButtonStatus();
    }

    @PostMapping("/testDrop")
    @ResponseBody
    public void testDrop() {
        adsWriteService.testDrop();
    }
}
