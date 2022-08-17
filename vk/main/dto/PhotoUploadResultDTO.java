package com.aboba.vk.main.dto;

import com.vk.api.sdk.objects.photos.PhotoSizes;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PhotoUploadResultDTO {
    private int height;
    private int width;
    private String url;
    private String name;

    public PhotoUploadResultDTO(PhotoSizes photo, String name) {
        this.height = photo.getHeight();
        this.width = photo.getWidth();
        this.url = photo.getUrl().toString();
        this.name = name;
    }
}
