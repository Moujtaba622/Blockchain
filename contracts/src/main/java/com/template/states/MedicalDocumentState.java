package com.template.states;

import net.corda.core.contracts.BelongsToContract;
import com.template.contracts.MedicalDocumentContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * A Corda state representing a medical document proof.
 *
 * The original medical document is NEVER stored here.
 * Only the SHA-256 hash and minimal metadata are recorded on the ledger.
 */
@BelongsToContract(MedicalDocumentContract.class)
public class MedicalDocumentState implements ContractState {

    private final String caseId;
    private final String artifactId;
    private final String documentHash;
    private final String documentType;
    private final String createdAt;
    private final String ledgerStatus;
    private final Party owner;
    private final UniqueIdentifier linearId;

    public MedicalDocumentState(String caseId,
                                String artifactId,
                                String documentHash,
                                String documentType,
                                String createdAt,
                                String ledgerStatus,
                                Party owner) {
        this.caseId = caseId;
        this.artifactId = artifactId;
        this.documentHash = documentHash;
        this.documentType = documentType;
        this.createdAt = createdAt;
        this.ledgerStatus = ledgerStatus;
        this.owner = owner;
        this.linearId = new UniqueIdentifier();
    }

    public String getCaseId() { return caseId; }
    public String getArtifactId() { return artifactId; }
    public String getDocumentHash() { return documentHash; }
    public String getDocumentType() { return documentType; }
    public String getCreatedAt() { return createdAt; }
    public String getLedgerStatus() { return ledgerStatus; }
    public Party getOwner() { return owner; }
    public UniqueIdentifier getLinearId() { return linearId; }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return Collections.singletonList(owner);
    }
}