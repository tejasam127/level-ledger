package com.level.ledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "audit_log")
public class AuditLog {

  public enum OperationType {
    TRANSFER,
    TRANSFER_REVERSE,
    ACCOUNT_CREATE,
    DEPOSIT,
  }

  public enum Outcome {
    SUCCESS,
    FAILURE,
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "operation_type", nullable = false, length = 64)
  private OperationType operationType;

  @Column(name = "from_account_id")
  private Long fromAccountId;

  @Column(name = "to_account_id")
  private Long toAccountId;

  @Column(precision = 19, scale = 2)
  private BigDecimal amount;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private Outcome outcome;

  @Column(length = 1024)
  private String detail;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "transfer_id")
  private Transfer transfer;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  protected AuditLog() {}

  public static AuditLog entry(
      OperationType type,
      Long fromId,
      Long toId,
      BigDecimal amount,
      Outcome outcome,
      String detail,
      Transfer transfer) {
    AuditLog e = new AuditLog();
    e.operationType = type;
    e.fromAccountId = fromId;
    e.toAccountId = toId;
    e.amount = amount;
    e.outcome = outcome;
    e.detail = detail;
    e.transfer = transfer;
    return e;
  }

  public Long getId() {
    return id;
  }

  public OperationType getOperationType() {
    return operationType;
  }

  public Long getFromAccountId() {
    return fromAccountId;
  }

  public Long getToAccountId() {
    return toAccountId;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public Outcome getOutcome() {
    return outcome;
  }

  public String getDetail() {
    return detail;
  }

  public Transfer getTransfer() {
    return transfer;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }
}
