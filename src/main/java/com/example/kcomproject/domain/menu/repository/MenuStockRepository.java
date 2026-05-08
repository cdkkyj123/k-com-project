package com.example.kcomproject.domain.menu.repository;

import com.example.kcomproject.domain.menu.entity.MenuStock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MenuStockRepository extends JpaRepository<MenuStock, Long> {
    Optional<MenuStock> findByStoreIdAndMenuId(Long storeId, Long menuId);
}
