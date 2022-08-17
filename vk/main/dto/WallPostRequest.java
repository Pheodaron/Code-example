package com.aboba.vk.main.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class WallPostRequest {
    @JsonProperty("postId")
    private Integer postId;
    @JsonProperty("message")
    private String message;
    @JsonProperty("images")
    private List<String> images;
    @JsonProperty("videos")
    private List<String> videos;
    @JsonProperty("publishedDate")
    private String publishedDate;
}
