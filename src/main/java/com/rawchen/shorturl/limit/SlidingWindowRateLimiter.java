package com.rawchen.shorturl.limit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 滑动窗口限流器（本地内存实现）
 * <p>
 * 原理：统计当前时间窗口内的请求数，超过阈值则拒绝。
 * 相比令牌桶，滑动窗口没有令牌积累效应，空闲后不会"补偿"请求。
 * <p>
 * 支持小数 perSecond：
 * - perSecond=2.0：每秒最多2次，窗口=1秒，maxCount=2
 * - perSecond=0.5：每秒最多0.5次（即每2秒1次），窗口=2秒，maxCount=1
 * - perSecond=0.1：每秒最多0.1次（即每10秒1次），窗口=10秒，maxCount=1
 *
 * @author RawChen
 * @date 2026-04-26
 */
public class SlidingWindowRateLimiter {

    private static final Logger logger = LoggerFactory.getLogger(SlidingWindowRateLimiter.class);

    /**
     * 存储每个key的窗口数据
     * Key: 限流key
     * Value: 该key最近请求的时间戳队列
     */
    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<Long>> counters = new ConcurrentHashMap<>();

    public SlidingWindowRateLimiter() {
    }

    /**
     * 尝试获取许可
     *
     * @param key        限流key（如：接口名:IP）
     * @param perSecond  每秒允许的请求数（支持小数，如0.5表示每2秒1次）
     * @return true 表示允许通过，false 表示被限流
     */
    public boolean tryAcquire(String key, double perSecond) {
        long currentTime = System.currentTimeMillis();
        ConcurrentLinkedQueue<Long> queue = counters.computeIfAbsent(key, k -> new ConcurrentLinkedQueue<>());

        // 根据perSecond动态计算窗口大小
        // perSecond>=1: 窗口=1秒，maxCount=perSecond
        // perSecond<1:  窗口=1/perSecond秒，maxCount=1
        long windowSizeMs;
        int maxCount;
        if (perSecond >= 1.0) {
            windowSizeMs = 1000;
            maxCount = (int) Math.ceil(perSecond);
        } else {
            // perSecond=0.5 -> 窗口=2000ms，每2秒1次
            windowSizeMs = (long) Math.ceil(1000.0 / perSecond);
            maxCount = 1;
        }

        // 计算当前窗口的起始时间
        long windowStart = currentTime - windowSizeMs;

        // 清理过期的请求记录（滑动窗口核心）
        // 使用 <= 确保边界值也被清理：窗口为 [windowStart, currentTime)
        while (!queue.isEmpty() && queue.peek() <= windowStart) {
            queue.poll();
        }

        // 检查当前窗口内的请求数
        if (queue.size() < maxCount) {
            queue.offer(currentTime);
            return true;
        }

        return false;
    }

    /**
     * 清理所有过期的计数器（防止内存泄漏）
     */
    public void cleanExpired() {
        long currentTime = System.currentTimeMillis();
        // 使用较大的窗口来清理（10秒应该足够覆盖所有场景）
        long windowStart = currentTime - 10000;

        counters.entrySet().removeIf(entry -> {
            ConcurrentLinkedQueue<Long> queue = entry.getValue();
            // 清理过期时间戳
            while (!queue.isEmpty() && queue.peek() < windowStart) {
                queue.poll();
            }
            return queue.isEmpty();
        });
    }
}