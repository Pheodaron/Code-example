package com.aboba.vk.main;

import com.aboba.domain.vk.main.service.VkService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class VkCredentialsAspect {

    private final VkService vkService;

    public VkCredentialsAspect(VkService vkService) {
        this.vkService = vkService;
    }

    @Around("@annotation(com.aboba.annotation.VkCredentials)")
    public Object checkCredentials(ProceedingJoinPoint joinPoint) throws Throwable {
        var args = joinPoint.getArgs();
        var vkId = (Integer) args[0];
        args[1] = vkService.getAccountByVkId(vkId);
        return joinPoint.proceed(args);
    }
}
