package com.prompt_forge.service.impl;

import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.prompt_forge.dto.subscription.CheckoutRequest;
import com.prompt_forge.dto.subscription.CheckoutResponse;
import com.prompt_forge.dto.subscription.PortalResponse;
import com.prompt_forge.entity.Plan;
import com.prompt_forge.entity.User;
import com.prompt_forge.enums.SubscriptionStatus;
import com.prompt_forge.error.BadRequestException;
import com.prompt_forge.error.ResourceNotFoundException;
import com.prompt_forge.reposityory.PlanRepository;
import com.prompt_forge.reposityory.UserRepository;
import com.prompt_forge.security.AuthUtil;
import com.prompt_forge.service.PaymentProcessor;
import com.prompt_forge.service.SubscriptionService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Transactional
@Slf4j
public class StripePaymentProcessor implements PaymentProcessor {

    AuthUtil authUtil;
    PlanRepository planRepository;
    UserRepository userRepository;
    SubscriptionService subscriptionService;

    @Value("${client.url}")
    @NonFinal
    String frontEndUrl;

    @Override
    public CheckoutResponse createCheckoutSessionUrl(CheckoutRequest request) {
        Plan plan = planRepository.findById(request.planId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan", request.planId().toString()));
        Long userId = authUtil.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));

        var params = SessionCreateParams.builder()
                .addLineItem(
                        SessionCreateParams.LineItem.builder().setPrice(plan.getStripePriceId()).setQuantity(1L).build()
                )
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setSubscriptionData(
                        new SessionCreateParams.SubscriptionData.Builder()
                                .setBillingMode(SessionCreateParams.SubscriptionData.BillingMode.builder()
                                        .setType(SessionCreateParams.SubscriptionData.BillingMode.Type.FLEXIBLE)
                                        .build())
                                .build()
                )
                .setSuccessUrl(frontEndUrl + "/success.html?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(frontEndUrl + "/cancel.html")
                .putMetadata("user_id", userId.toString())
                .putMetadata("plan_id", plan.getId().toString());

        try {

            String stripeCustomerId = user.getStripeCustomerId();
            if (stripeCustomerId == null || stripeCustomerId.isEmpty()) {
                params.setCustomerEmail(user.getUsername());
            } else {
                params.setCustomer(stripeCustomerId);
            }

            Session session = Session.create(params.build());
            return new CheckoutResponse(session.getUrl());
        } catch (StripeException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public PortalResponse openCustomerPortal() {

        Long userId = authUtil.getCurrentUserId();
        User user = getUser(userId);
        String stripeCustomerId = user.getStripeCustomerId();

        if (stripeCustomerId == null || stripeCustomerId.isEmpty()) {
            throw new BadRequestException("User does not have a Stripe customer ID. Cannot open customer portal.");
        }
        try {
            var portalSession = com.stripe.model.billingportal.Session.create(
                    com.stripe.param.billingportal.SessionCreateParams.builder()
                            .setCustomer(stripeCustomerId)
                            .setReturnUrl(frontEndUrl)
                            .build());

            return new PortalResponse(portalSession.getUrl());
        } catch (StripeException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void handleWebhookEvent(String type, StripeObject stripeObject, Map<String, String> metadata) {
        log.info("Received Stripe webhook event: {} with metadata: {}", type, metadata);

        switch (type) {
            case "checkout.session.completed" -> handleCheckoutSessionCompleted((Session) stripeObject, metadata);
            case "customer.subscription.updated" -> handleCustomerSubscriptionUpdated((Subscription) stripeObject);
            case "customer.subscription.deleted" -> handleCustomerSubscriptionDeleted((Subscription) stripeObject);
            case "invoice.paid" -> handleInvoicePaid((Invoice) stripeObject);
            case "invoice.payment_failed" -> handleInvoicePaymentFailed((Invoice) stripeObject);
            default -> log.warn("Unhandled Stripe webhook event type: {}", type);
        }
    }

    private void handleCheckoutSessionCompleted(Session session, Map<String, String> metadata) {

        if (session == null) {
            log.error("Stripe session is null in checkout.session.completed webhook event");
            return;
        }

        Long userId = Long.parseLong(metadata.get("user_id"));
        Long planId = Long.parseLong(metadata.get("plan_id"));

        String subscriptionId = session.getSubscription();
        String customerId = session.getCustomer();

        User user = getUser(userId);

        if (user.getStripeCustomerId() == null) {
            user.setStripeCustomerId(customerId);
            userRepository.save(user);
        }
        subscriptionService.activateSubscription(
                userId, planId, subscriptionId, customerId
        );
    }

    private void handleCustomerSubscriptionUpdated(Subscription subscription) {
        if (subscription == null) {
            log.error("Stripe subscription is null in customer.subscription.updated webhook event");
            return;
        }

        SubscriptionStatus status = mapStripeStatus(subscription.getStatus());
        if (status == null) {
            log.warn("Unknown Stripe subscription status: {}, for subscription : {}", subscription.getStatus(), subscription.getId());
        }

        SubscriptionItem item = subscription.getItems().getData().get(0);
        Instant periodStart = toInstant(item.getCurrentPeriodStart());
        Instant periodEnd = toInstant(item.getCurrentPeriodEnd());

        Long planId = resolvePlanId(item.getPrice());

        subscriptionService.updateSubscription(
                subscription.getId(), status, periodStart, periodEnd, subscription.getCancelAtPeriodEnd(), planId
        );
    }

    private void handleCustomerSubscriptionDeleted(Subscription subscription) {
        if (subscription == null) {
            log.error("Stripe subscription is null in customer.subscription.deleted webhook event");
            return;
        }

        subscriptionService.cancelSubscription(subscription.getId());
    }

    private void handleInvoicePaid(Invoice invoice) {
        String subscriptionId = extractSubscriptionId(invoice);
        if (subscriptionId == null) {
            log.error("Failed to extract subscription ID from invoice with ID: {}", invoice.getId());
            return;
        }
        try {
            Subscription subscription = Subscription.retrieve(subscriptionId);

            SubscriptionItem item = subscription.getItems().getData().get(0);
            Instant periodStart = toInstant(item.getCurrentPeriodStart());
            Instant periodEnd = toInstant(item.getCurrentPeriodEnd());

            subscriptionService.renewSubscriptionPeriod(
                    subscriptionId, periodStart, periodEnd
            );
        } catch (Exception e) {
            log.error("Failed to retrieve subscription with ID: {}. Error: {}", subscriptionId, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private void handleInvoicePaymentFailed(Invoice invoice) {
        String subscriptionId = extractSubscriptionId(invoice);
        if (subscriptionId == null) {
            return;
        }

        subscriptionService.markSubscriptionPastDue(subscriptionId);
    }

    private SubscriptionStatus mapStripeStatus(String stripeStatus) {
        return switch (stripeStatus) {
            case "active" -> SubscriptionStatus.ACTIVE;
            case "past_due", "unpaid", "paused", "incomplete_expired" -> SubscriptionStatus.PAST_DUE;
            case "canceled" -> SubscriptionStatus.CANCELED;
            case "incomplete" -> SubscriptionStatus.INCOMPLETE;
            default -> {
                log.warn("Unknown Stripe subscription status: {}", stripeStatus);
                yield null;
            }
        };
    }

    private String extractSubscriptionId(Invoice invoice) {
        var parent = invoice.getParent();
        if (parent == null) {
            return null;
        }
        var subscriptionDetails = parent.getSubscriptionDetails();
        if (subscriptionDetails == null) {
            return null;
        }
        return subscriptionDetails.getSubscription();
    }

    private Long resolvePlanId(Price price) {
        if (price == null || price.getId() == null) {
            log.error("Price or Price ID is null in resolvePlanId");
            return null;
        }
        return planRepository.findByStripePriceId(price.getId())
                .map(Plan::getId)
                .orElse(null);
//                .orElseThrow(() -> new ResourceNotFoundException("Plan", "Stripe Price ID: " + price.getId()));
    }

    private Instant toInstant(Long epochSecond) {
        return Instant.ofEpochSecond(epochSecond);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));
    }

}
