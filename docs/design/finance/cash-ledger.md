# Payment accounts and cash ledger

Expenses keep their budget kind and use FinanceRecord.account to identify the payment account. DEBIT and CASH expenses reduce balance; CARD expenses increase debt. Blank accounts remain unspecified for legacy/manual records and do not change account balances.

WITHDRAWAL is neither income nor expense. Its account is the debit source; its destination is the reserved CASH account wallet:cash, created on first use. A withdrawal decreases debit and increases cash equally. Bank totals include cash, so net money stays unchanged. Cash purchases are expenses only when registered separately.

Add/update/delete run under the existing Room transaction. An edit reverses the prior movement before applying the new one; invalid withdrawal sources throw and roll back. Accounts with linked movements cannot be deleted, including the wallet with inbound withdrawals. Credit cash advances are not represented by WITHDRAWAL and cannot be selected as sources.

Enums are stored as strings by Room; no column migration is needed. Existing names and records are retained. Backup JSON preserves the new kind and cash account using the existing name-based serialization. Notification parsing was not changed.

Verification: JVM tests cover account selection, debit-to-cash conservation, credit isolation, invalid sources, edit/delete reversal, and budget exclusion. DebugFinanceChecks additionally contains a real Room close/reopen and atomic rollback check, pending execution because the emulator is offline. See cash-flow-evidence.json for native limitations.
