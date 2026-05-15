package com.stablecoin.custody.fireblocks.infrastructure.security

import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.HttpMethod
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.intercept.AuthorizationFilter
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfiguration {
    @Bean
    @Order(1)
    fun webhookSecurityFilterChain(
        http: HttpSecurity,
        webhookFilter: FireblocksWebhookAuthenticationFilter,
    ): SecurityFilterChain {
        http
            .securityMatcher("/api/v1/webhooks/**")
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { it.anyRequest().permitAll() }
            .addFilterBefore(webhookFilter, UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }

    @Bean
    fun webhookFilterRegistration(
        filter: FireblocksWebhookAuthenticationFilter,
    ): FilterRegistrationBean<FireblocksWebhookAuthenticationFilter> {
        val registration = FilterRegistrationBean(filter)
        registration.isEnabled = false
        return registration
    }

    @Bean
    fun rateLimitFilterRegistration(filter: RateLimitFilter): FilterRegistrationBean<RateLimitFilter> {
        val registration = FilterRegistrationBean(filter)
        registration.isEnabled = false
        return registration
    }

    @Bean
    @Order(2)
    fun apiSecurityFilterChain(
        http: HttpSecurity,
        rateLimitFilter: RateLimitFilter,
    ): SecurityFilterChain {
        http
            .securityMatcher("/api/**")
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .oauth2ResourceServer { it.jwt(Customizer.withDefaults()) }
            .authorizeHttpRequests {
                it
                    .requestMatchers(HttpMethod.GET, "/api/v1/**")
                    .hasAuthority("SCOPE_custody:read")
                    .requestMatchers(HttpMethod.POST, "/api/v1/**")
                    .hasAuthority("SCOPE_custody:write")
                    .anyRequest()
                    .authenticated()
            }.addFilterAfter(rateLimitFilter, AuthorizationFilter::class.java)
        return http.build()
    }
}
