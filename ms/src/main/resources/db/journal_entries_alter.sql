-- Alter table to add entry_type to distinguish Step 3 (AMORTIZATION) vs Step 4 (PAYMENT)
ALTER TABLE journal_entries
ADD COLUMN IF NOT EXISTS entry_type VARCHAR(20) NOT NULL DEFAULT 'AMORTIZATION' COMMENT '分录类型: AMORTIZATION(摊销)/PAYMENT(付款)';

-- Optional: backfill existing rows if needed (example, set PAYMENT for rows linked to a payment)
UPDATE journal_entries je
LEFT JOIN payments p ON je.payment_id = p.id
SET je.entry_type = CASE WHEN p.id IS NOT NULL THEN 'PAYMENT' ELSE 'AMORTIZATION' END;
