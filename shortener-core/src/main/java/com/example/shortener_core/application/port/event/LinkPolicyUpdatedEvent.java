package com.example.shortener_core.application.port.event;

import com.example.shortener_core.domain.model.LinkPolicy;

/**
 * Передаёт обновлённую политику listener-у для обновления записи в Redis после commit транзакции.
 *
 * @param linkPolicy обновлённая политика ссылки
 */
public record LinkPolicyUpdatedEvent(LinkPolicy linkPolicy) {
}
