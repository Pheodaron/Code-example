package com.aboba.vk.main.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.Map;

@Entity
@Getter
@Setter
@Table(name = "managed_groups")
@NoArgsConstructor
@AllArgsConstructor
public class ManagedGroup {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id")
    private Integer groupId;

    @Column(name = "access_token")
    private String accessToken;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "account_settings_id")
    private VkAccountSettings accountSettings;

    public ManagedGroup(Map.Entry<Integer, String> groupCredentials, VkAccountSettings accountSettings) {
        this.groupId = groupCredentials.getKey();
        this.accessToken = groupCredentials.getValue();
        this.accountSettings = accountSettings;
    }
}
