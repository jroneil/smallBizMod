package com.oneilsoftware.sbp.module.job.internal.persistence;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.oneilsoftware.sbp.module.job.api.JobPriority;
import com.oneilsoftware.sbp.module.job.api.JobStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "sbp_job")
public class JobEntity {

	@Id
	private UUID id;

	@Column(name = "customer_id", nullable = false)
	private UUID customerId;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(length = 2000)
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private JobStatus status;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private JobPriority priority;

	@Column(name = "scheduled_date")
	private LocalDate scheduledDate;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected JobEntity() {
	}

	public JobEntity(
			UUID id,
			UUID customerId,
			String title,
			String description,
			JobStatus status,
			JobPriority priority,
			LocalDate scheduledDate,
			Instant createdAt,
			Instant updatedAt) {
		this.id = id;
		this.customerId = customerId;
		this.title = title;
		this.description = description;
		this.status = status;
		this.priority = priority;
		this.scheduledDate = scheduledDate;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public void updateFields(String title, String description, JobPriority priority, LocalDate scheduledDate, Instant updatedAt) {
		this.title = title;
		this.description = description;
		this.priority = priority;
		this.scheduledDate = scheduledDate;
		this.updatedAt = updatedAt;
	}

	public void changeStatus(JobStatus status, Instant updatedAt) {
		this.status = status;
		this.updatedAt = updatedAt;
	}

	public UUID getId() {
		return id;
	}

	public UUID getCustomerId() {
		return customerId;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public JobStatus getStatus() {
		return status;
	}

	public JobPriority getPriority() {
		return priority;
	}

	public LocalDate getScheduledDate() {
		return scheduledDate;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
