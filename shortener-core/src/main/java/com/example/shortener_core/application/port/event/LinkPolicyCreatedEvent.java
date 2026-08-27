package com.example.shortener_core.application.port.event;

import com.example.shortener_core.domain.model.LinkPolicy;

/**
 * Передаёт созданную политику listener-у для сохранения в Redis после commit транзакции.
 *
 * @param linkPolicy сохранённая политика ссылки
 */
public record LinkPolicyCreatedEvent(LinkPolicy linkPolicy){
}
