package com.aboba.vk.main.entity.schedule;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.vk.api.sdk.objects.photos.PhotoSizesType;
import com.vk.api.sdk.objects.wall.WallpostAttachment;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter
@Setter
@Table(name = "photos")
@NoArgsConstructor
public class Photo {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "name")
    private String name;
    @Column(name = "height")
    private Integer height;
    @Column(name = "width")
    private Integer width;
    @Column(name = "url")
    private String url;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "post_id")
    private ScheduledPost post;

    public Photo(WallpostAttachment attachment, Integer accountId, ScheduledPost post) {
        var optionalPhoto = attachment.getPhoto().getSizes().stream().filter(size -> size.getType()
                .equals(PhotoSizesType.Q)).findFirst();
        if (optionalPhoto.isPresent()) {
            this.name = String.format("photo%s_%s", accountId, attachment.getPhoto().getId());
            var photo = optionalPhoto.get();
            this.height = photo.getHeight();
            this.width = photo.getWidth();
            this.url = photo.getUrl().toString();
            this.post = post;
        }
    }
}
