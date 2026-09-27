package com.prompt_forge.service;

import com.prompt_forge.dto.subscription.PlanLimitResponse;
import com.prompt_forge.dto.subscription.UsageTodayResponse;

public interface UsageService {
    UsageTodayResponse getTodayUsage(Long userId);

    PlanLimitResponse getCurrentSubscriptionLimitsOfUser(Long userId);
}
