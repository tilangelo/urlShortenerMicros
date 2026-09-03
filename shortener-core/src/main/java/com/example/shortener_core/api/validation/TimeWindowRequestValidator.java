package com.example.shortener_core.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.Instant;

public class TimeWindowRequestValidator
        implements ConstraintValidator<ValidTimeWindow, TimeWindowRequest> {

    @Override
    public boolean isValid(TimeWindowRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true;
        }

        Instant start = request.getAllowedTimeStart();
        Instant end = request.getAllowedTimeEnd();
        return start == null || end == null || !start.isAfter(end);
    }
}
