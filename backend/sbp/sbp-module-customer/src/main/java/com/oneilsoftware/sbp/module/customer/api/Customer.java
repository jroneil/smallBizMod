package com.oneilsoftware.sbp.module.customer.api;

import java.util.UUID;

public record Customer(UUID id, String name, CustomerStatus status) {
}
