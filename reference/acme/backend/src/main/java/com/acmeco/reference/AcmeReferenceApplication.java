package com.acmeco.reference;

import com.acmeco.reference.customer.internal.persistence.AcmeCustomerProfileEntity;
import com.acmeco.reference.customer.internal.persistence.AcmeCustomerProfileRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackageClasses = AcmeCustomerProfileEntity.class)
@EnableJpaRepositories(basePackageClasses = AcmeCustomerProfileRepository.class)
public class AcmeReferenceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AcmeReferenceApplication.class, args);
	}
}
