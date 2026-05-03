DELETE FROM audit_log al
WHERE al.transfer_id IN (
  SELECT t.id
  FROM transfers t
  WHERE t.from_account_id IN (
      SELECT id FROM accounts
      WHERE name IN ('Checking — Alice', 'Savings — Bob', 'Ops — float'))
     OR t.to_account_id IN (
      SELECT id FROM accounts
      WHERE name IN ('Checking — Alice', 'Savings — Bob', 'Ops — float')));

DELETE FROM transfers t
WHERE t.from_account_id IN (
    SELECT id FROM accounts
    WHERE name IN ('Checking — Alice', 'Savings — Bob', 'Ops — float'))
   OR t.to_account_id IN (
    SELECT id FROM accounts
    WHERE name IN ('Checking — Alice', 'Savings — Bob', 'Ops — float'));

DELETE FROM audit_log
WHERE from_account_id IN (
    SELECT id FROM accounts
    WHERE name IN ('Checking — Alice', 'Savings — Bob', 'Ops — float'))
   OR to_account_id IN (
    SELECT id FROM accounts
    WHERE name IN ('Checking — Alice', 'Savings — Bob', 'Ops — float'));

DELETE FROM accounts
WHERE name IN ('Checking — Alice', 'Savings — Bob', 'Ops — float');
