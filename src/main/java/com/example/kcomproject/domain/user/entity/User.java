package com.example.kcomproject.domain.user.entity;

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

    public void charge(Long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Charge amount must be positive");
        }
        this.pointBalance += amount;
    }

    public void use(Long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Use amount must be positive");
        }
        if (this.pointBalance < amount) {
            throw new IllegalStateException("Insufficient points");
        }
        this.pointBalance -= amount;
    }
}
