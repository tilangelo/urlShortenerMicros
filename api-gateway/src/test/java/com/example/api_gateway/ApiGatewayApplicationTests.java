package com.example.api_gateway;

import com.example.api_gateway.model.LinkPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ApiGatewayApplicationTests {

    @Test
    void contextLoads() {
    }

    // ==================== CIDR Tests ====================

    @Test
    void testCidrMatching_BasicMatch() {
        LinkPolicy policy = createPolicyWithIps("192.168.1.0/24");
        assertTrue(policy.isIpAllowed("192.168.1.42"));
        assertTrue(policy.isIpAllowed("192.168.1.0"));
        assertTrue(policy.isIpAllowed("192.168.1.255"));
    }

    @Test
    void testCidrMatching_NoMatch() {
        LinkPolicy policy = createPolicyWithIps("192.168.1.0/24");
        assertFalse(policy.isIpAllowed("192.168.2.1"));
        assertFalse(policy.isIpAllowed("10.0.0.1"));
    }

    @Test
    void testCidrMatching_InvalidPrefix() {
        LinkPolicy policy = createPolicyWithIps("192.168.1.0/33");
        assertFalse(policy.isIpAllowed("192.168.1.1"));
    }

    @Test
    void testCidrMatching_ZeroPrefix() {
        LinkPolicy policy = createPolicyWithIps("0.0.0.0/0");
        assertTrue(policy.isIpAllowed("192.168.1.1"));
        assertTrue(policy.isIpAllowed("10.0.0.1"));
        assertTrue(policy.isIpAllowed("255.255.255.255"));
    }

    @Test
    void testCidrMatching_ExactHost() {
        LinkPolicy policy = createPolicyWithIps("192.168.1.50/32");
        assertTrue(policy.isIpAllowed("192.168.1.50"));
        assertFalse(policy.isIpAllowed("192.168.1.51"));
    }

    // ==================== Range Tests ====================

    @Test
    void testRangeMatching_NormalOrder() {
        LinkPolicy policy = createPolicyWithIps("10.0.0.1-10.0.0.10");
        assertTrue(policy.isIpAllowed("10.0.0.5"));
        assertTrue(policy.isIpAllowed("10.0.0.1"));
        assertTrue(policy.isIpAllowed("10.0.0.10"));
    }

    @Test
    void testRangeMatching_ReverseOrder() {
        LinkPolicy policy = createPolicyWithIps("10.0.0.10-10.0.0.1");
        assertTrue(policy.isIpAllowed("10.0.0.5"));
        assertTrue(policy.isIpAllowed("10.0.0.1"));
        assertTrue(policy.isIpAllowed("10.0.0.10"));
    }

    @Test
    void testRangeMatching_OutOfRange() {
        LinkPolicy policy = createPolicyWithIps("10.0.0.1-10.0.0.10");
        assertFalse(policy.isIpAllowed("10.0.0.11"));
        assertFalse(policy.isIpAllowed("10.0.0.0"));
    }

    @Test
    void testRangeMatching_InvalidRangeFormat() {
        LinkPolicy policy = createPolicyWithIps("192.168.1.1");
        assertFalse(policy.matchesIpPattern("invalid-range", "192.168.1.5"));
    }

    // ==================== Wildcard Tests ====================

    @Test
    void testWildcardMatching_TwoAsterisks() {
        LinkPolicy policy = createPolicyWithIps("172.16.*.*");
        assertTrue(policy.isIpAllowed("172.16.5.200"));
        assertTrue(policy.isIpAllowed("172.16.0.0"));
        assertTrue(policy.isIpAllowed("172.16.255.255"));
    }

    @Test
    void testWildcardMatching_NoMatch() {
        LinkPolicy policy = createPolicyWithIps("172.16.*.*");
        assertFalse(policy.isIpAllowed("172.17.5.200"));
        assertFalse(policy.isIpAllowed("10.16.5.200"));
    }

    @Test
    void testWildcardMatching_InvalidOctet() {
        LinkPolicy policy = createPolicyWithIps("192.168.1.*");
        assertFalse(policy.isIpAllowed("192.168.1.abc"));
        assertFalse(policy.isIpAllowed("192.168.1.256"));
    }

    @Test
    void testWildcardMatching_OneAsterisk() {
        LinkPolicy policy = createPolicyWithIps("192.168.1.*");
        assertTrue(policy.isIpAllowed("192.168.1.0"));
        assertTrue(policy.isIpAllowed("192.168.1.255"));
        assertFalse(policy.isIpAllowed("192.168.2.1"));
    }

    @Test
    void testWildcardMatching_WrongLength() {
        LinkPolicy policy = createPolicyWithIps("192.168.*");
        assertFalse(policy.isIpAllowed("192.168.1.1"));
    }

    // ==================== Exact Match Tests ====================

    @Test
    void testExactMatching_Match() {
        LinkPolicy policy = createPolicyWithIps("192.168.1.100");
        assertTrue(policy.isIpAllowed("192.168.1.100"));
    }

    @Test
    void testExactMatching_NoMatch() {
        LinkPolicy policy = createPolicyWithIps("192.168.1.100");
        assertFalse(policy.isIpAllowed("192.168.1.101"));
    }

    // ==================== Multiple IPs in Policy ====================

    @Test
    void testMultipleIps_CidrAndExact() {
        LinkPolicy policy = createPolicyWithIps("192.168.1.0/24", "10.0.0.50");
        assertTrue(policy.isIpAllowed("192.168.1.42"));
        assertTrue(policy.isIpAllowed("10.0.0.50"));
        assertFalse(policy.isIpAllowed("10.0.0.51"));
    }

    @Test
    void testMultipleIps_WildcardAndRange() {
        LinkPolicy policy = createPolicyWithIps("192.168.*.*", "10.0.0.1-10.0.0.10");
        assertTrue(policy.isIpAllowed("192.168.5.5"));
        assertTrue(policy.isIpAllowed("10.0.0.5"));
        assertFalse(policy.isIpAllowed("172.16.1.1"));
    }

    // ==================== Empty/Null Policy Tests ====================

    @Test
    void testEmptyAllowedIps_AllowsAll() {
        LinkPolicy policy = LinkPolicy.builder()
                .allowed_ips(Collections.emptyList())
                .build();
        assertTrue(policy.isIpAllowed("192.168.1.1"));
        assertTrue(policy.isIpAllowed("10.0.0.1"));
    }

    @Test
    void testNullAllowedIps_AllowsAll() {
        LinkPolicy policy = new LinkPolicy();
        assertTrue(policy.isIpAllowed("192.168.1.1"));
    }

    // ==================== Invalid Input Tests ====================

    @Test
    void testInvalidInput_NullPattern() {
        LinkPolicy policy = new LinkPolicy();
        assertFalse(policy.matchesIpPattern(null, "192.168.1.1"));
    }

    @Test
    void testInvalidInput_EmptyPattern() {
        LinkPolicy policy = new LinkPolicy();
        assertFalse(policy.matchesIpPattern("", "192.168.1.1"));
    }

    @Test
    void testInvalidInput_NullClientIp() {
        LinkPolicy policy = createPolicyWithIps("192.168.1.0/24");
        assertThrows(IllegalArgumentException.class, () -> policy.isIpAllowed(null));
    }

    @Test
    void testInvalidInput_EmptyClientIp() {
        LinkPolicy policy = createPolicyWithIps("192.168.1.0/24");
        assertThrows(IllegalArgumentException.class, () -> policy.isIpAllowed("   "));
    }

    @Test
    void testInvalidInput_InvalidIpFormat() {
        LinkPolicy policy = createPolicyWithIps("192.168.1.0/24");
        assertFalse(policy.isIpAllowed("invalid-ip"));
        assertFalse(policy.isIpAllowed("192.168.1"));
    }

    @Test
    void testInvalidInput_InvalidCidr() {
        LinkPolicy policy = new LinkPolicy();
        assertFalse(policy.matchesIpPattern("invalid/24", "192.168.1.1"));
    }

    // ==================== Time Window Tests ====================

    @Test
    void testTimeWindow_NoRestrictions() {
        LinkPolicy policy = LinkPolicy.builder()
                .time_start(null)
                .time_end(null)
                .build();
        assertTrue(policy.isTimeWindowValid());
    }

    @Test
    void testTimeWindow_OnlyStartInPast() {
        LinkPolicy policy = LinkPolicy.builder()
                .time_start(Instant.now().minus(1, ChronoUnit.HOURS))
                .time_end(null)
                .build();
        assertTrue(policy.isTimeWindowValid());
    }

    @Test
    void testTimeWindow_OnlyStartInFuture() {
        LinkPolicy policy = LinkPolicy.builder()
                .time_start(Instant.now().plus(1, ChronoUnit.HOURS))
                .time_end(null)
                .build();
        assertFalse(policy.isTimeWindowValid());
    }

    @Test
    void testTimeWindow_OnlyEndInFuture() {
        LinkPolicy policy = LinkPolicy.builder()
                .time_start(null)
                .time_end(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();
        assertTrue(policy.isTimeWindowValid());
    }

    @Test
    void testTimeWindow_OnlyEndInPast() {
        LinkPolicy policy = LinkPolicy.builder()
                .time_start(null)
                .time_end(Instant.now().minus(1, ChronoUnit.HOURS))
                .build();
        assertFalse(policy.isTimeWindowValid());
    }

    @Test
    void testTimeWindow_ValidWindow() {
        LinkPolicy policy = LinkPolicy.builder()
                .time_start(Instant.now().minus(1, ChronoUnit.HOURS))
                .time_end(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();
        assertTrue(policy.isTimeWindowValid());
    }

    @Test
    void testTimeWindow_ExpiredWindow() {
        LinkPolicy policy = LinkPolicy.builder()
                .time_start(Instant.now().minus(2, ChronoUnit.HOURS))
                .time_end(Instant.now().minus(1, ChronoUnit.HOURS))
                .build();
        assertFalse(policy.isTimeWindowValid());
    }

    @Test
    void testTimeWindow_FutureWindow() {
        LinkPolicy policy = LinkPolicy.builder()
                .time_start(Instant.now().plus(1, ChronoUnit.HOURS))
                .time_end(Instant.now().plus(2, ChronoUnit.HOURS))
                .build();
        assertFalse(policy.isTimeWindowValid());
    }

    // ==================== Helper Methods ====================

    private LinkPolicy createPolicyWithIps(String... ips) {
        return LinkPolicy.builder()
                .allowed_ips(Arrays.asList(ips))
                .build();
    }
}
