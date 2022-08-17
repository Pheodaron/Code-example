package com.aboba.vk.main.repository;

import com.aboba.domain.vk.main.entity.Word;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface WordRepository extends JpaRepository<Word, Long> {
    @Query("select word from Word w")
    List<String> findAllWordFields();
}
