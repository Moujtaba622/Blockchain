package com.template.flows;

import net.corda.core.node.StatesToRecord;
import co.paralleluniverse.fibers.Suspendable;
import com.template.contracts.MedicalDocumentContract;
import com.template.states.MedicalDocumentState;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.TransactionState;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

/**
 * Flow that records a new medical document proof on the Corda ledger.
 *
 * Input:  the proof metadata (case_id, artifact_id, document_hash, ...)
 * Output: the Corda transaction ID and confirmation
 */
@StartableByRPC
public class RecordProofFlow extends FlowLogic<String> {

    // ---- Inputs ----
    private final String caseId;
    private final String artifactId;
    private final String documentHash;
    private final String documentType;
    private final String createdAt;

    public RecordProofFlow(String caseId,
                           String artifactId,
                           String documentHash,
                           String documentType,
                           String createdAt) {
        this.caseId = caseId;
        this.artifactId = artifactId;
        this.documentHash = documentHash;
        this.documentType = documentType;
        this.createdAt = createdAt;
    }

    @Suspendable
    @Override
    public String call() throws FlowException {

        // ---- 0. Validate input BEFORE building the transaction ----
        // (Contract validates too, but failing fast gives clearer errors.)
        if (caseId == null || caseId.trim().isEmpty())
            throw new FlowException("case_id must not be empty");
        if (artifactId == null || artifactId.trim().isEmpty())
            throw new FlowException("artifact_id must not be empty");
        if (documentHash == null || !documentHash.matches("^[0-9a-fA-F]{64}$"))
            throw new FlowException("document_hash must be a 64-character hex SHA-256 hash");
        if (documentType == null || documentType.trim().isEmpty())
            throw new FlowException("document_type must not be empty");
        if (createdAt == null || !createdAt.matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z$"))
            throw new FlowException("created_at must be a valid ISO 8601 UTC timestamp");

        // ---- 1. Who is recording this proof? ----
        Party me = getOurIdentity();

        // ---- 2. Create the state ----
        MedicalDocumentState state = new MedicalDocumentState(
                caseId,
                artifactId,
                documentHash,
                documentType,
                createdAt,
                "RECORDED",   // Corda-side ledger status
                me
        );

        // ---- 3. Build the transaction ----
        // Notary: the node that finalises the transaction
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addOutputState(state, MedicalDocumentContract.ID)
                .addCommand(
                        new Command<>(new MedicalDocumentContract.Commands.Create(),
                                Collections.singletonList(me.getOwningKey()))
                );

        // ---- 4. Verify the transaction against the contract ----
        txBuilder.verify(getServiceHub());

        // ---- 5. Sign the transaction ----
        SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);

        // ---- 6. Record it on the ledger ----
        getServiceHub().recordTransactions(
                StatesToRecord.ONLY_RELEVANT,
                Collections.singletonList(signedTx)
        );

        // ---- 7. Return the transaction ID ----
        return signedTx.getId().toString();
    }
}