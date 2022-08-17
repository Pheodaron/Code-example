package com.aboba.vk.main.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UploadStoriesResultDTO {
    @JsonProperty("contentType")
    private String contentType;
    @JsonProperty("uploadResult")
    private String uploadResult;
    @JsonProperty("url")
    private String url;
}
