package com.aboba.vk.main.repository;

import com.aboba.domain.vk.main.entity.VkAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VkAccountRepository extends JpaRepository<VkAccount, Integer> {
    Optional<VkAccount> findByUserId(String userId);

    Optional<VkAccount> findByIdAndUserId(Integer accountId, String userId);

    List<VkAccount> findAllBySettingsApprovingFriendshipTrue();

    List<VkAccount> findAllBySettingsRepostingNewPostsInGroupsTrue();

    List<VkAccount> findAllBySettingsLikingNewPostsInGroupsTrue();

    List<VkAccount> findAllBySettingsCommentingNewPostsInGroupsTrue();

    List<VkAccount> findAllBySettingsLikingNewPhotosOfFriendsTrue();

    void deleteByUserId(String userId);
}
