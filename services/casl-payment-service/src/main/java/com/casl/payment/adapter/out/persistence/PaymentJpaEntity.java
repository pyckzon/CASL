package com.casl.payment.adapter.out.persistence;

import com.casl.payment.domain.model.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class PaymentJpaEntity {

	@Id
	@Column(name = "id")
	private UUID id;

	@Column(name = "idempotency_key", nullable = false, length = 128, unique = true)
	private String idempotencyKey;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal amount;

	@Column(nullable = false, length = 3)
	private String currency;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PaymentStatus status;

	@Column(name = "failure_reason", length = 500)
	private String failureReason;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected PaymentJpaEntity() {
	}

	public PaymentJpaEntity(UUID id, String idempotencyKey, BigDecimal amount, String currency, PaymentStatus status,
			String failureReason, Instant createdAt, Instant updatedAt) {
		this.id = id;
		this.idempotencyKey = idempotencyKey;
		this.amount = amount;
		this.currency = currency;
		this.status = status;
		this.failureReason = failureReason;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public UUID getId() { return id; }
	public String getIdempotencyKey() { return idempotencyKey; }
	public BigDecimal getAmount() { return amount; }
	public String getCurrency() { return currency; }
	public PaymentStatus getStatus() { return status; }
	public String getFailureReason() { return failureReason; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
}
