package com.prompt_forge.security;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class JwtUserPrincipal {
    Long userId;
    String username;

}
