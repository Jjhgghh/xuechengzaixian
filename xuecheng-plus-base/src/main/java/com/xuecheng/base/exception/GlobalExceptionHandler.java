package com.xuecheng.base.exception;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(XueChengPlusException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public RestErrorResponse costomException(XueChengPlusException e) {
        String errMessage = e.getErrMessage();
        log.error("系统异常{}",errMessage,e);
        return new RestErrorResponse(errMessage);
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public RestErrorResponse exception(Exception e) {
        String errMessage = e.getMessage();
        log.error("系统异常{}",errMessage,e);
        if (errMessage.equals("不允许访问")){
            return new RestErrorResponse("没有操作此功能的权限");
        }

        return new RestErrorResponse(CommonError.UNKOWN_ERROR.getErrMessage());
    }
    //MethodArgumentNotValidException
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public RestErrorResponse MethodArgumentNotValidException(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        List<FieldError> fieldErrors = bindingResult.getFieldErrors();
        ArrayList<String> strings = new ArrayList<>();
        fieldErrors.stream().forEach(item->{
            strings.add(item.getDefaultMessage());
        });
        String join = StringUtils.join(strings, ",");
        log.error("系统异常{}",join,e);
        return new RestErrorResponse(join);
    }

}
