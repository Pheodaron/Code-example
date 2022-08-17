package com.aboba.vk.main.repository;

import com.aboba.domain.vk.main.entity.RepostedGroup;
import com.aboba.domain.vk.main.entity.VkAccountSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RepostedGroupRepository extends JpaRepository<RepostedGroup, Long> {
    List<RepostedGroup> findAllByAccountSettingsAndIsAutoRepostedTrue(VkAccountSettings vkAccountSettings);

    List<RepostedGroup> findAllByAccountSettings(VkAccountSettings vkAccountSettings);
}
