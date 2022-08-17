package com.aboba.vk.main.repository;

import com.aboba.domain.vk.main.entity.ScheduledStories;
import com.aboba.domain.vk.main.entity.VkAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ScheduledStoriesRepository extends JpaRepository<ScheduledStories, Long> {
    List<ScheduledStories> findAllByPublishDateBefore(LocalDateTime localDateTime);

    List<ScheduledStories> findAllByPublishDateBeforeOrderByPublishDateAsc(LocalDateTime localDateTime);

    List<ScheduledStories> findAllByAccount(VkAccount account);

    Page<ScheduledStories> findAllByAccount(VkAccount account, Pageable pageable);

    Optional<ScheduledStories> findByIdAndAccount(Long id, VkAccount account);
}
