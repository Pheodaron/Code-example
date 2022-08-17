package com.aboba.vk.main.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Setter
@Getter
@Table(name = "themes")
@NoArgsConstructor
public class Theme {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "theme")
    private String theme;

    @Column(name = "code")
    private int code;

    @OneToMany(mappedBy = "theme", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Word> words = new LinkedHashSet<>();

    public Theme(String theme, Set<Word> words) {
        this.theme = theme;
        this.words = words;
    }

    public Theme(String theme, int code, Set<Word> words) {
        this.theme = theme;
        this.code = code;
        this.words = words;
    }
}
