package com.aboba.vk.main.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GroupResponse {
    private String img;
    private String name;
    private String category;
    private String members;
    private String url;
    private String shortName;

    public GroupResponse(
            String img,
            String name,
            String category,
            String members,
            String shortName
    ) {
        this.img = img;
        this.name = name;
        this.category = category;
        this.members = members;
        this.url = "https://vk.com" + shortName;
        this.shortName = shortName.substring(1);
    }
}
