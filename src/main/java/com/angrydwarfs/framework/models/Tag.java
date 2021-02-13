/*
 * Copyright 2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.angrydwarfs.framework.models;

import com.angrydwarfs.framework.models.Enums.ETag;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.*;

import javax.persistence.*;

/**
 * Тэги пользователя. Записывается в БД в таблицу с имененм tags.
 * @version 0.001
 * @author habatoo
 *
 * @param "id" - primary key таблицы users.
 * @param "tagName" - тэг - вид активности.
 * @param "userTags" - пользователь тэга.
 * @see User (пользователи).
 * @param "tagLevel" - уровень тэга.
 * @see Level (уровень).
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "tags")
@ToString(of = {"id", "tagName"})
@EqualsAndHashCode(of = {"id"})
public class Tag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ETag tagName;

    @JsonIdentityInfo(
            generator = ObjectIdGenerators.PropertyGenerator.class,
            property = "id")
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User userTags;

    @JsonIdentityInfo(
            generator = ObjectIdGenerators.PropertyGenerator.class,
            property = "id")
    @ManyToOne
    @JoinColumn(name = "activity_id")
    private Activity userActivity;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "level_id")
    private Level tagLevel;

    public Tag(ETag tagName) {
        this.tagName = tagName;
    }

    @Override
    public String toString() {
        return this.tagName.toString() + ": " + this.tagLevel;
    }
}
