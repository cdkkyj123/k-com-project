package com.example.kcomproject.domain.point.entity;

import com.example.kcomproject.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "point_histories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PointHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PointTransactionType type;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false)
    private Long balanceAfter;

    // FIFO 및 유효기간 관리를 위한 필드
    private LocalDateTime expiredAt;

    private Long remainAmount;

    public void decreaseRemain(Long amount) {
        if (this.remainAmount < amount) {
            throw new IllegalArgumentException("Deduction amount exceeds remain amount");
        }
        this.remainAmount -= amount;
    }
}
