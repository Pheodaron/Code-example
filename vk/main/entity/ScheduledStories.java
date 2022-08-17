package com.aboba.vk.main.entity;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.aboba.utils.CheckContentType;
import com.aboba.utils.DateTimeParseHelper;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "scheduled_stories")
@NoArgsConstructor
public class ScheduledStories {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "publish_date")
    private LocalDateTime publishDate;

    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "account_id")
    private VkAccount account;

    @JoinColumn(name = "upload_result")
    private String uploadResult;

    @JoinColumn(name = "content_type")
    private String contentType;

    public ScheduledStories(
            LocalDateTime publishDate,
            VkAccount account,
            String uploadResult,
            String contentType
    ) {
        this.publishDate = publishDate;
        this.account = account;
        this.uploadResult = uploadResult;
        setContentType(contentType);
    }

    @JsonGetter("publishDate")
    public String getPublishDate() {
        return DateTimeParseHelper.fromLocalDateTimeToString(this.publishDate);
    }

    public void setContentType(String contentType) {
        if (CheckContentType.isValidImageType(contentType)) {
            contentType = "image/jpeg";
        }
        this.contentType = contentType;
    }

    @JsonIgnore
    public String getS3ObjectKey() {
        var finalString = "stories";
        if (CheckContentType.isValidImageType(this.contentType)) {
            finalString = finalString.concat("/image/");
        } else if (CheckContentType.isValidVideoType(this.contentType)) {
            finalString = finalString.concat("/video/");
        }
        var fileName = String.format("%s.%s", this.uploadResult, this.contentType.substring(6));
        return finalString.concat(fileName);
    }
}
