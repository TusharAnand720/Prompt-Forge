package com.prompt_forge.mapper;

import com.prompt_forge.dto.subscription.PlanResponse;
import com.prompt_forge.dto.subscription.SubscriptionResponse;
import com.prompt_forge.entity.Plan;
import com.prompt_forge.entity.Subscription;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SubscriptionMapper {

    SubscriptionResponse toSubscriptionResponse(Subscription subscription);

    PlanResponse toPlanResponse(Plan plan);
}
