package com.recuriq.billing.ai;

import org.springframework.http.HttpStatus;

import com.recuriq.billing.exception.CustomException;

/** HTTP errors the AI providers raise, so every provider reports failures the same way. */
final class AiErrors {

    private AiErrors() {
    }

    static CustomException badKey() {
        return new CustomException("The AI assistant's API key is invalid.", HttpStatus.SERVICE_UNAVAILABLE, "AI_AUTH_FAILED");
    }

    static CustomException rateLimited() {
        return new CustomException("The AI assistant is busy. Please try again in a minute.", HttpStatus.TOO_MANY_REQUESTS, "AI_RATE_LIMITED");
    }

    static CustomException upstream() {
        return new CustomException("The AI service returned an error. Please try again.", HttpStatus.BAD_GATEWAY, "AI_UPSTREAM_ERROR");
    }

    static CustomException unreachable() {
        return new CustomException("Could not reach the AI service. Check the server's internet connection.", HttpStatus.BAD_GATEWAY, "AI_UNREACHABLE");
    }
}
