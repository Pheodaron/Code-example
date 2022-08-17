package com.aboba.vk.main.entity;

import com.aboba.domain.vk.main.entity.schedule.ScheduledPost;
import com.aboba.domain.vk.main.enums.ECommentType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.*;

@Entity
@Getter
@Setter
@Table(name = "vk_account_settings")
@NoArgsConstructor
public class VkAccountSettings {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "approving_friendship")
    private boolean approvingFriendship;

    @Column(name = "liking_photos_when_approve_friendship")
    private boolean likingPhotosWhenApproveFriendship;

    @Column(name = "commenting_photos_when_approve_friendship")
    private boolean commentingPhotosWhenApproveFriendship;

    @Column(name = "commenting_posts_when_subscribe_group")
    private boolean commentingPostsWhenSubscribeGroup;

    @Column(name = "liking_posts_when_subscribe_group")
    private boolean likingPostsWhenSubscribeGroup;

    @Column(name = "commenting_new_posts_in_groups")
    private boolean commentingNewPostsInGroups;

    @Column(name = "reposting_new_posts_in_groups")
    private boolean repostingNewPostsInGroups;

    @Column(name = "liking_new_photos_of_friends")
    private boolean likingNewPhotosOfFriends;

    @Column(name = "liking_new_posts_in_groups")
    private boolean likingNewPostsInGroups;

    @OneToMany(mappedBy = "accountSettings",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private List<Friend> friends = new ArrayList<>();

    @OneToMany(mappedBy = "accountSettings", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RepostedGroup> repostedGroups = new ArrayList<>();

    @OneToMany(mappedBy = "accountSettings", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ManagedGroup> managedGroups = new ArrayList<>();

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(name = "vk_account_settings_parsed_groups",
            joinColumns = @JoinColumn(name = "vk_account_settings_id"),
            inverseJoinColumns = @JoinColumn(name = "parsed_groups_id"))
    private Set<ParsedGroup> parsedGroups = new LinkedHashSet<>();

    @OneToMany(mappedBy = "accountSettings",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "accountSettings",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private List<SubscribedGroup> subscribedGroups = new ArrayList<>();

    @OneToMany(mappedBy = "accountSettings", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ScheduledPost> scheduledPosts = new ArrayList<>();

    public List<Comment> getGroupPostComments() {
        return this.comments.stream().filter(comment -> comment.getType().equals(ECommentType.GROUP_POST)).toList();
    }

    public void setGroupPostComments(List<String> texts) {
        comments.addAll(texts.stream().map(text -> new Comment(text, ECommentType.GROUP_POST, this)).toList());
    }

    public List<Comment> getFriendPhotoComments() {
        return this.comments.stream().filter(comment -> comment.getType().equals(ECommentType.FRIEND_PHOTO)).toList();
    }

    public void setFriendPhotoComments(List<String> texts) {
        comments.addAll(texts.stream().map(text -> new Comment(text, ECommentType.FRIEND_PHOTO, this)).toList());
    }

    public void deleteAllGroupPostComments() {
        comments.removeAll(getGroupPostComments());
    }

    public void deleteAllFriendPhotoComments() {
        comments.removeAll(getFriendPhotoComments());
    }

    public void addSubscribedGroup(SubscribedGroup group) {
        subscribedGroups.add(group);
        group.setAccountSettings(this);
    }

    public void setGroupCredentials(Map<Integer, String> groupTokens) {
        this.managedGroups.addAll(
                groupTokens.entrySet().stream().map(credentials -> new ManagedGroup(credentials, this))
                        .toList());
    }

    public void addFriend(Friend friend) {
        this.friends.add(friend);
        friend.setAccountSettings(this);
    }
}
