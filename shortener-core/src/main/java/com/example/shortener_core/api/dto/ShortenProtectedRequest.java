package com.example.shortener_core.api.dto;

import com.example.shortener_core.api.validation.TimeWindowRequest;
import com.example.shortener_core.api.validation.ValidTimeWindow;
import com.example.shortener_core.domain.model.LinkPolicy;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@ValidTimeWindow
public class ShortenProtectedRequest implements TimeWindowRequest {

    @NotBlank(message = "URL обязателен")
    @Size(max = 2048, message = "URL слишком длинный")
    private String longUrl;

    private List<String> allowedIps;

    private Instant allowedTimeStart;

    @NotNull(message = "Время окончания действия ссылки обязательно")
    @Future(message = "Время окончания действия ссылки должно быть в будущем")
    private Instant allowedTimeEnd;

    @NotNull(message = "Тип аутентификации обязателен")
    private LinkPolicy.AuthType authType = LinkPolicy.AuthType.NONE;
}
