package com.example.shortener_core.api.validation;

import java.time.Instant;

public interface TimeWindowRequest {
    Instant getAllowedTimeStart();
    Instant getAllowedTimeEnd();
}
