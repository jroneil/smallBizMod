package com.oneilsoftware.sbp.module.job.internal.web;

import java.time.LocalDate;

import com.oneilsoftware.sbp.module.job.api.JobPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record UpdateJobRequest(
		@NotBlank @Size(max = 200) String title,
		@Size(max = 2000) String description,
		JobPriority priority,
		LocalDate scheduledDate) {
}
