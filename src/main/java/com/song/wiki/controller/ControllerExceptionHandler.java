package com.song.wiki.controller;

import com.song.wiki.dto.resp.ErrorCode;
import com.song.wiki.dto.resp.RestfulModel;
import com.song.wiki.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
public class ControllerExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ControllerExceptionHandler.class);

    /**
     * 统一处理参数校验异常
     * @param e 捕捉到的异常
     */
    @ExceptionHandler(value = BindException.class)
    @ResponseBody
    public RestfulModel<Object> validExceptionHandler(BindException e) {
        String exceptionMsg = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        LOG.warn("参数校验失败：{}", exceptionMsg);
        return new RestfulModel<>(ErrorCode.ARGS_VALIDATION_ERROR, exceptionMsg, null);
    }

    /**
     * 统一业务处理
     * @param exc 异常
     */
    @ExceptionHandler(value = BusinessException.class)
    @ResponseBody
    public RestfulModel<Object> validExceptionHandler(BusinessException exc) {
        String errorMsg = exc.getCode().getDesc();
        LOG.warn("业务异常：{}", errorMsg);
        return new RestfulModel<>(ErrorCode.BUSINESS_EXCEPTION, errorMsg, null);
    }

    /**
     * 统一处理 Exception，此时表明程序出现 BUG
     * @param exc 异常
     */
    @ExceptionHandler(value = Exception.class)
    @ResponseBody
    public RestfulModel<Object> validExceptionHandler(Exception exc) {
        LOG.error("System error ", exc);
        String errorMsg = "System error occurred. Please contact the administrator.";
        return new RestfulModel<>(ErrorCode.UNKNOWN_ERROR, errorMsg, null);
    }
}
