package com.conduit.backend.shared.security;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.conduit.backend.shared.config.AppProperties;
import com.conduit.backend.shared.web.Api;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
public class SecurityConfig {

	private static final String V1 = Api.V1;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService,
			AuthUserLookup users, ContractAuthenticationEntryPoint entryPoint,
			ContractAccessDeniedHandler accessDeniedHandler) throws Exception {
		http
				.csrf(csrf -> csrf.disable())
				.cors(Customizer.withDefaults())
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.httpBasic(basic -> basic.disable())
				.formLogin(form -> form.disable())
				.logout(logout -> logout.disable())
				.headers(headers -> headers
						.referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.NO_REFERRER)))
				.exceptionHandling(ex -> ex
						.authenticationEntryPoint(entryPoint)
						.accessDeniedHandler(accessDeniedHandler))
				.addFilterBefore(new TokenAuthenticationFilter(jwtService, users),
						UsernamePasswordAuthenticationFilter.class)
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers("/health", "/error", "/openapi.yml", "/swagger-ui.html",
								"/swagger-ui/**").permitAll()
						// auth NONE
						.requestMatchers(HttpMethod.POST, V1 + "/users", V1 + "/users/login").permitAll()
						.requestMatchers(HttpMethod.GET, V1 + "/tags").permitAll()
						// auth REQUIRED (must be listed before the optional {slug} pattern)
						.requestMatchers(HttpMethod.GET, V1 + "/articles/feed").authenticated()
						// auth OPTIONAL: anonymous allowed, but a present token is still verified by the filter
						.requestMatchers(HttpMethod.GET, V1 + "/profiles/*").permitAll()
						.requestMatchers(HttpMethod.GET, V1 + "/articles", V1 + "/articles/*",
								V1 + "/articles/*/comments").permitAll()
						// everything else in the API requires a valid token
						.requestMatchers(V1 + "/**").authenticated()
						// non-API paths fall through to MVC and get a contract-shaped 404
						.anyRequest().permitAll());
		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource(AppProperties properties) {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(properties.corsAllowedOrigins());
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Request-Id"));
		config.setExposedHeaders(List.of("X-Request-Id"));
		config.setMaxAge(3600L);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}
}
