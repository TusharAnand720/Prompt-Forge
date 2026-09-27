package com.prompt_forge.service.impl;

import com.prompt_forge.dto.subscription.SubscriptionResponse;
import com.prompt_forge.entity.Plan;
import com.prompt_forge.entity.Subscription;
import com.prompt_forge.entity.User;
import com.prompt_forge.enums.SubscriptionStatus;
import com.prompt_forge.error.ResourceNotFoundException;
import com.prompt_forge.mapper.SubscriptionMapper;
import com.prompt_forge.reposityory.PlanRepository;
import com.prompt_forge.reposityory.ProjectMemberRepository;
import com.prompt_forge.reposityory.SubscriptionRepository;
import com.prompt_forge.reposityory.UserRepository;
import com.prompt_forge.security.AuthUtil;
import com.prompt_forge.service.SubscriptionService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Transactional
public class SubscriptionServiceImpl implements SubscriptionService {

    AuthUtil authUtil;

    SubscriptionRepository subscriptionRepository;
    UserRepository userRepository;
    PlanRepository planRepository;
    ProjectMemberRepository projectMemberRepository;

    SubscriptionMapper subscriptionMapper;

    @Override
    public SubscriptionResponse getCurrentSubscription() {
        Long userId = authUtil.getCurrentUserId();
        var subscription = subscriptionRepository.findByUserIdAndStatusIn(
                        userId, Set.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.PAST_DUE, SubscriptionStatus.TRIALING))
                .orElse(new Subscription());

        return subscriptionMapper.toSubscriptionResponse(subscription);
    }

    @Override
    public void activateSubscription(Long userId, Long planId, String subscriptionId, String customerId) {
        boolean exists = subscriptionRepository.existsByStripeSubscriptionId(subscriptionId);
        if (exists) {
            return;
        }

        User user = getUser(userId);
        Plan plan = getPlan(planId);

        Subscription subscription = Subscription.builder()
                .user(user)
                .plan(plan)
                .stripeSubscriptionId(subscriptionId)
                .status(SubscriptionStatus.INCOMPLETE)
                .build();

        subscriptionRepository.save(subscription);

    }

    @Override
    public void updateSubscription(String subscriptionId, SubscriptionStatus status, Instant periodStart, Instant periodEnd, Boolean cancelAtPeriodEnd, Long planId) {
        Subscription subscription = getSubscriptionByStripeSubscriptionId(subscriptionId);

        boolean hasSubscriptionUpdated = false;

        if (status != null && status != subscription.getStatus()) {
            hasSubscriptionUpdated = true;
            subscription.setStatus(status);
        }
        if (periodStart != null && periodStart.equals(subscription.getCurrentPeriodStart())) {
            hasSubscriptionUpdated = true;
            subscription.setCurrentPeriodStart(periodStart);
        }
        if (periodEnd != null && periodEnd.equals(subscription.getCurrentPeriodEnd())) {
            hasSubscriptionUpdated = true;
            subscription.setCurrentPeriodEnd(periodEnd);
        }
        if (cancelAtPeriodEnd != null && cancelAtPeriodEnd != subscription.getCancelAtPeriodEnd()) {
            hasSubscriptionUpdated = true;
            subscription.setCancelAtPeriodEnd(cancelAtPeriodEnd);
        }
        if (planId != null && planId.equals(subscription.getPlan().getId())) {
            hasSubscriptionUpdated = true;
            Plan newPLan = getPlan(planId);
            subscription.setPlan(newPLan);
        }

        if (hasSubscriptionUpdated) {
            subscriptionRepository.save(subscription);
        }
    }

    @Override
    public void cancelSubscription(String subscriptionId) {
        Subscription subscription = getSubscriptionByStripeSubscriptionId(subscriptionId);
        subscription.setStatus(SubscriptionStatus.CANCELED);
        subscriptionRepository.save(subscription);
    }

    @Override
    public void renewSubscriptionPeriod(String subscriptionId, Instant periodStart, Instant periodEnd) {
        Subscription subscription = getSubscriptionByStripeSubscriptionId(subscriptionId);

        Instant newStart = periodStart != null ? periodStart : subscription.getCurrentPeriodEnd();
        subscription.setCurrentPeriodStart(newStart);
        subscription.setCurrentPeriodEnd(periodEnd);

        if (subscription.getStatus() == SubscriptionStatus.PAST_DUE || subscription.getStatus() == SubscriptionStatus.INCOMPLETE) {
            subscription.setStatus(SubscriptionStatus.ACTIVE);
        }

        subscriptionRepository.save(subscription);
    }

    @Override
    public void markSubscriptionPastDue(String subscriptionId) {
        Subscription subscription = getSubscriptionByStripeSubscriptionId(subscriptionId);
        if (subscription.getStatus() == SubscriptionStatus.PAST_DUE) {
            return;
        }
        subscription.setStatus(SubscriptionStatus.PAST_DUE);
        subscriptionRepository.save(subscription);
    }

    @Override
    public boolean canCreateProject() {
        SubscriptionResponse subscription = getCurrentSubscription();
        int countOfOwnedProjects = projectMemberRepository.countProjectOwnedByUser(authUtil.getCurrentUserId());
        if (subscription.plan() == null) {
            return countOfOwnedProjects < 1;
        }
        return countOfOwnedProjects < subscription.plan().maxProjects();
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("userId", userId.toString()));
    }

    private Plan getPlan(Long planId) {
        return planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("planId", planId.toString()));
    }

    private Subscription getSubscriptionByStripeSubscriptionId(String subscriptionId) {
        return subscriptionRepository.findByStripeSubscriptionId(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("subscriptionId", subscriptionId));
    }
}
