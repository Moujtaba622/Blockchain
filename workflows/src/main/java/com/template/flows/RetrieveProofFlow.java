package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.template.states.MedicalDocumentState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.node.services.Vault;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * Flow that retrieves a previously recorded medical document proof.
 *
 * Lookup key: case_id + artifact_id.
 * Returns a ProofResult, or a "not found" ProofResult if no match exists.
 */
@StartableByRPC
public class RetrieveProofFlow extends FlowLogic<ProofResult> {

    private final String caseId;
    private final String artifactId;

    public RetrieveProofFlow(String caseId, String artifactId) {
        this.caseId = caseId;
        this.artifactId = artifactId;
    }

    @Suspendable
    @Override
    public ProofResult call() throws FlowException {

        // ---- 1. Query the vault for all MedicalDocumentStates ----
        List<StateAndRef<MedicalDocumentState>> all =
                getServiceHub().getVaultService()
                        .queryBy(MedicalDocumentState.class)
                        .getStates();

        // ---- 2. Filter by case_id + artifact_id ----
        Optional<StateAndRef<MedicalDocumentState>> match = all.stream()
                .filter(sar -> {
                    MedicalDocumentState s = sar.getState().getData();
                    return s.getCaseId().equals(caseId)
                            && s.getArtifactId().equals(artifactId);
                })
                .findFirst();

        // ---- 3. If not found, return a clear not-found result ----
        if (!match.isPresent()) {
            return new ProofResult(caseId, artifactId);
        }

        // ---- 4. Extract the state and its transaction ID ----
        StateAndRef<MedicalDocumentState> ref = match.get();
        MedicalDocumentState s = ref.getState().getData();
        String txId = ref.getRef().getTxhash().toString();

        // ---- 5. Return the proof ----
        return new ProofResult(
                s.getCaseId(),
                s.getArtifactId(),
                s.getDocumentHash(),
                s.getDocumentType(),
                s.getCreatedAt(),
                s.getLedgerStatus(),
                txId
        );
    }
}