package com.aboba.vk.main.repository;

import com.aboba.domain.vk.main.entity.ParsedGroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

public interface ParsedGroupMemberRepository extends JpaRepository<ParsedGroupMember, Integer>,
        QuerydslPredicateExecutor<ParsedGroupMember> {
    void deleteAllByGroupId(Integer groupId);
}
