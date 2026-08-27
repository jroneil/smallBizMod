package com.oneilsoftware.sbp.module.usermaintenance.internal.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record UpdateGroupRequest(
		@NotBlank @Size(max = 100) String name,
		@Size(max = 500) String description) {
}
