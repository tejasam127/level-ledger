package com.level.ledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "accounts")
public class Account {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal balance;

  protected Account() {}

  public Account(String name, BigDecimal balance) {
    this.name = name;
    this.balance = balance;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public BigDecimal getBalance() {
    return balance;
  }

  public void adjustBalance(BigDecimal delta) {
    this.balance = this.balance.add(delta);
  }
}
