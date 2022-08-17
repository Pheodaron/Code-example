package com.aboba.vk.main.entity.schedule;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.aboba.domain.vk.main.entity.VkAccount;
import com.aboba.domain.vk.main.entity.VkAccountSettings;
import com.aboba.utils.DateTimeParseHelper;
import com.vk.api.sdk.objects.wall.WallpostFull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "scheduled_posts")
@NoArgsConstructor
public class ScheduledPost {
    @Id
    @JsonIgnore
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "postId")
    private Integer postId;

    @Column(name = "text")
    private String text;

    @Column(name = "scheduled_date")
    private Integer scheduledDate;

    @JsonIgnore
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "account_settings_id")
    private VkAccountSettings accountSettings;

    @OneToMany(mappedBy = "post", orphanRemoval = true, cascade = CascadeType.ALL)
    private List<Photo> photos = new ArrayList<>();

    public ScheduledPost(WallpostFull post, VkAccount account) {
        this.photos =
                post.getAttachments().stream().map(attachment -> new Photo(attachment, account.getId(), this)).toList();
        this.postId = post.getId();
        this.text = post.getText();
        this.scheduledDate = post.getDate();
        this.accountSettings = account.getSettings();
    }

    @JsonGetter("scheduledDate")
    public String getStringDate() {
        return DateTimeParseHelper.fromEpochSecondsToString(this.scheduledDate);
    }

    @JsonIgnore
    public List<String> getAttachments() {
        return photos.stream().map(Photo::getName).toList();
    }

    public void updateAttachments(WallpostFull post, Integer accountId) {
        this.photos.clear();
        this.photos.addAll(
                post.getAttachments().stream().map(attachment -> new Photo(attachment, accountId, this)).toList());
    }
}
