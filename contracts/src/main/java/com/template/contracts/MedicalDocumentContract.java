package com.template.contracts;

import com.template.states.MedicalDocumentState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

import static net.corda.core.contracts.ContractsDSL.requireThat;

/**
 * The contract (rulebook) for medical document proofs.
 *
 * Every transaction that creates a MedicalDocumentState must pass these rules.
 * If any rule fails, the transaction is rejected.
 */
public class MedicalDocumentContract implements Contract {

    // Contract ID — identifies this contract on the ledger
    public static final String ID = "com.template.contracts.MedicalDocumentContract";

    // A SHA-256 hash is exactly 64 hexadecimal characters
    private static final Pattern SHA256_PATTERN = Pattern.compile("^[0-9a-fA-F]{64}$");

    // ISO 8601 UTC timestamp, e.g. 2026-10-11T10:00:00Z
    private static final Pattern ISO8601_UTC_PATTERN =
            Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z$");

    @Override
    public void verify(@NotNull LedgerTransaction tx) {

        // We only care about Create commands for this contract
        for (CommandWithParties<CommandData> command : tx.getCommands()) {
            if (command.getValue() instanceof Commands.Create) {
                verifyCreate(tx);
            }
        }
    }

    private void verifyCreate(LedgerTransaction tx) {

        requireThat(require -> {

            // ---- Rule 0: exactly one output state ----
            require.using("A Create transaction must have exactly one output state.",
                    tx.getOutputs().size() == 1);

            // ---- Get the new state ----
            MedicalDocumentState state = tx.outputsOfType(MedicalDocumentState.class).get(0);

            // ---- Rule 1: case_id present and non-empty ----
            require.using("case_id must be present and non-empty.",
                    state.getCaseId() != null && !state.getCaseId().trim().isEmpty());

            // ---- Rule 2: artifact_id present and non-empty ----
            require.using("artifact_id must be present and non-empty.",
                    state.getArtifactId() != null && !state.getArtifactId().trim().isEmpty());

            // ---- Rule 3: document_hash must be exactly 64 hex chars ----
            require.using("document_hash must be a 64-character hexadecimal SHA-256 hash.",
                    state.getDocumentHash() != null
                            && SHA256_PATTERN.matcher(state.getDocumentHash()).matches());

            // ---- Rule 4: document_type present and non-empty ----
            require.using("document_type must be present and non-empty.",
                    state.getDocumentType() != null && !state.getDocumentType().trim().isEmpty());

            // ---- Rule 5: created_at must be a valid ISO 8601 UTC timestamp ----
            require.using("created_at must be a valid ISO 8601 UTC timestamp (e.g. 2026-10-11T10:00:00Z).",
                    state.getCreatedAt() != null
                            && ISO8601_UTC_PATTERN.matcher(state.getCreatedAt()).matches());

            // ---- Rule 6: ledger status must be set ----
            require.using("ledger_status must be present.",
                    state.getLedgerStatus() != null && !state.getLedgerStatus().trim().isEmpty());

            return null;
        });
    }

    // ---- Commands (actions this contract understands) ----
    public interface Commands extends CommandData {
        class Create implements Commands {}
    }
}