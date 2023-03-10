package wang.jilijili.framework.aspect;


import com.alibaba.fastjson2.JSONObject;
import com.github.ksuid.KsuidGenerator;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.java.Log;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import wang.jilijili.common.exception.BizException;
import wang.jilijili.common.utils.IpUtils;
import wang.jilijili.framework.annotation.JilJilOperationLog;
import wang.jilijili.system.mapper.OperationLogMapper;
import wang.jilijili.system.mapper.UserMapper;
import wang.jilijili.system.pojo.entity.OperationLog;
import wang.jilijili.system.pojo.entity.User;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

/**
 * @author Amani
 * @date 2023年03月06日 10:28
 */
@Aspect
@Component
@Log
public class LogAspect {

    @Autowired
    OperationLogMapper operationLogMapper;

    @Autowired
    UserMapper userMapper;

    @Pointcut(value = "@annotation(wang.jilijili.framework.annotation.JilJilOperationLog)")
    public void operationLogPointcut() {

    }


    @Around(value = "operationLogPointcut()")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature methodSignature = (MethodSignature) pjp.getSignature();
        Method method = methodSignature.getMethod();

        OperationLog operationLog = new OperationLog();
        JilJilOperationLog annotation = method.getAnnotation(JilJilOperationLog.class);
        if (annotation != null) {
            operationLog.setModuleName(annotation.moduleName());
            operationLog.setOperationType(annotation.type().getNum());
        }
        operationLog.setMethodExecution(pjp.getTarget().getClass().getName() + "." + method.getName());
        operationLog.setRequestData(Arrays.toString(pjp.getArgs()));

        //获取request
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();

        operationLog.setRequestUrl(request.getMethod() + " " + request.getRequestURI());
        operationLog.setLastLoginIp(IpUtils.getIpAddress(request));

        Object proceed = pjp.proceed();

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object username = authentication.getPrincipal();

        User user = userMapper.getUserByUsername((String) username);
        // 设置id
        operationLog.setUserId(user.getId());
        operationLog.setUsername(user.getUsername());
        operationLog.setContent(JSONObject.toJSONString(proceed));
        operationLog.setId(KsuidGenerator.generate());

        operationLogMapper.insert(operationLog);
        return proceed;
    }

    @AfterThrowing(value = "operationLogPointcut()", throwing = "ex")
    public void afterThrowing(JoinPoint joinPoint, Throwable ex) {
        OperationLog operationLog = new OperationLog();


        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        JilJilOperationLog jilOperationLog = signature.getMethod().getAnnotation(JilJilOperationLog.class);
        if(jilOperationLog!=null){
            operationLog.setModuleName(jilOperationLog.moduleName());
            operationLog.setOperationType(jilOperationLog.type().getNum());
        }
        operationLog.setMethodExecution(joinPoint.getTarget().getClass().getName() + "." + signature.getName());
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object username = authentication.getPrincipal();

        User user = userMapper.getUserByUsername((String) username);
        operationLog.setId(KsuidGenerator.generate());
        operationLog.setUserId(user.getId());
        operationLog.setUsername(user.getUsername());
        // 设置异常信息
        if (ex != null) {
            operationLog.setContent(ex.getCause().getMessage());
        }

        //获取request
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();

        operationLog.setRequestUrl(request.getMethod() + " " + request.getRequestURI());

        operationLog.setRequestData(JSONObject.toJSONString(request.getParameterMap()));
        operationLog.setLastLoginIp(IpUtils.getIpAddress(request));

        operationLogMapper.insert(operationLog);

        throw new BizException(ex.getCause().getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value());

    }

}
