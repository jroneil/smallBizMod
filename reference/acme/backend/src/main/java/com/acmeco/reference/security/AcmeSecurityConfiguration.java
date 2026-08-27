package com.acmeco.reference.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AcmeSecurityProperties.class)
class AcmeSecurityConfiguration {

	@Bean
	SecurityFilterChain acmeSecurityFilterChain(HttpSecurity http) throws Exception {
		http
				.authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
				.httpBasic(httpBasic -> httpBasic.authenticationEntryPoint(
						(request, response, exception) -> response.sendError(401)))
				.csrf(AbstractHttpConfigurer::disable);
		return http.build();
	}

	@Bean
	UserDetailsService acmeUserDetailsService(
			AcmeSecurityProperties properties,
			PasswordEncoder passwordEncoder) {
		return new InMemoryUserDetailsManager(User.withUsername(properties.username())
				.password(passwordEncoder.encode(properties.password()))
				.roles("USER")
				.build());
	}

	@Bean
	PasswordEncoder acmePasswordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}
}
