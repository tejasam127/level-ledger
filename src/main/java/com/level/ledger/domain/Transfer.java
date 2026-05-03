package com.level.ledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "transfers")
public class Transfer {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "from_account_id")
  private Account fromAccount;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "to_account_id")
  private Account toAccount;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(nullable = false)
  private boolean reversed = false;

  protected Transfer() {}

  public Transfer(Account fromAccount, Account toAccount, BigDecimal amount) {
    this.fromAccount = fromAccount;
    this.toAccount = toAccount;
    this.amount = amount;
  }

  public Long getId() {
    return id;
  }

  public Account getFromAccount() {
    return fromAccount;
  }

  public Account getToAccount() {
    return toAccount;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public boolean isReversed() {
    return reversed;
  }

  public void markReversed() {
    this.reversed = true;
  }
}
