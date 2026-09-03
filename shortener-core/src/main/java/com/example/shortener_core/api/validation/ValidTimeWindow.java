package com.example.shortener_core.api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TimeWindowRequestValidator.class)
public @interface ValidTimeWindow {
    String message() default "allowedTimeStart must be before or equal to allowedTimeEnd";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
