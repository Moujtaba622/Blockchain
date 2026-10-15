package com.template.flows;

/**
 * The result of a proof retrieval.
 * Mirrors the response structure required by the integration spec.
 */
public class ProofResult {

    private final boolean found;
    private final String caseId;
    private final String artifactId;
    private final String documentHash;
    private final String documentType;
    private final String createdAt;
    private final String ledgerStatus;
    private final String transactionId;

    // Constructor for a successful lookup
    public ProofResult(String caseId, String artifactId, String documentHash,
                       String documentType, String createdAt, String ledgerStatus,
                       String transactionId) {
        this.found = true;
        this.caseId = caseId;
        this.artifactId = artifactId;
        this.documentHash = documentHash;
        this.documentType = documentType;
        this.createdAt = createdAt;
        this.ledgerStatus = ledgerStatus;
        this.transactionId = transactionId;
    }

    // Constructor for "not found"
    public ProofResult(String caseId, String artifactId) {
        this.found = false;
        this.caseId = caseId;
        this.artifactId = artifactId;
        this.documentHash = null;
        this.documentType = null;
        this.createdAt = null;
        this.ledgerStatus = null;
        this.transactionId = null;
    }

    public boolean isFound() { return found; }
    public String getCaseId() { return caseId; }
    public String getArtifactId() { return artifactId; }
    public String getDocumentHash() { return documentHash; }
    public String getDocumentType() { return documentType; }
    public String getCreatedAt() { return createdAt; }
    public String getLedgerStatus() { return ledgerStatus; }
    public String getTransactionId() { return transactionId; }

    @Override
    public String toString() {
        if (!found) {
            return "ProofResult{found=false, caseId='" + caseId + "', artifactId='" + artifactId + "'}";
        }
        return "ProofResult{" +
                "caseId='" + caseId + '\'' +
                ", artifactId='" + artifactId + '\'' +
                ", documentHash='" + documentHash + '\'' +
                ", documentType='" + documentType + '\'' +
                ", createdAt='" + createdAt + '\'' +
                ", ledgerStatus='" + ledgerStatus + '\'' +
                ", transactionId='" + transactionId + '\'' +
                '}';
    }
}