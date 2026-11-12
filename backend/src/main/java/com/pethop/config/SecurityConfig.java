package com.pethop.config;

import com.pethop.user.UserRepository;
import com.pethop.security.SecurityUserService;
import com.pethop.security.AccountStatusFilter;
import com.pethop.security.InMemoryRateLimiter;
import com.pethop.security.RateLimitFilter;
import com.pethop.security.RateLimitProperties;
import com.pethop.security.PasswordResetProperties;
import com.pethop.veterinary.VeterinaryAiProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties({RateLimitProperties.class, VeterinaryAiProperties.class, PasswordResetProperties.class})
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }

    @Bean
    DaoAuthenticationProvider authenticationProvider(SecurityUserService users, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(encoder);
        return provider;
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    SecurityContextRepository securityContextRepository() { return new HttpSessionSecurityContextRepository(); }

    @Bean
    SessionAuthenticationStrategy sessionAuthenticationStrategy() { return new ChangeSessionIdAuthenticationStrategy(); }

    @Bean
    RateLimitFilter rateLimitFilter(RateLimitProperties properties) {
        return new RateLimitFilter(new InMemoryRateLimiter(), properties);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, DaoAuthenticationProvider provider, ObjectProvider<UserRepository> users,
            RateLimitFilter rateLimitFilter,
            SecurityContextRepository contextRepository,
            @Value("${server.servlet.session.cookie.secure:false}") boolean secureCookie,
            @Value("${server.servlet.session.cookie.same-site:lax}") String sameSite) throws Exception {
        CookieCsrfTokenRepository csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepository.setCookiePath("/");
        csrfRepository.setCookieCustomizer(cookie -> cookie.secure(secureCookie).sameSite(sameSite));
        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.csrfTokenRepository(csrfRepository)
                    .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
            .authenticationProvider(provider)
            .addFilterBefore(rateLimitFilter, AuthorizationFilter.class)
            .addFilterBefore(new AccountStatusFilter(users.getIfAvailable()), AuthorizationFilter.class)
            .securityContext(context -> context.securityContextRepository(contextRepository))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/actuator/health", "/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                    .requestMatchers("/api/v1/auth/csrf", "/api/v1/auth/register", "/api/v1/auth/login",
                            "/api/v1/auth/password-reset/request", "/api/v1/auth/password-reset/confirm").permitAll()
                    .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                    .requestMatchers("/api/v1/veterinary-assistant/**").permitAll()
                    .requestMatchers("/api/v1/pets/*/veterinary-assistant/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/affiliates").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/v1/pets/*/reports").authenticated()
                    .requestMatchers(HttpMethod.GET, "/api/v1/pets/mine").hasAnyRole("SHELTER", "ADMIN")
                    .requestMatchers(HttpMethod.GET, "/api/v1/pets/*/health-records", "/api/v1/pets/*/health-records/upcoming").authenticated()
                    .requestMatchers(HttpMethod.GET, "/api/v1/pets", "/api/v1/pets/*").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/pet-images/*").permitAll()
                    .requestMatchers("/api/v1/pets/**").hasAnyRole("SHELTER", "ADMIN")
                    .anyRequest().authenticated())
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
