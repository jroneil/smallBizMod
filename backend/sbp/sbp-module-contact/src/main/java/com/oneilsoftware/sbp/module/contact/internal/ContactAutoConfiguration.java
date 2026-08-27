package com.oneilsoftware.sbp.module.contact.internal;

import com.oneilsoftware.sbp.module.contact.api.ContactOperations;
import com.oneilsoftware.sbp.module.contact.internal.application.ContactApplicationService;
import com.oneilsoftware.sbp.module.contact.internal.persistence.ContactEntity;
import com.oneilsoftware.sbp.module.contact.internal.persistence.ContactRepository;
import com.oneilsoftware.sbp.module.contact.internal.web.ContactController;
import com.oneilsoftware.sbp.module.customer.api.CustomerOperations;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@AutoConfiguration
@EntityScan(basePackageClasses = ContactEntity.class)
@EnableJpaRepositories(basePackageClasses = ContactRepository.class)
public class ContactAutoConfiguration {

	@Bean
	ContactOperations contactOperations(
			ContactRepository contactRepository,
			CustomerOperations customerOperations) {
		return new ContactApplicationService(contactRepository, customerOperations);
	}

	@Bean
	ContactController contactController(ContactOperations contactOperations) {
		return new ContactController(contactOperations);
	}
}
