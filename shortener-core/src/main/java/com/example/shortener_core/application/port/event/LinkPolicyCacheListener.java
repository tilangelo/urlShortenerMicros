package com.example.shortener_core.application.port.event;

import com.example.shortener_core.application.port.out.LinkPolicyCachePort;
import com.example.shortener_core.domain.model.LinkPolicy;
import com.example.shortener_core.domain.model.LinkPolicyRedis;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Duration;
import java.time.Instant;

/**
 * Синхронизирует политики ссылок с Redis после успешного commit в PostgreSQL.
 * Ошибка кеша логируется и не влияет на основную транзакцию.
 */
@Component
@Slf4j
public class LinkPolicyCacheListener {

    private final LinkPolicyCachePort cachePort;

    /**
     * Создаёт listener с доступом к кешу политик.
     *
     * @param cachePort порт для работы с кешем политик
     */
    public LinkPolicyCacheListener(LinkPolicyCachePort cachePort) {
        this.cachePort = cachePort;
    }

    /**
     * Сохраняет созданную политику в Redis после commit транзакции.
     *
     * @param event событие с созданной политикой
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCreatedLinkPolicy(LinkPolicyCreatedEvent event) {
        cachePolicy(event.linkPolicy());
    }

    /**
     * Перезаписывает политику в Redis после commit транзакции.
     *
     * @param event событие с обновлённой политикой
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUpdatedLinkPolicy(LinkPolicyUpdatedEvent event) {
        cachePolicy(event.linkPolicy());
    }

    /**
     * Удаляет политику из Redis после её удаления из PostgreSQL.
     *
     * @param event событие с shortcode удалённой политики
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDeletedLinkPolicy(LinkPolicyDeletedEvent event) {
        try {
            cachePort.deletePolicy(event.shortcode());
        } catch (RuntimeException exception) {
            log.error(
                    "Failed to delete cached policy for shortcode: {}",
                    event.shortcode(),
                    exception
            );
        }
    }

    /**
     * Преобразует политику в кеш-модель и сохраняет её с оставшимся TTL.
     * Истёкшая политика в Redis не записывается.
     *
     * @param policy политика для сохранения в кеш
     */
    private void cachePolicy(LinkPolicy policy) {
        LinkPolicyRedis redisPolicy = LinkPolicyRedis.fromDomain(policy);

        try {
            Duration remainingTtl = Duration.between(
                    Instant.now(),
                    redisPolicy.getTime_end()
            );

            if (remainingTtl.isZero() || remainingTtl.isNegative()) {
                log.warn(
                        "Skipping cache for expired policy, shortcode: {}",
                        policy.getShortcodeValue()
                );
                return;
            }

            cachePort.savePolicy(
                    policy.getShortcodeValue(),
                    redisPolicy,
                    remainingTtl
            );
        } catch (RuntimeException exception) {
            log.error(
                    "Failed to cache policy for shortcode: {}",
                    policy.getShortcodeValue(),
                    exception
            );
        }
    }
}
