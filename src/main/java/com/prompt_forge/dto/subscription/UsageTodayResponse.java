package com.prompt_forge.dto.subscription;

public record UsageTodayResponse(
        Integer tokenUsed,
        Integer tokenLimit,
        Integer previewsRunning,
        Integer previewsLimit) {
}
