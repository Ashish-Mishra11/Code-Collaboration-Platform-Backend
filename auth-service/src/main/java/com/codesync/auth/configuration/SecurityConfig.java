package com.codesync.auth.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.codesync.auth.component.GitHubOAuthSuccessHandler;
import com.codesync.auth.component.GoogleOAuthSuccessHandler;
import com.codesync.auth.filter.JwtFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
	@Autowired
	private UserDetailsService userDetailsService;

	@Autowired
	private JwtFilter jwtFilter;
	
	@Autowired
	private GitHubOAuthSuccessHandler gitHubOAuthSuccessHandler;
	
	@Autowired
	private GoogleOAuthSuccessHandler googleOAuthSuccessHandler;
	
	 public SecurityConfig(UserDetailsService userDetailsService) {
	        this.userDetailsService = userDetailsService;
	    }
	    
		@Bean
		public AuthenticationProvider authProvider() {
			DaoAuthenticationProvider provider=new DaoAuthenticationProvider(userDetailsService);
			provider.setPasswordEncoder(new BCryptPasswordEncoder(12));
			return provider;
		}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

		http.csrf(customizer -> customizer.disable())
				.authorizeHttpRequests(request -> request
						.requestMatchers("/api/auth/register"
								,"/api/auth/forgot-password"
								,"/api/auth/reset-password"
								, "/api/auth/login"
								,"/oauth2/**"
								,"/login/oauth2/**"
								,"/api/auth/register/dev"
								,"/api/auth/admin/approve"
								,"/api/auth/admin/reject"
								,"/api/auth/approvedeveloper")
//						.requestMatchers("/**")
						.permitAll()
						.anyRequest().authenticated())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				// OAuth2 Login Configuration
	            .oauth2Login(oauth2 -> oauth2
	                .successHandler((request, response, authentication) -> {
	                	String registrationId = null;

	                    // Safe casting
	                    if (authentication instanceof OAuth2AuthenticationToken) {
	                        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
	                        registrationId = oauthToken.getAuthorizedClientRegistrationId();
	                    }

	                    if ("github".equals(registrationId)) {
	                        gitHubOAuthSuccessHandler.onAuthenticationSuccess(request, response, authentication);
	                    } 
	                    else if ("google".equals(registrationId)) {
	                        googleOAuthSuccessHandler.onAuthenticationSuccess(request, response, authentication);
	                    } 
	                    else {
	                        // Fallback
	                        response.sendRedirect("/");
	                    }
	                })
	            )
				.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
		return config.getAuthenticationManager();
	}
	
	
	
	
}
