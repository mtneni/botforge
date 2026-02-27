package org.legendstack.basebot.security;

import org.legendstack.basebot.user.DummyBotForgeUserService;
import org.legendstack.basebot.user.BotForgeUser;
import org.legendstack.basebot.user.BotForgeUserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Security configuration for BotForge — standard Spring Security for the React
 * SPA.
 */
@Configuration
@EnableWebSecurity
class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, BotForgeUserService userService) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login", "/api/auth/users", "/api/config").permitAll()
                        .requestMatchers("/images/**", "/assets/**").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .userDetailsService(userService)
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.logoutUrl("/api/auth/logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID"));

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        var config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:3000"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    @Bean
    BotForgeUserService dummyBotForgeUserService(
            com.embabel.agent.rag.service.NamedEntityDataRepository entityRepository) {
        return new DummyBotForgeUserService(
                entityRepository,
                new BotForgeUser("alice-id", "Alice Agu", "alice"),
                new BotForgeUser("cassie-id", "Cassie Silver", "cassie"),
                new BotForgeUser("bob-id", "Bob", "bob"));
    }
}
