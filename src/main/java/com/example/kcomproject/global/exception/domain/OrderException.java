package com.example.kcomproject.global.exception.domain;

import com.example.kcomproject.global.exception.common.ErrorCode;
import com.example.kcomproject.global.exception.common.ServiceException;

public class OrderException extends ServiceException {
    public OrderException(ErrorCode errorCode) {
        super(errorCode);
    }
}
