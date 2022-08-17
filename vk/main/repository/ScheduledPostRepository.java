package com.aboba.vk.main.repository;

import com.aboba.domain.vk.main.entity.VkAccountSettings;
import com.aboba.domain.vk.main.entity.schedule.ScheduledPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScheduledPostRepository extends JpaRepository<ScheduledPost, Long> {
    List<ScheduledPost> findAllByAccountSettings(VkAccountSettings accountSettings);

    Page<ScheduledPost> findAllByAccountSettings(VkAccountSettings accountSettings, Pageable pageable);

    List<ScheduledPost> findByPostId(Integer postId);
}
