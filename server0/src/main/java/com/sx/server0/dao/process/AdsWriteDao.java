package com.sx.server0.dao.process;

import com.sx.server0.entity.device.InitialDataEntity;
import com.sx.server0.entity.res.SimOperateEntity;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

/**
 * @Author: ZhangQi
 * @Date: 2023/9/11 16:42
 * @Description:
 */
@Mapper
@Repository
public interface AdsWriteDao {
    // 获取仿真进度和配置的光伏容量
    SimOperateEntity getSimOperate();
    // 查询仿真的状态，用于启停数据读取方法
    int getSimStatus();

    int setSimStatus(int status);

    int getButtonStatus();

    @Insert("INSERT INTO time_axis(time) value (#{time})")
    void testDrop(String time);

    @Delete("DELETE FROM time_axis ")
    void testDrop1();

    @Update("ALTER TABLE time_axis AUTO_INCREMENT=1")
    void testDrop2();

    int getWriteCount();

    InitialDataEntity getInitialData(int count);

    int setSimConfig(float pvCapacity);
}
