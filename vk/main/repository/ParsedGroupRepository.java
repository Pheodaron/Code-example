package com.aboba.vk.main.repository;

import com.aboba.domain.vk.main.entity.ParsedGroup;
import com.aboba.domain.vk.main.entity.VkAccountSettings;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

public interface ParsedGroupRepository
        extends JpaRepository<ParsedGroup, Integer>, QuerydslPredicateExecutor<ParsedGroup> {
    Page<ParsedGroup> findAllByAccountSettings(VkAccountSettings settings, Pageable pageable);
}
