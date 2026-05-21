package itis.boardgametracker.security.config

import itis.boardgametracker.api.exception.entrypoint.DelegatedAccessDeniedHandler
import itis.boardgametracker.api.exception.entrypoint.DelegatedAuthenticationEntryPoint
import itis.boardgametracker.security.filter.JwtAuthenticationFilter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val delegatedAuthenticationEntryPoint: DelegatedAuthenticationEntryPoint,
    private val delegatedAccessDeniedHandler: DelegatedAccessDeniedHandler,
    @Value("\${app.cors.allowed-origins:}")
    private val corsAllowedOrigins: String,
    @Value("\${app.cors.allowed-origin-patterns:http://localhost:*,http://127.0.0.1:*}")
    private val corsAllowedOriginPatterns: String
) {


    @Bean
    fun filterChain(
        http: HttpSecurity,
        jwtAuthenticationFilter: JwtAuthenticationFilter
    ): SecurityFilterChain {
        http
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .cors { }
            .csrf { it.disable() }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .logout { it.disable() }
            .exceptionHandling { ex ->
                ex.authenticationEntryPoint(delegatedAuthenticationEntryPoint)
                ex.accessDeniedHandler(delegatedAccessDeniedHandler)
            }
            .authorizeHttpRequests { auth ->
                auth.requestMatchers(
                    "/auth/register",
                    "/auth/login",
                    "/auth/refresh"
                ).permitAll()
                auth.anyRequest().authenticated()
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            allowedOrigins = parseCorsValues(corsAllowedOrigins)
            allowedOriginPatterns = parseCorsValues(corsAllowedOriginPatterns)
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf("Authorization", "Content-Type", "Accept", "Origin")
            maxAge = 3600
        }

        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", configuration)
        }
    }

    private fun parseCorsValues(value: String): List<String> {
        return value
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }
}
