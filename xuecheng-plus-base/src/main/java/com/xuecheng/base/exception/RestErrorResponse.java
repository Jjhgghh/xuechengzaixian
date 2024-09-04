package com.xuecheng.base.exception;

import java.io.Serializable;

public class RestErrorResponse implements Serializable {
    private String errMessage;

    public String getErrMessage() {
        return errMessage;
    }

    public void setErrMessage(String errMessage) {
        this.errMessage = errMessage;
    }

    public RestErrorResponse() {
    }

    public RestErrorResponse(String errMessage) {
        this.errMessage = errMessage;
    }
}
