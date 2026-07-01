package com.futbol.scraping.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

@Aspect
@Component
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger("AUDIT");
    private static final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    @Around("execution(* com.futbol.scraping.service..*(..)) || execution(* com.futbol.scraping.web..*(..))")
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {
        String method = joinPoint.getSignature().toShortString();
        String params = Arrays.toString(joinPoint.getArgs());
        String user = resolveCurrentUser();
        String timestamp = LocalDateTime.now(ZoneOffset.UTC).format(fmt);

        long start = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long duration = System.currentTimeMillis() - start;

        log.info("[AUDIT] {} | User: {} | Method: {} | Params: {} | Duration: {}ms",
                timestamp, user, method, params, duration);

        return result;
    }

    private String resolveCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return "ANONYMOUS";
        }
        return auth.getName();
    }
}
