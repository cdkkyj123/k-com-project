package com.example.kcomproject.domain.store.service;

import com.example.kcomproject.domain.store.dto.response.StoreResponse;
import com.example.kcomproject.domain.store.entity.Store;
import com.example.kcomproject.domain.store.entity.StoreStatus;
import com.example.kcomproject.domain.store.repository.StoreRepository;
import com.example.kcomproject.global.dto.PageResponseDto;
import com.example.kcomproject.global.exception.common.ErrorCode;
import com.example.kcomproject.global.exception.domain.StoreException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreService {

    private final StoreRepository storeRepository;

    @Transactional
    @CacheEvict(value = "storeList", allEntries = true)
    public void updateStoreStatus(Long storeId, StoreStatus status) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new StoreException(ErrorCode.STORE_NOT_FOUND));
        store.updateStatus(status);
        storeRepository.save(store);
    }

    @Cacheable(value = "storeList", key = "#keyword + '_' + #lastId + '_' + #size", unless = "#result.content().isEmpty()")
    public PageResponseDto<StoreResponse> getStores(String keyword, Long lastId, int size) {
        List<Store> stores = storeRepository.findStoresByFilter(keyword, lastId, size);

        boolean hasNext = false;
        if (stores.size() > size) {
            hasNext = true;
            stores.remove(size);
        }

        List<StoreResponse> content = stores.stream()
                .map(StoreResponse::from)
                .toList();

        Long nextLastId = content.isEmpty() ? null : content.get(content.size() - 1).id();

        return PageResponseDto.ofCursor(content, size, hasNext, nextLastId);
    }

    public void validateStoreStatus(Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new StoreException(ErrorCode.STORE_NOT_FOUND));

        if (store.getStatus() != StoreStatus.OPEN) {
            throw new StoreException(ErrorCode.STORE_CLOSED);
        }
    }
}
