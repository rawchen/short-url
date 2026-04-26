package com.rawchen.shorturl.limit;

import com.rawchen.shorturl.entity.Result;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

/**
 * 限流切面
 *
 * @author RawChen
 * @date 2022-03-31 11:46
 */
@Aspect
@Component
public class RateLimitAspect {

	private static final Logger logger = LoggerFactory.getLogger(RateLimitAspect.class);

	/**
	 * 滑动窗口限流器（本地内存实现）
	 * 相比令牌桶，滑动窗口没有令牌积累效应，空闲后不会"补偿"请求
	 */
	private static final SlidingWindowRateLimiter rateLimiter = new SlidingWindowRateLimiter();

	/**
	 * 切点
	 * 通过扫包切入 @Pointcut("execution(public * com.rawchen.shorturl.*.*(..))")
	 * 带有指定注解切入 @Pointcut("@annotation(com.rawchen.shorturl.annotation.RateLimit)")
	 */
	@Pointcut("@annotation(com.rawchen.shorturl.limit.RateLimit)")
	public void pointcut() {
	}

	@Around("pointcut()")
	public Object around(ProceedingJoinPoint point) throws Throwable {
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
		MethodSignature signature = (MethodSignature) point.getSignature();
		Method method = signature.getMethod();
		if (method.isAnnotationPresent(RateLimit.class)) {
			RateLimit rateLimit = method.getAnnotation(RateLimit.class);
			String cacheKey = RateLimitUtil.generateCacheKey(method, request);
			double perSecond = rateLimit.perSecond();

			if (!rateLimiter.tryAcquire(cacheKey, perSecond)) {
				String pointMethodName = point.getSignature().getName();
				logger.info("限流{}方法，具体内容【{}】", pointMethodName, cacheKey);
				if ("toLink".equals(pointMethodName)) {
					return "redirect:" + "/400";
				} else {
					return Result.fail("你手速太快了");
				}
			} else {
				logger.info("放行{}方法，具体内容【{}】", point.getSignature().getName(), cacheKey);
			}
		}
		return point.proceed();
	}
}
