package com.prompt_forge.entity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UsageLog {

    Long id;

    User user;

    Project project;

    String action;
    Integer tokenUsed;
    Integer duration;
    String metaData;

    Instant createdAt;
}
