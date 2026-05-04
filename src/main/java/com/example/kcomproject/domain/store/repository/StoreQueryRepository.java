package com.example.kcomproject.domain.store.repository;

import com.example.kcomproject.domain.store.entity.Store;
import java.util.List;

public interface StoreQueryRepository {
    List<Store> findStoresByFilter(String keyword, Long lastId, int size);
}
