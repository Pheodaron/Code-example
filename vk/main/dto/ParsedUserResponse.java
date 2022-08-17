package com.aboba.vk.main.dto;

import com.vk.api.sdk.objects.base.City;
import com.vk.api.sdk.objects.base.Country;
import com.vk.api.sdk.objects.groups.UserXtrRole;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Data
@NoArgsConstructor
public class ParsedUserResponse {
    private Integer id;
    private String bDate;
    private String country;
    private String city;
    private String sex;
    private String lastSeen;
    private String fullName;
    private String imageUrl;

    public ParsedUserResponse(UserXtrRole user) {
        this.id = user.getId();
        this.bDate = user.getBdate();
        this.country = Optional.ofNullable(user.getCountry()).map(Country::getTitle).orElse(null);
        this.city = Optional.ofNullable(user.getCity()).map(City::getTitle).orElse(null);
        switch (user.getSex().ordinal()) {
            case 1 -> this.sex = "female";
            case 2 -> this.sex = "male";
            default -> this.sex = "unknown";
        }
        if (user.getLastSeen() == null) {
            this.lastSeen = null;
        } else {
            var instant = Instant.ofEpochSecond(user.getLastSeen().getTime());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneOffset.ofHours(3));
            this.lastSeen = localDateTime.format(DateTimeFormatter.ISO_DATE_TIME);
        }
        fullName = String.format("%s %s", user.getFirstName(), user.getLastName());
        imageUrl = user.getPhoto100().toString();
    }
}
