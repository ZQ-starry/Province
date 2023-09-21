package com.sx.server0.controller.ads;

import com.sx.server0.service.process.ProcessService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Timer;
import java.util.TimerTask;

/**
 * @Author: ZhangQi
 * @Date: 2023/7/31 10:30
 * @Description:
 */
@RestController
@EnableScheduling
public class ResultAdsController {

    @Autowired
    private ProcessService processService;



    @RequestMapping("/test")
    // @Scheduled(initialDelay = 5000,fixedRate = 5000)
    public void devResultRead() {
        System.out.println("------------------------");
        processService.dev011DataProcess();  //湖州1已测试
        processService.dev012DataProcess();  //湖州2已测试
        processService.dev021DataProcess();  //嘉兴1已测试
        processService.dev022DataProcess();  //嘉兴2已测试
        processService.dev031DataProcess();  //温州1已测试
        processService.dev032DataProcess();  //温州2已测试
        processService.dev041DataProcess();  //温州3已测试
        processService.dev051DataProcess();  //绍兴1已测
        processService.dev052DataProcess();  //绍兴2已测
        processService.dev061DataProcess();  //台州1已测
        processService.dev062DataProcess();  //台州2已测
        processService.dev071DataProcess();  //宁波1已测试
        processService.dev072DataProcess();  //宁波2已测试
        processService.dev081DataProcess();  //宁波3已测试
        processService.dev082DataProcess();  //舟山1已测
        processService.dev091DataProcess();  //杭州1已测试
        processService.dev092DataProcess();  //杭州2已测试
        processService.dev101DataProcess();  //杭州3已测试
        processService.dev102DataProcess();  //杭州4已测试
        processService.dev111DataProcess();  //杭州5已测
        processService.dev112DataProcess();  //杭州6已测
        processService.dev121DataProcess();  //杭州7已测
        processService.dev131DataProcess();  //金华1已测
        processService.dev132DataProcess();  //金华2已测
        processService.dev141DataProcess();  //丽衢1已测
        processService.dev142DataProcess();  //丽衢2已测
        // processService.dev151DataProcess();
        // processService.dev152DataProcess();
        // processService.dev161DataProcess();
        // processService.dev162DataProcess();
    }
}
