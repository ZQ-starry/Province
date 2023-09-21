package com.sx.server0.service.process;

import com.sx.server0.component.AdsWriteUtil;
import com.sx.server0.controller.ads.ResultAdsController;
import com.sx.server0.controller.read.DataReadController;
import com.sx.server0.dao.process.AdsWriteDao;
import com.sx.server0.entity.common.BaseRes;
import com.sx.server0.entity.common.ResStatus;
import com.sx.server0.entity.device.InitialDataEntity;
import com.sx.server0.entity.res.SimOperateEntity;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Time;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @Author: ZhangQi
 * @Date: 2023/9/11 16:41
 * @Description:
 */
@Service
public class AdsWriteServiceImpl implements AdsWriteService{


    @Autowired
    private AdsWriteDao adsWriteDao;

    @Autowired
    private ResultAdsController resultAdsController;

    /**
     * 模拟仿真
     * @return
     */
    @Override
    public SimOperateEntity simOperateRead() {
        return adsWriteDao.getSimOperate();
    }


    @Override
    public BaseRes setSimConfig(float pvCapacity) {
        BaseRes baseRes = new BaseRes();
        baseRes.setUser("zhejiang");
        int flag = adsWriteDao.setSimConfig(pvCapacity);
        if (flag != 0){
            baseRes.setStatus(ResStatus.SUCCESS);
        }else {
            baseRes.setStatus(ResStatus.FAILED);
        }
        return baseRes;
    }


    @Override
    public BaseRes setSimStop() {
        BaseRes baseRes = new BaseRes();
        baseRes.setUser("zhejiang");
        int status = 0;
        int flag = adsWriteDao.setSimStatus(status);
        if (flag != 0){
            baseRes.setStatus(ResStatus.SUCCESS);
        }else {
            baseRes.setStatus(ResStatus.FAILED);
        }
        return baseRes;
    }

    @Override
    public boolean getButtonStatus() {
        boolean value;
        int status = adsWriteDao.getButtonStatus();
        if (status == 1) {
            value = true;
        } else {
            value = false;
        }
        return value;
    }

    @Override
    public void testDrop() {
        String time = "111111111111111111111";
        for (int i=0;i<10;i++){
            adsWriteDao.testDrop(time);
        }
        System.out.println("已插入");
        adsWriteDao.testDrop1();
        for (int i=0;i<10;i++){
            adsWriteDao.testDrop(time);
        }
        System.out.println("已再次插入");
        adsWriteDao.testDrop1();
        adsWriteDao.testDrop2();
        for (int i=0;i<10;i++){
            adsWriteDao.testDrop(time);
        }
        System.out.println("已再再次插入");
    }

    /**
     * 数据读取的开始与停止，使用timer task进行控制
     */
    @Override
    public void simStartOrStop() {
        // 将仿真状态置为1
        int status = 1;
        int flag = adsWriteDao.setSimStatus(status);
        if (flag!=0){
            // 开始仿真前清空表中的所有数据 truncate Table
            final Timer timer = new Timer();
            TimerTask task = new TimerTask() {
                @SneakyThrows
                @Override
                public void run() {
                    // 查询一下控制状态
                    int status =  adsWriteDao.getSimStatus();
                    if (status == 0){
                        // 若状态为0，则停止读取
                        timer.cancel();
                    }else {
                        adsDataWrite();
                        Thread.sleep(2000);
                        resultAdsController.devResultRead();
                    }
                }
            };
            timer.schedule(task, 0, 5000);
        }
    }

    private void adsDataWrite(){
        // 查询写入状态，写入到第几次
        int count = adsWriteDao.getWriteCount();
        // 查询初始数据
        InitialDataEntity initialDataEntity = adsWriteDao.getInitialData(count);
        // 执行各类基础数据写入
        AdsWriteUtil adsWriteUtil = new AdsWriteUtil();
        adsWriteUtil.adsStructWrite(initialDataEntity);
    }
}
