package com.example.kcomproject.global.exception.domain;

import com.example.kcomproject.global.exception.common.ErrorCode;
import com.example.kcomproject.global.exception.common.ServiceException;

public class StoreException extends ServiceException {
    public StoreException(ErrorCode errorCode) {
        super(errorCode);
    }
}
