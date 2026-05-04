package com.example.kcomproject.domain.menu.repository;

import com.example.kcomproject.domain.menu.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface MenuRepository extends JpaRepository<Menu, Long>, MenuQueryRepository {

    List<Menu> findAllByIdIn(Collection<Long> ids);
}
