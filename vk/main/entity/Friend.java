package com.aboba.vk.main.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.Hibernate;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Getter
@Setter
@Table(name = "friends")
@NoArgsConstructor
public class Friend {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vk_id")
    private Integer vkId;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "vk_account_settings_id")
    private VkAccountSettings accountSettings;

    public Friend(Integer vkId) {
        this.vkId = vkId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        Friend friend = (Friend) o;
        return id != null && Objects.equals(id, friend.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
