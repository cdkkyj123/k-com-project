package com.example.kcomproject.global.exception.domain;

import com.example.kcomproject.global.exception.common.ErrorCode;
import com.example.kcomproject.global.exception.common.ServiceException;

public class UserException extends ServiceException {
    public UserException(ErrorCode errorCode) {
        super(errorCode);
    }
}
