package itis.boardgametracker.security.config

import itis.boardgametracker.api.exception.entrypoint.DelegatedAccessDeniedHandler
import itis.boardgametracker.api.exception.entrypoint.DelegatedAuthenticationEntryPoint
import itis.boardgametracker.security.filter.JwtAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val delegatedAuthenticationEntryPoint: DelegatedAuthenticationEntryPoint,
    private val delegatedAccessDeniedHandler: DelegatedAccessDeniedHandler
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
}
