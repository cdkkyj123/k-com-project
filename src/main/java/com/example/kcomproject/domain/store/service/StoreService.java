package com.example.kcomproject.domain.store.service;

import com.example.kcomproject.domain.store.entity.Store;
import com.example.kcomproject.domain.store.entity.StoreStatus;
import com.example.kcomproject.domain.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreService {

    private final StoreRepository storeRepository;

    public List<Store> findAllStores() {
        return storeRepository.findAll();
    }

    public void validateStoreStatus(Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Store not found: " + storeId));

        if (store.getStatus() != StoreStatus.OPEN) {
            throw new IllegalStateException("Store is not open: " + storeId);
        }
    }
}
