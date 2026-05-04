package com.example.kcomproject.domain.menu.repository;

import com.example.kcomproject.domain.menu.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MenuRepository extends JpaRepository<Menu, Long>, MenuQueryRepository {

    @Query("SELECT m FROM Menu m JOIN Order o ON m.id = o.menuId WHERE o.createdAt >= :since GROUP BY m.id ORDER BY COUNT(o.id) DESC LIMIT 3")
    List<Menu> findPopularMenus(@Param("since") LocalDateTime since);
}
