package com.level.ledger.service;

import com.level.ledger.domain.Account;
import com.level.ledger.domain.AuditLog;
import com.level.ledger.domain.Transfer;
import com.level.ledger.repo.AccountRepository;
import com.level.ledger.repo.AuditLogRepository;
import com.level.ledger.repo.TransferRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LedgerService {

  private final AccountRepository accounts;
  private final TransferRepository transfers;
  private final AuditLogRepository auditLogs;

  public LedgerService(
      AccountRepository accounts, TransferRepository transfers, AuditLogRepository auditLogs) {
    this.accounts = accounts;
    this.transfers = transfers;
    this.auditLogs = auditLogs;
  }

  @Transactional(readOnly = true)
  public List<Account> listAccounts() {
    return accounts.findAll();
  }

  @Transactional(readOnly = true)
  public List<Transfer> recentTransfers() {
    return transfers.findTop50ByOrderByCreatedAtDesc();
  }

  @Transactional(readOnly = true)
  public List<AuditLog> recentAudit() {
    return auditLogs.findTop100ByOrderByCreatedAtDesc();
  }

  @Transactional
  public Optional<String> createAccount(String name, BigDecimal openingBalance) {
    String n = name == null ? "" : name.trim();
    if (n.isEmpty()) {
      auditLogs.save(
          AuditLog.entry(
              AuditLog.OperationType.ACCOUNT_CREATE,
              null,
              null,
              openingBalance,
              AuditLog.Outcome.FAILURE,
              "Name is required",
              null));
      return Optional.of("Account name is required.");
    }
    BigDecimal open = openingBalance == null ? BigDecimal.ZERO : openingBalance;
    if (open.compareTo(BigDecimal.ZERO) < 0) {
      auditLogs.save(
          AuditLog.entry(
              AuditLog.OperationType.ACCOUNT_CREATE,
              null,
              null,
              open,
              AuditLog.Outcome.FAILURE,
              "Opening balance cannot be negative",
              null));
      return Optional.of("Opening balance cannot be negative.");
    }

    Account a = new Account(n, open);
    accounts.save(a);

    auditLogs.save(
        AuditLog.entry(
            AuditLog.OperationType.ACCOUNT_CREATE,
            null,
            a.getId(),
            open,
            AuditLog.Outcome.SUCCESS,
            null,
            null));
    return Optional.empty();
  }

  @Transactional
  public Optional<String> deposit(long accountId, BigDecimal amount) {
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      auditLogs.save(
          AuditLog.entry(
              AuditLog.OperationType.DEPOSIT,
              null,
              accountId,
              amount,
              AuditLog.Outcome.FAILURE,
              "Amount must be positive",
              null));
      return Optional.of("Deposit amount must be positive.");
    }

    Account a = accounts.findByIdForUpdate(accountId).orElse(null);
    if (a == null) {
      auditLogs.save(
          AuditLog.entry(
              AuditLog.OperationType.DEPOSIT,
              null,
              accountId,
              amount,
              AuditLog.Outcome.FAILURE,
              "Unknown account",
              null));
      return Optional.of("Account does not exist.");
    }

    a.adjustBalance(amount);
    auditLogs.save(
        AuditLog.entry(
            AuditLog.OperationType.DEPOSIT,
            null,
            accountId,
            amount,
            AuditLog.Outcome.SUCCESS,
            null,
            null));
    return Optional.empty();
  }

  @Transactional
  public Optional<String> transfer(long fromId, long toId, BigDecimal amount) {
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      auditLogs.save(
          AuditLog.entry(
              AuditLog.OperationType.TRANSFER,
              fromId,
              toId,
              amount,
              AuditLog.Outcome.FAILURE,
              "Amount must be positive",
              null));
      return Optional.of("Amount must be positive.");
    }
    if (fromId == toId) {
      auditLogs.save(
          AuditLog.entry(
              AuditLog.OperationType.TRANSFER,
              fromId,
              toId,
              amount,
              AuditLog.Outcome.FAILURE,
              "Source and destination must differ",
              null));
      return Optional.of("Source and destination must be different accounts.");
    }

    List<Long> ids = new ArrayList<>(List.of(fromId, toId));
    ids.sort(Comparator.naturalOrder());

    Account first = accounts.findByIdForUpdate(ids.get(0)).orElse(null);
    Account second = accounts.findByIdForUpdate(ids.get(1)).orElse(null);
    if (first == null || second == null) {
      auditLogs.save(
          AuditLog.entry(
              AuditLog.OperationType.TRANSFER,
              fromId,
              toId,
              amount,
              AuditLog.Outcome.FAILURE,
              "Unknown account",
              null));
      return Optional.of("One or both accounts do not exist.");
    }

    Account from = fromId == first.getId() ? first : second;
    Account to = toId == first.getId() ? first : second;

    if (from.getBalance().compareTo(amount) < 0) {
      auditLogs.save(
          AuditLog.entry(
              AuditLog.OperationType.TRANSFER,
              fromId,
              toId,
              amount,
              AuditLog.Outcome.FAILURE,
              "Insufficient funds",
              null));
      return Optional.of("Insufficient funds.");
    }

    from.adjustBalance(amount.negate());
    to.adjustBalance(amount);

    Transfer t = new Transfer(from, to, amount);
    transfers.save(t);

    auditLogs.save(
        AuditLog.entry(
            AuditLog.OperationType.TRANSFER,
            fromId,
            toId,
            amount,
            AuditLog.Outcome.SUCCESS,
            null,
            t));
    return Optional.empty();
  }

  @Transactional
  public ReverseOutcome reverseTransfer(long transferId) {
    Transfer t = transfers.findByIdForUpdate(transferId).orElse(null);
    if (t == null) {
      auditLogs.save(
          AuditLog.entry(
              AuditLog.OperationType.TRANSFER_REVERSE,
              null,
              null,
              null,
              AuditLog.Outcome.FAILURE,
              "Transfer not found: " + transferId,
              null));
      return new ReverseOutcome.Failed("Transfer not found.");
    }

    if (t.isReversed()) {
      auditLogs.save(
          AuditLog.entry(
              AuditLog.OperationType.TRANSFER_REVERSE,
              t.getFromAccount().getId(),
              t.getToAccount().getId(),
              t.getAmount(),
              AuditLog.Outcome.SUCCESS,
              "Already reversed (idempotent no-op)",
              t));
      return new ReverseOutcome.AlreadyReversed();
    }

    long fromId = t.getFromAccount().getId();
    long toId = t.getToAccount().getId();
    BigDecimal amount = t.getAmount();

    List<Long> ids = new ArrayList<>(List.of(fromId, toId));
    ids.sort(Comparator.naturalOrder());

    Account first = accounts.findByIdForUpdate(ids.get(0)).orElseThrow();
    Account second = accounts.findByIdForUpdate(ids.get(1)).orElseThrow();
    Account originalFrom = fromId == first.getId() ? first : second;
    Account originalTo = toId == first.getId() ? first : second;

    if (originalTo.getBalance().compareTo(amount) < 0) {
      auditLogs.save(
          AuditLog.entry(
              AuditLog.OperationType.TRANSFER_REVERSE,
              fromId,
              toId,
              amount,
              AuditLog.Outcome.FAILURE,
              "Cannot reverse: destination balance too low",
              t));
      return new ReverseOutcome.Failed(
          "Cannot reverse: recipient no longer has enough balance.");
    }

    originalTo.adjustBalance(amount.negate());
    originalFrom.adjustBalance(amount);
    t.markReversed();

    auditLogs.save(
        AuditLog.entry(
            AuditLog.OperationType.TRANSFER_REVERSE,
            fromId,
            toId,
            amount,
            AuditLog.Outcome.SUCCESS,
            null,
            t));
    return new ReverseOutcome.Applied();
  }
}
