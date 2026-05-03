SELECT CASE
  WHEN EXISTS (SELECT 1 FROM accounts LIMIT 1) THEN
    setval(
      pg_get_serial_sequence('accounts', 'id'),
      (SELECT MAX(id) FROM accounts),
      true)
  ELSE
    setval(pg_get_serial_sequence('accounts', 'id'), 1, false)
  END;
