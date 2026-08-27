package com.example.shortener_core.application.port.event;

/**
 * Передаёт shortcode удалённой политики listener-у для очистки Redis после commit транзакции.
 *
 * @param shortcode код ссылки, для которой удалена политика
 */
public record LinkPolicyDeletedEvent(String shortcode) {

    /**
     * Не позволяет создать событие без shortcode.
     */
    public LinkPolicyDeletedEvent {
        if (shortcode == null || shortcode.isBlank()) {
            throw new IllegalArgumentException("shortcode cannot be blank");
        }
    }
}
