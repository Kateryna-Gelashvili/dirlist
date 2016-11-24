package org.k.config;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Map;

@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    public static final String DEFAULT_ROLE_PREFIX = "ROLE_";

    public static final Map<String, String> ROLE_NAME_PREFIX_MAP = ImmutableMap
            .<String, String>builder()
            .put("USER", "user.")
            .put("ADMIN", "admin.")
            .build();
    private static final ImmutableList<String> CORS_ALL = ImmutableList.of(CorsConfiguration.ALL);

    @Autowired
    UserDetailsService userDetailsService;

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable()
                .authorizeRequests()
                .anyRequest()
                .hasAnyRole(ROLE_NAME_PREFIX_MAP.keySet().stream().toArray(String[]::new))
                .and()
                .httpBasic()
                .and()
                .cors();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.setAllowCredentials(true);
        corsConfiguration.setAllowedOrigins(CORS_ALL);
        corsConfiguration.setAllowedHeaders(CORS_ALL);
        corsConfiguration.setAllowedMethods(CORS_ALL);
        source.registerCorsConfiguration("/**", corsConfiguration);
        return source;
    }
}
