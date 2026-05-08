package com.example.kcomproject.domain.menu.entity;

import com.example.kcomproject.global.entity.BaseEntity;
import com.example.kcomproject.global.exception.common.ErrorCode;
import com.example.kcomproject.global.exception.domain.MenuException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "menu_stocks")
public class MenuStock extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long storeId;

    @Column(nullable = false)
    private Long menuId;

    @Column(nullable = false)
    private Integer quantity;

    @Builder
    public MenuStock(Long storeId, Long menuId, Integer quantity) {
        this.storeId = storeId;
        this.menuId = menuId;
        this.quantity = quantity;
    }

    public void decrease(int amount) {
        if (this.quantity < amount) {
            throw new MenuException(ErrorCode.INSUFFICIENT_STOCK);
        }
        this.quantity -= amount;
    }

    public boolean isSoldOut() {
        return this.quantity <= 0;
    }
}
