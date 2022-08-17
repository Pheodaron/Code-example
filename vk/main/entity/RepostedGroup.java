package com.aboba.vk.main.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.vk.api.sdk.objects.groups.responses.GetByIdObjectLegacyResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter
@Setter
@Table(name = "reposted_groups")
@NoArgsConstructor
public class RepostedGroup {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id")
    private Integer groupId;

    @Column(name = "name")
    private String name;

    @Column(name = "url")
    private String url;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "members_count")
    private Integer membersCount;

    @Column(name = "is_auto_reposted")
    private boolean isAutoReposted;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "account_settings_id")
    private VkAccountSettings accountSettings;

    public RepostedGroup(GetByIdObjectLegacyResponse group, String url, VkAccountSettings accountSettings) {
        this.groupId = group.getId();
        this.name = group.getName();
        this.imageUrl = group.getPhoto100().toString();
        this.membersCount = group.getMembersCount();
        this.url = url;
        this.accountSettings = accountSettings;
    }
}
