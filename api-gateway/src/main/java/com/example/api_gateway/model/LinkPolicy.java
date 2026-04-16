package com.example.api_gateway.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.util.SubnetUtils;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
public class LinkPolicy implements Serializable {
    
    private List<String> allowed_ips;
    private Instant time_start;
    private Instant time_end;
    private String auth_type;

    private static final long IPV4_MASK = 0xFFFFFFFFL;
    private static final int IPV4_OCTETS = 4;
    private static final int IPV4_MAX_PREFIX = 32;

    /**
     * Проверяет, находится ли текущее время в допустимом временном диапазоне
     * @return true or false в зависимости от того, находится ли попытка
     * в разрешённом временном окне
     */
    public boolean isTimeWindowValid() {
        Instant now = Instant.now();
        
        if (time_start == null && time_end == null) {
            return true; // Нет временных ограничений
        }

        log.info("Current time: {}", now);
        log.info("Time to start: {}", time_start);

        // true если timestart null или текущее время не раньше времени начала (now >= start)
        boolean afterStart = time_start == null || now.isAfter(time_start) || now.equals(time_start);
        // true если time_end null или текущее время не позже времени окончания (now <= end)
        boolean beforeEnd = time_end == null || now.isBefore(time_end) || now.equals(time_end);
        
        return afterStart && beforeEnd;
    }

    /**
     * метод для перебора ip паттернов из списка в policy
     * и вызов методов для их валидации
     * @param clientIp адрес клиента
     * @return true or false в зависимости о того,
     * находится ли ip клиента в разрешённом диапозоне
     */
    public boolean isIpAllowed(String clientIp) {
        if (allowed_ips == null || allowed_ips.isEmpty()) {
            return true; // Нет IP-ограничений
        }

        if (clientIp == null || clientIp.isBlank()) {
            log.error("Provided empty ip");
            throw new IllegalArgumentException("Client IP cannot be null or blank");
        }
        
        /*// разрешаю localhost
        if (clientIp.equals("127.0.0.1") || clientIp.equals("localhost") ||
            clientIp.equals("0:0:0:0:0:0:0:1") || clientIp.equals("::1")) {
            return true;
        }*/

        return allowed_ips.stream()
                .anyMatch(pattern -> matchesIpPattern(pattern, clientIp));
    }

    /**
     * Проверяет, соответствует ли IP заданному паттерну
     * Поддерживает: CIDR, диапазоны, wildcard, точное совпадение
     * @param pattern паттерн ip из policy - с которым сравнивают ip(ip клиента)
     * @param clientIp адрес клиента
     * @return возвращает true or false выше для метода isIpAllowed после проверки
     * в выбранном методе паттерна для валидации
     */
    public boolean matchesIpPattern(String pattern, String clientIp) {

        if(pattern == null || pattern.isBlank()) {
            log.warn("patter ip is empty: {}, returning false", pattern);
            return false;
        }

        // все адреса
        if (pattern.equals("0.0.0.0/0")) {
            return true; // Любой IP
        }

        try{
            // Поддержка CIDR нотации (например, 192.168.1.0/24)
            // используется commons-net
            if (pattern.contains("/")) {
                try {
                    SubnetUtils utils = new SubnetUtils(pattern);
                    utils.setInclusiveHostCount(true); // включать .0 и .255
                    return utils.getInfo().isInRange(clientIp);
                } catch (IllegalArgumentException e) {
                    log.debug("Invalid CIDR: {}", pattern);
                    return false;
                }
            }

            // Поддержка диапазонов через дефис (например, 192.168.1.1-192.168.1.50)
            if (pattern.contains("-")) {
                return isIpInRange(clientIp, pattern);
            }

            // Поддержка wildcard (например, 192.168.1.*)
            if (pattern.contains("*")) {
                return matchesWildcard(pattern, clientIp);
            }

            // Точное совпадение
            return pattern.equals(clientIp);
        } catch (IllegalArgumentException e) {
            log.error("IP pattern mismatch (invalid format): pattern={}, ip={}", pattern, clientIp, e);
            return false;
        }
    }


    /**
     * Валидирует ip клиента с паттерном range(знак деш)
     * @param ip адрес клиента
     * @param range паттерн ip из policy - с которым сравнивают ip(ip клиента)
     * @return true or false, совпал ли range
     */
    private boolean isIpInRange(String ip, String range) {
        var parts = range.split("-", 2);
        if (parts.length != 2) {
            return false;
        }

        long ipValue = ipToLong(ip);
        long start = ipToLong(parts[0].trim());
        long end = ipToLong(parts[1].trim());

        // Гарантия корректного порядка границ
        long min = Math.min(start, end);
        long max = Math.max(start, end);

        return ipValue >= min && ipValue <= max;
    }

    /**
     * Валидирует ip клиента с паттерном wildcard (192.168.1.*)
     * @param pattern паттерн ip из policy - с которым сравнивают ip(ip клиента)
     * @param ip адрес клиента
     * @return true or false, совпала ли wildcard
     */
    private boolean matchesWildcard(String pattern, String ip) {
        String[] patternParts = pattern.split("\\.");
        String[] ipParts = ip.split("\\.");

        if (patternParts.length != ipParts.length) {
            return false;
        }

        // читает каждую часть, false если проверка не дошла
        // до * в ip и части паттерна и ip клиента не совпадают
        // иначе цикл проходит и возвращает true
        for (int i = 0; i < patternParts.length; i++) {
            // валидация что октета IP - число от 0 до 255 (всегда проверяем)
            if (!isValidOctet(ipParts[i])) {
                return false;
            }
            if (patternParts[i].equals("*")) {
                continue;
            }
            if (!patternParts[i].equals(ipParts[i])) {
                return false;
            }
        }

        return true;
    }


    //
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    //


    /**
     * преобразование IPv4 в long (беззнаковое 32-битное значение) (ex: 192.168.1.1  //  → 3232235777  (0xC0A80101))
     * @param ip строка ip
     * @return long значение исходного ip
     * @throws UnknownHostException если не валидный формат
     */
    private long ipToLong(String ip){
        try{

            byte[] address = InetAddress.getByName(ip).getAddress();
            if (address.length != IPV4_OCTETS) {
                throw new IllegalArgumentException("Not an IPv4 address");
            }
            // ByteBuffer.getInt() возвращает signed int, поэтому маска
            return ByteBuffer.wrap(address).getInt() & IPV4_MASK;

        }catch (UnknownHostException e){
            throw new IllegalArgumentException("Invalid IP address format: " + ip, e);
        }
    }


    private boolean isValidOctet(String octet) {
        try {
            int value = Integer.parseInt(octet);
            return value >= 0 && value <= 255;
        } catch (NumberFormatException e) {
            return false;
        }
    }

}
