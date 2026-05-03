package com.level.ledger.web;

import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public String handleTypeMismatch(
      MethodArgumentTypeMismatchException ex, RedirectAttributes redirect) {
    redirect.addFlashAttribute(
        "flashError",
        "Invalid value for \""
            + ex.getName()
            + "\". Use a number like 10.00 (no letters or symbols).");
    return "redirect:/";
  }

  @ExceptionHandler(DataAccessException.class)
  public String handleDb(DataAccessException ex, RedirectAttributes redirect) {
    redirect.addFlashAttribute(
        "flashError",
        "Database error (is Postgres running on port 5433?). Check the server log for details.");
    return "redirect:/";
  }
}
