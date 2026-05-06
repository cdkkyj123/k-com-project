package com.example.kcomproject.global.exception.domain;

import com.example.kcomproject.global.exception.common.ErrorCode;
import com.example.kcomproject.global.exception.common.ServiceException;

public class PointException extends ServiceException {
    public PointException(ErrorCode errorCode) {
        super(errorCode);
    }
}
