package com.ocbc.finance.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

/**
 * 线程池监控工具类
 * 定期输出线程池状态信息，便于性能调优
 * 
 * @author OCBC Finance Team
 * @version 1.0.0
 */
@Slf4j
@Component
public class ThreadPoolMonitor {

    @Autowired
    @Qualifier("pdfProcessingExecutor")
    private Executor pdfProcessingExecutor;

    @Autowired
    @Qualifier("asyncTaskExecutor")
    private Executor asyncTaskExecutor;

    /**
     * 每5分钟输出一次线程池状态
     */
    @Scheduled(fixedRate = 300000) // 5分钟 = 300000毫秒
    public void monitorThreadPools() {
        try {
            log.info("=== 线程池状态监控 ===");
            
            // 监控PDF处理线程池
            if (pdfProcessingExecutor instanceof ThreadPoolTaskExecutor) {
                ThreadPoolTaskExecutor pdfExecutor = (ThreadPoolTaskExecutor) pdfProcessingExecutor;
                logThreadPoolStatus("PDF处理线程池", pdfExecutor);
            }
            
            // 监控异步任务线程池
            if (asyncTaskExecutor instanceof ThreadPoolTaskExecutor) {
                ThreadPoolTaskExecutor asyncExecutor = (ThreadPoolTaskExecutor) asyncTaskExecutor;
                logThreadPoolStatus("异步任务线程池", asyncExecutor);
            }
            
        } catch (Exception e) {
            log.warn("线程池监控异常: {}", e.getMessage());
        }
    }

    /**
     * 输出线程池详细状态
     */
    private void logThreadPoolStatus(String poolName, ThreadPoolTaskExecutor executor) {
        if (executor.getThreadPoolExecutor() != null) {
            var threadPool = executor.getThreadPoolExecutor();
            
            log.info("{} - 核心线程数: {}, 最大线程数: {}, 当前线程数: {}, 活跃线程数: {}, " +
                    "队列大小: {}, 已完成任务数: {}, 总任务数: {}",
                poolName,
                threadPool.getCorePoolSize(),
                threadPool.getMaximumPoolSize(),
                threadPool.getPoolSize(),
                threadPool.getActiveCount(),
                threadPool.getQueue().size(),
                threadPool.getCompletedTaskCount(),
                threadPool.getTaskCount()
            );
            
            // 检查线程池是否过载
            double activeRatio = (double) threadPool.getActiveCount() / threadPool.getMaximumPoolSize();
            if (activeRatio > 0.8) {
                log.warn("{} 活跃线程比例过高: {:.2f}%, 建议检查任务处理情况", poolName, activeRatio * 100);
            }
            
            // 检查队列是否积压
            if (threadPool.getQueue().size() > executor.getQueueCapacity() * 0.8) {
                log.warn("{} 队列积压严重: {}/{}, 建议增加线程数或优化任务处理", 
                    poolName, threadPool.getQueue().size(), executor.getQueueCapacity());
            }
        }
    }

    /**
     * 手动触发线程池状态输出（用于调试）
     */
    public void printThreadPoolStatus() {
        log.info("手动触发线程池状态监控");
        monitorThreadPools();
    }
}
