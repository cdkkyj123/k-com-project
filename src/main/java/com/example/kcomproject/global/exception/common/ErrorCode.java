package com.example.kcomproject.global.exception.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 이메일입니다."),
    USER_INFO_MISMATCH(HttpStatus.BAD_REQUEST, "유저의 정보가 일치하지 않습니다."),
    USER_FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다"),
    USER_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증되지 않은 유저입니다."),

    // Developer
    DEVELOPER_NOT_FOUND(HttpStatus.NOT_FOUND, "개발자를 찾을 수 없습니다."),
    DEVELOPER_PAY_RANGE_INVALID(HttpStatus.BAD_REQUEST, "최소 시급은 최대 시급보다 클 수 없습니다."),

    // Client
    CLIENT_NOT_FOUND(HttpStatus.NOT_FOUND, "클라이언트를 찾을 수 없습니다."),

    // Project
    PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "프로젝트를 찾을 수 없습니다."),
    PROJECT_ALREADY_CLOSED(HttpStatus.BAD_REQUEST,"이미 마감된 프로젝트입니다."),
    PROJECT_BUDGET_BAD_REQUEST(HttpStatus.BAD_REQUEST,"잘못된 예산 설정입니다."),
    PROJECT_STATUS_UPDATE_FAILED(HttpStatus.BAD_REQUEST,"허용되지 않은 상태 전환입니다."),

    // Category
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND,"카테고리를 찾을 수 없습니다."),
    CATEGORY_ALREADY_EXISTS(HttpStatus.CONFLICT,"이미 존재하는 카테고리입니다."),
    CATEGORY_IN_USE(HttpStatus.CONFLICT, "사용 중인 카테고리는 삭제할 수 없습니다."),

    // Proposal
    PROPOSAL_NOT_FOUND(HttpStatus.NOT_FOUND, "제안서를 찾을 수 없습니다."),
    PROPOSAL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 해당 프로젝트에 제안서를 제출했습니다."),
    PROPOSAL_ALREADY_ACCEPTED(HttpStatus.CONFLICT, "이미 승인된 제안서가 존재하는 프로젝트입니다."),
    PROPOSAL_NOT_ACCEPTED(HttpStatus.BAD_REQUEST, "제안서가 승인되지 않은 상태입니다."),
    PROPOSAL_ACCEPTED_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "제안서 승인에 실패했습니다."),
    PROPOSAL_LOCK_FAILED(HttpStatus.CONFLICT, "현재 요청이 많습니다. 잠시 후 다시 시도해주세요."),
    PROPOSAL_SLOT_FULL(HttpStatus.BAD_REQUEST, "제안서 슬롯이 가득 찼습니다."),

    // Admin
    INVALID_ADMIN_ROLE(HttpStatus.UNAUTHORIZED, "잘못된 역할입니다."),
    INVALID_ADMIN_STATUS(HttpStatus.BAD_REQUEST, "잘못된 상태 요청입니다."),
    ADMIN_STATUS_NOT_MATCH(HttpStatus.BAD_REQUEST, "승인 대기중 상태가 아닙니다."),
    ADMIN_NOT_FOUND(HttpStatus.NOT_FOUND, "관리자를 찾을 수 없습니다"),
    ADMIN_ALREADY_APPROVE(HttpStatus.BAD_REQUEST, "이미 승인된 관리자 입니다."),
    ADMIN_NOT_APPROVED(HttpStatus.FORBIDDEN, "승인되지 않은 관리자입니다."),
    ADMIN_CANNOT_APPROVE_SELF(HttpStatus.BAD_REQUEST, "자기 자신을 승인할 수 없습니다."),

    // Review
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "리뷰를 찾을 수 없습니다."),
    REVIEW_UPDATE_DATA_NULL(HttpStatus.NOT_FOUND,"업데이트 데이터를 입력해주세요."),
    REVIEW_INVALID_RATING_RANGE(HttpStatus.BAD_REQUEST, "평점은 0.0에서 5.0 사이여야 합니다."),
    REVIEW_INVALID_COUNT(HttpStatus.BAD_REQUEST, "리뷰 개수가 올바르지 않습니다."),
    REVIEW_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 해당 프로젝트에 리뷰를 작성했습니다."),
    REVIEW_RATING_UPDATE_FAILED_DEVELOPER(HttpStatus.CONFLICT, "개발자 평점 갱신에 실패했습니다. 잠시 후 다시 시도해주세요."),
    REVIEW_RATING_UPDATE_FAILED_CLIENT(HttpStatus.CONFLICT, "클라이언트 평점 갱신에 실패했습니다. 잠시 후 다시 시도해주세요."),

    // ChatRoom
    CHATROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다."),
    CHATROOM_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 채팅방입니다."),

    // Message
    MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "메시지를 찾을 수 없습니다."),
    MESSAGE_PUBLISH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "메시지 발행에 실패했습니다."),

    // Skill
    SKILL_NOT_FOUND(HttpStatus.NOT_FOUND, "기술을 찾을 수 없습니다."),
    SKILL_UPDATE_DATA_NULL(HttpStatus.BAD_REQUEST, "업데이트 데이터를 입력해주세요."),

    // Common
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "잘못된 입력값입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),


    // Portfolio
    PORTFOLIO_NOT_FOUND(HttpStatus.NOT_FOUND, "포트폴리오를 찾을 수 없습니다."),

    // PopularSearch
    SEARCH_LENGTH_TOO_LONG(HttpStatus.BAD_REQUEST, "검색어 최대 길이를 초과했습니다."),
    SEARCH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "검색 서비스 이용 중 서버 에러가 발생했습니다."),
    SEARCH_INVALID_CHARACTER(HttpStatus.BAD_REQUEST, "특수 문자는 검색할 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}
