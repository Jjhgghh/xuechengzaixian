package com.xuecheng.base.exception;

import lombok.Data;

@Data
public class XueChengPlusException extends RuntimeException{
    private  String errMessage;

    public XueChengPlusException(String errMessage) {
        super(errMessage);
        this.errMessage = errMessage;
    }
    public XueChengPlusException() {
        super();
    }
    public static void cast(String errMessage){
        throw new XueChengPlusException(errMessage);
    }
    public static void cast(CommonError commonError){
        throw new XueChengPlusException(commonError.getErrMessage());
    }
}
