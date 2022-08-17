package com.aboba.vk.main.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.aboba.domain.vk.main.entity.ScheduledStories;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import static com.aboba.utils.CheckContentType.isValidImageType;
import static com.aboba.utils.CheckContentType.isValidVideoType;

@Getter
@Setter
@NoArgsConstructor
public class StoriesDTO {
    @JsonProperty("id")
    private Long id;
    @JsonProperty(value = "contentType", required = true)
    private String contentType;
    @JsonProperty(value = "storiesUploadResult", required = true)
    private String storiesUploadResult;
    @JsonProperty("publishedDate")
    private String publishedDate;
    @JsonProperty("url")
    private String url;

    public StoriesDTO(ScheduledStories stories, String s3Url) {
        this.id = stories.getId();
        this.storiesUploadResult = stories.getUploadResult();
        this.publishedDate = stories.getPublishDate();
        var stringBeforeFormat = "%s/stories/%s/%s.%s";
        String type = "";
        if (isValidImageType(stories.getContentType())) {
            type = "image";
        } else if (isValidVideoType(stories.getContentType())) {
            type = "video";
        }
        this.contentType = stories.getContentType();
        this.url = String.format(stringBeforeFormat, s3Url, type, stories.getUploadResult(),
                stories.getContentType().substring(6));
    }
}
