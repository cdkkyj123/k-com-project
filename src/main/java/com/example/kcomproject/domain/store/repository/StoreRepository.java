package com.example.kcomproject.domain.store.repository;

import com.example.kcomproject.domain.store.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreRepository extends JpaRepository<Store, Long> {
}
