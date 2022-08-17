package com.aboba.vk.main.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class StoriesPostRequest {
    @JsonProperty("id")
    private Long id;
    @JsonProperty("stories")
    private List<String> stories;
    @JsonProperty("publishedDate")
    private String publishedDate;
}
