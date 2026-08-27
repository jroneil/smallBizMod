package com.acmeco.reference;

import com.acmeco.reference.customer.internal.persistence.AcmeCustomerProfileEntity;
import com.acmeco.reference.customer.internal.persistence.AcmeCustomerProfileRepository;
import com.acmeco.reference.security.AcmeCredentialEntity;
import com.acmeco.reference.security.AcmeCredentialRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackageClasses = {AcmeCustomerProfileEntity.class, AcmeCredentialEntity.class})
@EnableJpaRepositories(basePackageClasses = {AcmeCustomerProfileRepository.class, AcmeCredentialRepository.class})
public class AcmeReferenceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AcmeReferenceApplication.class, args);
	}
}
