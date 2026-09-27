package com.prompt_forge.service.impl;


import com.prompt_forge.dto.subscription.PlanResponse;
import com.prompt_forge.service.PlanService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlanServiceImpl implements PlanService {

    @Override
    public List<PlanResponse> getAllActivePlans() {
        return List.of();
    }
}
