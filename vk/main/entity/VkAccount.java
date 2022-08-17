package com.aboba.vk.main.entity;

import com.aboba.domain.user.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.Hibernate;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Getter
@Setter
@Table(name = "vk_accounts")
@NoArgsConstructor
public class VkAccount {
    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "full_name")
    private String fullName;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "access_token")
    private String accessToken;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "account_settings_id")
    private VkAccountSettings settings;

    public VkAccount(User user, Integer id, String accessToken) {
        this.id = id;
        this.user = user;
        this.accessToken = accessToken;
        this.settings = new VkAccountSettings();
    }

    public VkAccount(User user, Integer id, String accessToken, String fullName) {
        this.id = id;
        this.user = user;
        this.accessToken = accessToken;
        this.settings = new VkAccountSettings();
        this.fullName = fullName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        VkAccount vkAccount = (VkAccount) o;
        return id != null && Objects.equals(id, vkAccount.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
