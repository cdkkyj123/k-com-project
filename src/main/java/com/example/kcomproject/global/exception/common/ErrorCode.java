package com.example.kcomproject.global.exception.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "잘못된 입력값입니다."),
    DUPLICATE_REQUEST(HttpStatus.CONFLICT, "이미 처리된 요청입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 이메일입니다."),
    USER_INFO_MISMATCH(HttpStatus.BAD_REQUEST, "유저의 정보가 일치하지 않습니다."),
    USER_FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다"),
    USER_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증되지 않은 유저입니다."),

    // Menu
    MENU_NOT_FOUND(HttpStatus.NOT_FOUND, "메뉴를 찾을 수 없습니다."),
    INVALID_MENU_STATUS(HttpStatus.BAD_REQUEST, "유효하지 않은 메뉴 상태입니다."),
    OUT_OF_STOCK(HttpStatus.BAD_REQUEST, "재고가 품절되었습니다."),
    INSUFFICIENT_STOCK(HttpStatus.BAD_REQUEST, "재고가 부족합니다."),
    STOCK_LOCK_FAILED(HttpStatus.CONFLICT, "재고 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."),

    // Store
    STORE_NOT_FOUND(HttpStatus.NOT_FOUND, "매장을 찾을 수 없습니다."),
    STORE_CLOSED(HttpStatus.BAD_REQUEST, "매장이 영업 중이 아닙니다."),

    // Point
    POINT_INSUFFICIENT_BALANCE(HttpStatus.BAD_REQUEST, "포인트 잔액이 부족합니다."),
    POINT_LOCK_FAILED(HttpStatus.CONFLICT, "포인트 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."),
    POINT_INVALID_AMOUNT(HttpStatus.BAD_REQUEST, "올바르지 않은 포인트 금액입니다."),
    POINT_MAX_BALANCE_EXCEEDED(HttpStatus.BAD_REQUEST, "포인트 최대 보유 한도를 초과했습니다."),

    // Order
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."),
    ORDER_CREATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "주문 생성에 실패했습니다.");

    private final HttpStatus status;
    private final String message;
}
