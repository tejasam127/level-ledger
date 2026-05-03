package com.level.ledger.web;

import com.level.ledger.service.LedgerService;
import com.level.ledger.service.ReverseOutcome;
import java.math.BigDecimal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class LedgerController {

  private final LedgerService ledger;

  public LedgerController(LedgerService ledger) {
    this.ledger = ledger;
  }

  @GetMapping("/")
  public String index(Model model) {
    model.addAttribute("accounts", ledger.listAccounts());
    model.addAttribute("transfers", ledger.recentTransfers());
    model.addAttribute("audit", ledger.recentAudit());
    return "index";
  }

  @PostMapping("/accounts")
  public String createAccount(
      @RequestParam("name") String name,
      @RequestParam(value = "openingBalance", required = false) String openingBalanceRaw,
      RedirectAttributes redirect) {
    BigDecimal openingBalance = null;
    if (openingBalanceRaw != null && !openingBalanceRaw.isBlank()) {
      try {
        openingBalance = new BigDecimal(openingBalanceRaw.trim());
      } catch (NumberFormatException e) {
        redirect.addFlashAttribute("flashError", "Opening balance must be a valid number.");
        return "redirect:/";
      }
    }
    ledger
        .createAccount(name, openingBalance)
        .ifPresentOrElse(
            err -> redirect.addFlashAttribute("flashError", err),
            () -> redirect.addFlashAttribute("flashSuccess", "Account created."));
    return "redirect:/";
  }

  @PostMapping("/deposit")
  public String deposit(
      @RequestParam("accountId") long accountId,
      @RequestParam("amount") BigDecimal amount,
      RedirectAttributes redirect) {
    ledger
        .deposit(accountId, amount)
        .ifPresentOrElse(
            err -> redirect.addFlashAttribute("flashError", err),
            () -> redirect.addFlashAttribute("flashSuccess", "Deposit recorded."));
    return "redirect:/";
  }

  @PostMapping("/transfer")
  public String transfer(
      @RequestParam("fromId") long fromId,
      @RequestParam("toId") long toId,
      @RequestParam("amount") BigDecimal amount,
      RedirectAttributes redirect) {
    ledger
        .transfer(fromId, toId, amount)
        .ifPresentOrElse(
            err -> redirect.addFlashAttribute("flashError", err),
            () -> redirect.addFlashAttribute("flashSuccess", "Transfer completed."));
    return "redirect:/";
  }

  @PostMapping("/transfers/{id}/reverse")
  public String reverse(@PathVariable("id") long transferId, RedirectAttributes redirect) {
    ReverseOutcome r = ledger.reverseTransfer(transferId);
    if (r instanceof ReverseOutcome.Failed failed) {
      redirect.addFlashAttribute("flashError", failed.message());
    } else if (r instanceof ReverseOutcome.AlreadyReversed) {
      redirect.addFlashAttribute(
          "flashSuccess", "Already reversed — no further change (idempotent).");
    } else {
      redirect.addFlashAttribute("flashSuccess", "Reversal applied.");
    }
    return "redirect:/";
  }
}
