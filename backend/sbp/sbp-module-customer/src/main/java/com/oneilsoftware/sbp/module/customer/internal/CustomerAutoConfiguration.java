package com.oneilsoftware.sbp.module.customer.internal;

import com.oneilsoftware.sbp.module.customer.api.CustomerOperations;
import com.oneilsoftware.sbp.module.customer.internal.application.CustomerApplicationService;
import com.oneilsoftware.sbp.module.customer.internal.persistence.CustomerEntity;
import com.oneilsoftware.sbp.module.customer.internal.persistence.CustomerRepository;
import com.oneilsoftware.sbp.module.customer.internal.web.CustomerController;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.persistence.autoconfigure.EntityScan;

@AutoConfiguration
@EntityScan(basePackageClasses = CustomerEntity.class)
@EnableJpaRepositories(basePackageClasses = CustomerRepository.class)
public class CustomerAutoConfiguration {

	@Bean
	CustomerOperations customerOperations(CustomerRepository customerRepository) {
		return new CustomerApplicationService(customerRepository);
	}

	@Bean
	CustomerController customerController(CustomerOperations customerOperations) {
		return new CustomerController(customerOperations);
	}
}
