package com.aboba.vk.main.entity;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.vk.api.sdk.objects.groups.responses.GetByIdObjectLegacyResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.Hibernate;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "parsed_groups")
public class ParsedGroup {
    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "name")
    private String name;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "updated")
    private LocalDateTime updated;

    @JsonIgnore
    @ManyToMany(mappedBy = "parsedGroups")
    private Set<VkAccountSettings> accountSettings = new LinkedHashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "group", orphanRemoval = true)
    private List<ParsedGroupMember> parsedGroupMembers = new ArrayList<>();

    public ParsedGroup(GetByIdObjectLegacyResponse group) {
        this.id = group.getId();
        this.name = group.getName();
        this.imageUrl = group.getPhoto100().toString();
    }

    public void addVkSettings(VkAccountSettings accountSettings) {
        this.accountSettings.add(accountSettings);
        accountSettings.getParsedGroups().add(this);
    }

    public void removeVkSettings(VkAccountSettings accountSettings) {
        this.accountSettings.remove(accountSettings);
        accountSettings.getParsedGroups().remove(this);
    }

    public void setUpdatedNow() {
        this.updated = LocalDateTime.now();
    }

    @JsonGetter(value = "updated")
    public String getUpdated() {
        if (this.updated == null) {
            return null;
        }
        return DateTimeFormatter.ISO_DATE_TIME.format(this.updated);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        ParsedGroup group = (ParsedGroup) o;
        return id != null && Objects.equals(id, group.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
