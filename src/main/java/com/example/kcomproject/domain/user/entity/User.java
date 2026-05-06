package com.example.kcomproject.domain.user.entity;

import com.example.kcomproject.global.exception.common.ErrorCode;
import com.example.kcomproject.global.exception.domain.PointException;
import com.example.kcomproject.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long pointBalance;

    // 비즈니스 제약 사항 상수
    public static final Long MAX_POINT_BALANCE = 2_000_000L; // 최대 200만 원 보유 가능
    public static final Long MIN_CHARGE_AMOUNT = 10_000L;    // 최소 1만 원부터 충전 가능
    public static final Long MAX_CHARGE_AMOUNT = 500_000L;   // 1회 최대 50만 원 충전 가능

    public void charge(Long amount) {
        // 1회 충전 금액 검증
        if (amount < MIN_CHARGE_AMOUNT || amount > MAX_CHARGE_AMOUNT) {
            throw new PointException(ErrorCode.POINT_INVALID_AMOUNT);
        }
        
        // 최대 보유 한도 검증
        if (this.pointBalance + amount > MAX_POINT_BALANCE) {
            throw new PointException(ErrorCode.POINT_MAX_BALANCE_EXCEEDED);
        }
        
        this.pointBalance += amount;
    }

    public void use(Long amount) {
        if (amount <= 0) {
            throw new PointException(ErrorCode.POINT_INVALID_AMOUNT);
        }
        if (this.pointBalance < amount) {
            throw new PointException(ErrorCode.POINT_INSUFFICIENT_BALANCE);
        }
        this.pointBalance -= amount;
    }
}
