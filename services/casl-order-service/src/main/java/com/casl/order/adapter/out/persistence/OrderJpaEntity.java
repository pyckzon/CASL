package com.casl.order.adapter.out.persistence;

import com.casl.order.domain.model.OrderLine;
import com.casl.order.domain.model.OrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class OrderJpaEntity {

	@Id
	private UUID id;

	@Column(name = "user_id")
	private UUID userId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private OrderStatus status;

	@Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal totalAmount;

	@Column(nullable = false, length = 3)
	private String currency;

	@Column(name = "payment_id")
	private UUID paymentId;

	@Column(name = "failure_reason", length = 500)
	private String failureReason;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "lines", columnDefinition = "jsonb", nullable = false)
	private List<OrderLine> lines;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected OrderJpaEntity() {
	}

	public OrderJpaEntity(UUID id, UUID userId, OrderStatus status, BigDecimal totalAmount, String currency,
			UUID paymentId, String failureReason, List<OrderLine> lines, Instant createdAt, Instant updatedAt) {
		this.id = id;
		this.userId = userId;
		this.status = status;
		this.totalAmount = totalAmount;
		this.currency = currency;
		this.paymentId = paymentId;
		this.failureReason = failureReason;
		this.lines = lines;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public UUID getId() { return id; }
	public UUID getUserId() { return userId; }
	public OrderStatus getStatus() { return status; }
	public BigDecimal getTotalAmount() { return totalAmount; }
	public String getCurrency() { return currency; }
	public UUID getPaymentId() { return paymentId; }
	public String getFailureReason() { return failureReason; }
	public List<OrderLine> getLines() { return lines; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
}
