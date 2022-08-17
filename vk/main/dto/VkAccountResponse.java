package com.aboba.vk.main.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VkAccountResponse {
    @JsonProperty("id")
    private Integer id;

    @JsonProperty("fullName")
    private String fullName;
}
