package org.legendstack.basebot.security;

import org.legendstack.basebot.user.DummyBotForgeUserService;
import org.legendstack.basebot.user.BotForgeUser;
import org.legendstack.basebot.user.BotForgeUserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Security configuration for BotForge — standard Spring Security for the React
 * SPA.
 */
@Configuration
@EnableWebSecurity
class SecurityConfiguration {

        @Value("${botforge.cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
        private List<String> corsAllowedOrigins;

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http, BotForgeUserService userService) throws Exception {
                // CsrfTokenRequestAttributeHandler for SPA compatibility (non-BREACH)
                var csrfHandler = new CsrfTokenRequestAttributeHandler();
                csrfHandler.setCsrfRequestAttributeName(null); // force eager resolution

                http
                                .csrf(csrf -> csrf
                                                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                                                .csrfTokenRequestHandler(csrfHandler)
                                                .ignoringRequestMatchers("/api/auth/login"))
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .addFilterAfter(new CsrfCookieFilter(),
                                                org.springframework.security.web.csrf.CsrfFilter.class)
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/api/auth/login", "/api/auth/users", "/api/config")
                                                .permitAll()
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

        /**
         * Spring Security 6.x defers CSRF token persistence. For SPAs using
         * CookieCsrfTokenRepository,
         * we must explicitly subscribe to the CsrfToken to ensure the cookie is written
         * on every response.
         */
        private static final class CsrfCookieFilter extends OncePerRequestFilter {
                @Override
                protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                FilterChain filterChain) throws ServletException, IOException {
                        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
                        if (csrfToken != null) {
                                // Force the token to be loaded and the cookie to be set
                                csrfToken.getToken();
                        }
                        filterChain.doFilter(request, response);
                }
        }

        @Bean
        CorsConfigurationSource corsConfigurationSource() {
                var config = new CorsConfiguration();
                config.setAllowedOrigins(corsAllowedOrigins);
                config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                config.setAllowedHeaders(List.of("*"));
                config.setAllowCredentials(true);
                var source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/api/**", config);
                return source;
        }

        /**
         * Development-only user service with hardcoded demo users.
         * In production, implement {@link BotForgeUserService} with a real identity
         * provider.
         */
        @Bean
        @Profile("dev")
        BotForgeUserService dummyBotForgeUserService(
                        com.embabel.agent.rag.service.NamedEntityDataRepository entityRepository) {
                return new DummyBotForgeUserService(
                                entityRepository,
                                new BotForgeUser("alice-id", "Alice Agu", "alice"),
                                new BotForgeUser("cassie-id", "Cassie Silver", "cassie"),
                                new BotForgeUser("bob-id", "Bob", "bob"));
        }
}
