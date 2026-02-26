package com.example.shortener_core.application.port.in;

import com.example.shortener_core.api.dto.RedirectResponse;

public interface RedirectUseCase {
    RedirectResponse redirect(String shortCode) throws Exception;
}
