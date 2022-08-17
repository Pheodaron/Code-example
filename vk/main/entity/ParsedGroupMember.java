package com.aboba.vk.main.entity;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vk.api.sdk.objects.base.City;
import com.vk.api.sdk.objects.base.Country;
import com.vk.api.sdk.objects.groups.UserXtrRole;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "parsed_group_members")
public class ParsedGroupMember {
    @Id
    @JsonIgnore
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vk_Id")
    private Integer vkId;

    @JsonProperty("bDate")
    @Column(name = "b_date")
    private LocalDate bDate;

    @JsonIgnore
    @Column(name = "country")
    private Integer country;

    @JsonIgnore
    @Column(name = "city")
    private Integer city;

    @JsonIgnore
    @Column(name = "sex")
    private Integer sex;

    @Column(name = "last_seen")
    private LocalDateTime lastSeen;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "image_url")
    private String imageUrl;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "group_id")
    private ParsedGroup group;

    public ParsedGroupMember(UserXtrRole user, ParsedGroup group) {
        this.group = group;
        this.vkId = user.getId();
        this.bDate = parseVkDateToLocalDate(user.getBdate());
        this.country = Optional.ofNullable(user.getCountry()).map(Country::getId).orElse(null);
        this.city = Optional.ofNullable(user.getCity()).map(City::getId).orElse(null);
        if (user.getSex().ordinal() == 0) {
            this.sex = null;
        } else {
            this.sex = user.getSex().ordinal();
        }
        if (user.getLastSeen() == null) {
            this.lastSeen = null;
        } else {
            var instant = Instant.ofEpochSecond(user.getLastSeen().getTime());
            this.lastSeen = LocalDateTime.ofInstant(instant, ZoneOffset.ofHours(3));
        }
        fullName = String.format("%s %s", user.getFirstName(), user.getLastName());
        imageUrl = user.getPhoto100().toString();
    }

    @JsonGetter("bDate")
    public String bDate() {
        if (this.bDate == null) {
            return null;
        }
        return DateTimeFormatter.ISO_DATE.format(this.bDate);
    }

    @JsonGetter("lastSeen")
    public String getLastSeen() {
        if (this.lastSeen == null) {
            return null;
        }
        return DateTimeFormatter.ISO_DATE_TIME.format(this.lastSeen);
    }

    private LocalDate parseVkDateToLocalDate(String vkBDate) {
        if (vkBDate == null) {
            return null;
        }
        var array = vkBDate.split("[.]");
        if (array.length != 3) {
            return null;
        }
        if (array[0].length() == 1) {
            array[0] = String.format("0%s", array[0]);
        }
        if (array[1].length() == 1) {
            array[1] = String.format("0%s", array[1]);
        }
        var finalDate = String.format("%s.%s.%s", array[0], array[1], array[2]);
        var formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        return LocalDate.parse(finalDate, formatter);
    }
}
