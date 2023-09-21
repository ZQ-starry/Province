package com.sx.server0.component;

import com.sx.server0.entity.result.LineResultEntity;
import com.sx.server0.entity.result.NodeResultEntity;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

/**
 * @Author: ZhangQi
 * @Date: 2023/8/28 10:52
 * @Description: 随项目启动而启动的运行类
 */
@Component
public class RunWithStart implements ApplicationRunner {

    public static Field[] LineEntity;

    public static Field[] NodeEntity;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        /*
        数据实体类的映射
         */
        LineEntity = LineResultEntity.class.getDeclaredFields();
        for (Field field : LineEntity) {
            field.setAccessible(true);
        }

        NodeEntity = NodeResultEntity.class.getDeclaredFields();
        for (Field field : NodeEntity) {
            field.setAccessible(true);
        }
    }
}
