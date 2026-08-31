INSERT INTO clients (client_id)
VALUES
	('CLIENT-001'),
	('CLIENT-002');

INSERT INTO balances (client_id, currency, amount)
SELECT id, 'USD', 10000.00
FROM clients
WHERE client_id = 'CLIENT-001';

INSERT INTO balances (client_id, currency, amount)
SELECT id, 'EUR', 8000.00
FROM clients
WHERE client_id = 'CLIENT-001';

INSERT INTO balances (client_id, currency, amount)
SELECT id, 'GBP', 5000.00
FROM clients
WHERE client_id = 'CLIENT-002';