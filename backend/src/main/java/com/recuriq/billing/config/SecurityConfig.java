package com.recuriq.billing.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.recuriq.billing.repository.UserRepository;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

	// Sites allowed to call the API from a browser; set CORS_ALLOWED_ORIGINS when hosting the frontend elsewhere
	@Value("${recuriq.cors.allowed-origins:http://localhost:3000,http://127.0.0.1:3000}")
	private List<String> allowedOrigins;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.cors(cors -> cors.configurationSource(corsConfigurationSource())).csrf(csrf -> csrf.disable())
				.securityContext(context -> context.requireExplicitSave(false))
				.authorizeHttpRequests(auth -> auth.requestMatchers("/error").permitAll()
						.requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui/**", "/api-docs/**")
						.permitAll().requestMatchers("/api/customer/login", "/api/customer/register").permitAll()
						.requestMatchers("/api/customer/plans/featured", "/api/customer/plans/all").permitAll()
						.requestMatchers("/api/customer/coupons", "/api/customer/coupons/validate").permitAll()
						.requestMatchers("/api/manager/login").permitAll().requestMatchers("/logout").permitAll()
						.requestMatchers("/api/auth/me", "/api/auth/expired").permitAll().requestMatchers("/")
						.permitAll().requestMatchers("/api/**").authenticated().anyRequest().authenticated())
				.exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, authException) -> {
					response.setStatus(401);
					response.setContentType("application/json");
					response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Authentication required\"}");
				})).logout(logout -> logout.logoutUrl("/logout")
						.logoutSuccessHandler((request, response, authentication) -> {
							response.setStatus(200);
							response.setContentType("application/json");
							response.getWriter()
									.write("{\"success\":true,\"message\":\"Logout successful\",\"redirect\":\"/\"}");
						}).invalidateHttpSession(true).deleteCookies("JSESSIONID").permitAll())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
						.sessionFixation().migrateSession().maximumSessions(1).expiredUrl("/api/auth/expired"));

		return http.build();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
		return config.getAuthenticationManager();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOriginPatterns(allowedOrigins);
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
		configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With",
				"X-XSRF-TOKEN", "Access-Control-Request-Method", "Access-Control-Request-Headers"));
		configuration.setExposedHeaders(
				Arrays.asList("Access-Control-Allow-Origin", "Access-Control-Allow-Credentials", "Set-Cookie"));
		configuration.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	@Bean
	public UserDetailsService userDetailsService(UserRepository userRepository) {
		return username -> userRepository.findByEmail(username)
				.orElseThrow(() -> new UsernameNotFoundException("User not found"));
	}

	@Bean
	public AuthenticationProvider authenticationProvider(UserRepository userRepository) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
		provider.setUserDetailsService(userDetailsService(userRepository));
		provider.setPasswordEncoder(passwordEncoder());
		return provider;
	}
}
