package com.aboba.vk.main.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter
@Setter
@Table(name = "subscribed_groups")
@NoArgsConstructor
public class SubscribedGroup {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id")
    private Integer groupId;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "account_settings_id")
    private VkAccountSettings accountSettings;

    public SubscribedGroup(Integer groupId) {
        this.groupId = groupId;
    }
}
