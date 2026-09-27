package com.prompt_forge.mapper;

import com.prompt_forge.dto.auth.SignUpRequest;
import com.prompt_forge.dto.auth.UserProfileResponse;
import com.prompt_forge.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    User toEntityFromSignUpRequest(SignUpRequest signUpRequest);

    UserProfileResponse toUserProfileResponse(User user);
}
