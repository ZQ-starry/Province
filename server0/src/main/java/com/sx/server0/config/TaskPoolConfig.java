package com.sx.server0.config;


import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@Slf4j
@EnableAsync
public class TaskPoolConfig {

    private int corePoolSize = 2;

    private int maxPoolSize = 4;

    private int queueCapacity= 20;


    @Bean("task1")
    public Executor taskExecutor1() {
        log.warn("start asyncServiceExecutor");
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        //设置为1，任务顺序执行
        executor.setCorePoolSize(corePoolSize);//核心线程数：线程池创建时候初始化的线程数
        executor.setMaxPoolSize(maxPoolSize);//最大线程数：线程池最大的线程数，只有在缓冲队列满了之后才会申请超过核心线程数的线程
        executor.setQueueCapacity(queueCapacity);//缓冲队列200：缓冲执行任务的队列
        executor.setKeepAliveSeconds(60);//允许线程的空闲时间60秒：当超过了核心线程出之外的线程在空闲时间到达之后会被销毁
        executor.setThreadNamePrefix("executor-1-");//线程池名的前缀：设置好了之后可以方便我们定位处理任务所在的线程池
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());// 线程池对拒绝任务的处理策略
        // 执行初始化
        executor.initialize();
        return executor;
    }

    @Bean("task2")
    public Executor taskExecutor2() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        //设置为1，任务顺序执行
        executor.setCorePoolSize(corePoolSize);//核心线程数：线程池创建时候初始化的线程数
        executor.setMaxPoolSize(maxPoolSize);//最大线程数：线程池最大的线程数，只有在缓冲队列满了之后才会申请超过核心线程数的线程
        executor.setQueueCapacity(queueCapacity);//缓冲队列200：缓冲执行任务的队列
        executor.setKeepAliveSeconds(60);//允许线程的空闲时间60秒：当超过了核心线程出之外的线程在空闲时间到达之后会被销毁
        executor.setThreadNamePrefix("executor-2-");//线程池名的前缀：设置好了之后可以方便我们定位处理任务所在的线程池
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());// 线程池对拒绝任务的处理策略
        return executor;
    }
}
