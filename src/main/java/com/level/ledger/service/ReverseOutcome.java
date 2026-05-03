package com.level.ledger.service;

public sealed interface ReverseOutcome {

  record Applied() implements ReverseOutcome {}

  record AlreadyReversed() implements ReverseOutcome {}

  record Failed(String message) implements ReverseOutcome {}
}
