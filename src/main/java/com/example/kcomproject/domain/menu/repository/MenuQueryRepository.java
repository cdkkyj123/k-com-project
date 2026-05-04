package com.example.kcomproject.domain.menu.repository;

import com.example.kcomproject.domain.menu.entity.Menu;
import com.example.kcomproject.domain.menu.entity.MenuCategory;
import com.example.kcomproject.domain.menu.entity.MenuStatus;
import java.util.List;

public interface MenuQueryRepository {
    List<Menu> findMenusByFilter(String keyword, MenuCategory category, MenuStatus status, Long lastId, int size);
}
