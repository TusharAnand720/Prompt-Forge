package com.prompt_forge.service;

import com.stripe.model.StripeObject;
import com.prompt_forge.dto.subscription.CheckoutRequest;
import com.prompt_forge.dto.subscription.CheckoutResponse;
import com.prompt_forge.dto.subscription.PortalResponse;

import java.util.Map;

public interface PaymentProcessor {
    CheckoutResponse createCheckoutSessionUrl(CheckoutRequest request);

    PortalResponse openCustomerPortal();

    void handleWebhookEvent(String type, StripeObject stripeObject, Map<String, String> metadata);
}
