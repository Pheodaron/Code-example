package com.aboba.vk.main.entity;

import com.aboba.domain.vk.main.enums.ECommentType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter
@Setter
@Table(name = "comments")
@NoArgsConstructor
public class Comment {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "text")
    private String text;

    @ManyToOne
    @JoinColumn(name = "account_settings_id")
    private VkAccountSettings accountSettings;

    @Column(name = "type")
    private ECommentType type;

    public Comment(String text, ECommentType type, VkAccountSettings accountSettings) {
        this.text = text;
        this.type = type;
        this.accountSettings = accountSettings;
    }
}
