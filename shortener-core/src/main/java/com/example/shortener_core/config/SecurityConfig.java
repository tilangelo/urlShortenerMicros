package com.example.shortener_core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .formLogin(form -> form.disable())

                .httpBasic(Customizer.withDefaults())

                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/core-api/shorten/**",
                                "/core-api/policies",
                                "/core-api/policies/**"
                        ).hasRole("MANAGER")

                        .requestMatchers(
                                "/internal/**"
                        ).hasRole("SERVICE_GATEWAY")

                        .anyRequest().denyAll()
                )

                .build();
    }



    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }


    @Bean
    UserDetailsService userDetailsService(
            PasswordEncoder passwordEncoder,

            @Value("${app.security.manager.username}")
            String managerUsername,

            @Value("${app.security.manager.password}")
            String managerPassword,

            @Value("${app.security.gateway.username}")
            String gatewayUsername,

            @Value("${app.security.gateway.password}")
            String gatewayPassword
    ) {
        UserDetails manager = User.withUsername(managerUsername)
                .password(passwordEncoder.encode(managerPassword))
                .roles("MANAGER")
                .build();

        UserDetails gateway = User.withUsername(gatewayUsername)
                .password(passwordEncoder.encode(gatewayPassword))
                .roles("SERVICE_GATEWAY")
                .build();

        return new InMemoryUserDetailsManager(
                manager,
                gateway
        );
    }

}
