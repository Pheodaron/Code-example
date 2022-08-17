package com.aboba.vk.main.repository;

import com.aboba.domain.vk.main.entity.Theme;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ThemeRepository extends JpaRepository<Theme, String> {
    boolean existsByTheme(String theme);

    Theme findByTheme(String theme);

    List<Theme> findAllByWordsWordIn(List<String> words);
}
