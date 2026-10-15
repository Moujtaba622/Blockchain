# Partner B — Corda Ledger Module

Medical document proof recording on Corda, for the project
**"Secure interoperability between Corda and Hedera for medical document integrity verification"**.

This module implements **Partner B's responsibilities**: accept the proof metadata produced by
Partner A (Python document service), record a valid proof on Corda, retrieve it, and return
the same identifiers and document hash.

---

## 1. What this module does

- Accepts proof metadata: `case_id`, `artifact_id`, `document_hash`, `document_type`, `created_at`.
- Validates the input (required fields, SHA-256 hash format, ISO 8601 UTC timestamp).
- Records a `MedicalDocumentState` on the Corda ledger via a transaction.
- Returns the real Corda **transaction ID**.
- Retrieves a previously recorded proof by `case_id` + `artifact_id`.
- Returns a clear **not-found** result when no proof exists.

**Important:** The original medical document is **never** stored on Corda. Only the SHA-256 hash
and minimal metadata are recorded. No patient identifiers are stored.

---

## 2. Version information

| Component | Version |
|---|---|
| Corda Community Edition | 4.12 |
| Java (JDK) | 17 (Zulu or Oracle) |
| Gradle | 7.6 (via wrapper) |
| Language | Java |
| Testing | JUnit 4, Corda MockNetwork |

---

## 3. Project structure
