package com.prompt_forge.controller;

import com.prompt_forge.dto.subscription.PlanLimitResponse;
import com.prompt_forge.dto.subscription.UsageTodayResponse;
import com.prompt_forge.service.UsageService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/usage")
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class UsageController {

    UsageService usageService;

    @GetMapping("/today")
    public ResponseEntity<UsageTodayResponse> getTodayUsage() {
        Long userId = 0L;
        return ResponseEntity.ok(usageService.getTodayUsage(userId));
    }

    @GetMapping("/limits")
    public ResponseEntity<PlanLimitResponse> getPlanLimits() {
        Long userId = 0L;
        return ResponseEntity.ok(usageService.getCurrentSubscriptionLimitsOfUser(userId));
    }
}
