package com.ocbc.finance.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池配置类
 * 
 * @author OCBC Finance Team
 * @version 1.0.0
 */
@Slf4j
@Configuration
public class ThreadPoolConfig {

    /**
     * PDF处理专用线程池
     * 用于处理可能耗时较长的PDF文本提取任务
     */
    @Bean("pdfProcessingExecutor")
    public Executor pdfProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 核心线程数：2个（适合PDF处理的IO密集型任务）
        executor.setCorePoolSize(2);
        
        // 最大线程数：5个（避免过多线程竞争）
        executor.setMaxPoolSize(5);
        
        // 队列容量：10个（适中的队列大小）
        executor.setQueueCapacity(10);
        
        // 线程名前缀
        executor.setThreadNamePrefix("PDF-Processing-");
        
        // 空闲线程存活时间：60秒
        executor.setKeepAliveSeconds(60);
        
        // 拒绝策略：调用者运行（保证任务不丢失）
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // 等待所有任务完成后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        
        // 等待时间：30秒
        executor.setAwaitTerminationSeconds(30);
        
        // 初始化线程池
        executor.initialize();
        
        log.info("PDF处理线程池初始化完成 - 核心线程数: {}, 最大线程数: {}, 队列容量: {}", 
            executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
        
        return executor;
    }

    /**
     * 通用异步任务线程池
     * 用于处理一般的异步任务
     */
    @Bean("asyncTaskExecutor")
    public Executor asyncTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 核心线程数：4个
        executor.setCorePoolSize(4);
        
        // 最大线程数：10个
        executor.setMaxPoolSize(10);
        
        // 队列容量：20个
        executor.setQueueCapacity(20);
        
        // 线程名前缀
        executor.setThreadNamePrefix("Async-Task-");
        
        // 空闲线程存活时间：60秒
        executor.setKeepAliveSeconds(60);
        
        // 拒绝策略：调用者运行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // 等待所有任务完成后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        
        // 等待时间：30秒
        executor.setAwaitTerminationSeconds(30);
        
        // 初始化线程池
        executor.initialize();
        
        log.info("通用异步任务线程池初始化完成 - 核心线程数: {}, 最大线程数: {}, 队列容量: {}", 
            executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
        
        return executor;
    }
}
