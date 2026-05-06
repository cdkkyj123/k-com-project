package com.example.kcomproject.global.exception.domain;

import com.example.kcomproject.global.exception.common.ErrorCode;
import com.example.kcomproject.global.exception.common.ServiceException;

public class MenuException extends ServiceException {
    public MenuException(ErrorCode errorCode) {
        super(errorCode);
    }
}
