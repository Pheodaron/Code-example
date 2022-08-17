package com.aboba.vk.main.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class GroupCommentsDTO {
    private List<String> comments;
    private boolean commentingNewPostsInGroups;
}
