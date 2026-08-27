package com.acmeco.reference.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AcmeSecurityProperties.class)
class AcmeSecurityConfiguration {

	@Bean
	SecurityFilterChain acmeSecurityFilterChain(HttpSecurity http) throws Exception {
		http
				.authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
				.httpBasic(httpBasic -> httpBasic.authenticationEntryPoint(
						(request, response, exception) -> response.sendError(401)))
				.csrf(AbstractHttpConfigurer::disable)
				.addFilterAfter(new AcmePasswordChangeRequiredFilter(), BasicAuthenticationFilter.class);
		return http.build();
	}

	@Bean
	UserDetailsService acmeUserDetailsService(AcmeCredentialService credentialService) {
		return credentialService::loadUserByUsername;
	}

	@Bean
	PasswordEncoder acmePasswordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}
}
