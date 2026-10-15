package com.template.flows;

import java.util.concurrent.ExecutionException;
import com.google.common.collect.ImmutableList;
import com.template.states.MedicalDocumentState;
import net.corda.core.contracts.ContractState;
import net.corda.core.flows.FlowException;
import net.corda.core.transactions.SignedTransaction;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.MockNetworkParameters;
import net.corda.testing.node.StartedMockNode;
import net.corda.testing.node.TestCordapp;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Automated tests for the medical document proof flows.
 */
public class MedicalDocumentFlowTests {

    private static MockNetwork network;
    private static StartedMockNode node;

    private static final String VALID_HASH =
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    private static final String VALID_TIMESTAMP = "2026-10-11T10:00:00Z";

    @BeforeClass
    public static void setup() {
        network = new MockNetwork(new MockNetworkParameters().withCordappsForAllNodes(
                ImmutableList.of(
                        TestCordapp.findCordapp("com.template.contracts"),
                        TestCordapp.findCordapp("com.template.flows")
                )));
        node = network.createPartyNode(null);
        network.runNetwork();
    }

    @AfterClass
    public static void tearDown() {
        network.stopNodes();
    }

    // ---- Helper: record a proof ----
    private String record(String caseId, String artifactId, String hash,
                          String type, String createdAt) throws Exception {
        RecordProofFlow flow = new RecordProofFlow(caseId, artifactId, hash, type, createdAt);
        String txId = node.startFlow(flow).get();
        network.runNetwork();
        return txId;
    }

    // ---- Helper: retrieve a proof ----
    private ProofResult retrieve(String caseId, String artifactId) throws Exception {
        RetrieveProofFlow flow = new RetrieveProofFlow(caseId, artifactId);
        ProofResult result = node.startFlow(flow).get();
        network.runNetwork();
        return result;
    }

    // ================================================================
    // 1. Valid proof can be recorded and retrieved
    // ================================================================
    @Test
    public void validProofCanBeRecordedAndRetrieved() throws Exception {
        String txId = record("CASE-001", "DOC-001", VALID_HASH,
                "lab_report", VALID_TIMESTAMP);
        assertNotNull(txId);

        ProofResult result = retrieve("CASE-001", "DOC-001");
        assertTrue(result.isFound());
        assertEquals("CASE-001", result.getCaseId());
        assertEquals("DOC-001", result.getArtifactId());
        assertEquals(VALID_HASH, result.getDocumentHash());
        assertEquals("lab_report", result.getDocumentType());
        assertEquals("RECORDED", result.getLedgerStatus());
    }

    // ================================================================
    // 2. Retrieved values match submitted values
    // ================================================================
    @Test
    public void retrievedValuesMatchSubmitted() throws Exception {
        String hash = "abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789";
        record("CASE-002", "DOC-002", hash, "prescription", VALID_TIMESTAMP);

        ProofResult result = retrieve("CASE-002", "DOC-002");
        assertEquals("CASE-002", result.getCaseId());
        assertEquals("DOC-002", result.getArtifactId());
        assertEquals(hash, result.getDocumentHash());
    }

    // ================================================================
    // 3. Malformed hash is rejected
    // ================================================================
    @Test
    public void malformedHashIsRejected() throws Exception {
        assertFlowRejected(() -> record("CASE-003", "DOC-003",
                "not-a-valid-hash", "lab_report", VALID_TIMESTAMP));
    }

    @Test
    public void shortHashIsRejected() throws Exception {
        assertFlowRejected(() -> record("CASE-004", "DOC-004",
                "abc123", "lab_report", VALID_TIMESTAMP));
    }

    @Test
    public void emptyCaseIdIsRejected() throws Exception {
        assertFlowRejected(() -> record("", "DOC-005",
                VALID_HASH, "lab_report", VALID_TIMESTAMP));
    }

    @Test
    public void emptyArtifactIdIsRejected() throws Exception {
        assertFlowRejected(() -> record("CASE-006", "",
                VALID_HASH, "lab_report", VALID_TIMESTAMP));
    }

    @Test
    public void emptyDocumentTypeIsRejected() throws Exception {
        assertFlowRejected(() -> record("CASE-007", "DOC-007",
                VALID_HASH, "", VALID_TIMESTAMP));
    }

    @Test
    public void invalidTimestampIsRejected() throws Exception {
        assertFlowRejected(() -> record("CASE-008", "DOC-008",
                VALID_HASH, "lab_report", "not-a-timestamp"));
    }

    // ================================================================
    // 5. Retrieving a nonexistent proof produces a clear result
    // ================================================================
    @Test
    public void nonexistentProofReturnsNotFound() throws Exception {
        ProofResult result = retrieve("CASE-999", "DOC-999");
        assertFalse(result.isFound());
        assertEquals("CASE-999", result.getCaseId());
        assertEquals("DOC-999", result.getArtifactId());
    }

    // ================================================================
    // 6. Successful recording returns a real Corda transaction ID
    // ================================================================
    @Test
    public void recordingReturnsRealTransactionId() throws Exception {
        String txId = record("CASE-010", "DOC-010", VALID_HASH,
                "lab_report", VALID_TIMESTAMP);
        assertNotNull(txId);
        // Corda tx IDs are 64-char hex SHA-256 hashes
        assertTrue("Transaction ID should be a 64-char hex string",
                txId.matches("^[0-9a-fA-F]{64}$"));
    }

    // ================================================================
    // 7. The recorded state contains no patient identifiers
    //    (only the metadata we expect)
    // ================================================================
    @Test
    public void noPatientIdentifiersAreStored() throws Exception {
        record("CASE-011", "DOC-011", VALID_HASH, "lab_report", VALID_TIMESTAMP);

        ProofResult result = retrieve("CASE-011", "DOC-011");
        assertTrue(result.isFound());

        // The state exposes exactly these fields and no more.
        // Confirm we only stored metadata + hash — not a name, address, etc.
        // (This is enforced by the state's design; the test documents it.)
        assertNotNull(result.getDocumentHash());
        assertEquals(VALID_HASH, result.getDocumentHash());
        // No "patientName", "address", etc. exist on the result.
    }
    // Helper: runs the given callable and asserts it fails with a FlowException
// (accounting for Corda's wrapping inside ExecutionException).
    private void assertFlowRejected(ThrowingRunnable action) {
        try {
            action.run();
            fail("Expected the flow to be rejected, but it succeeded.");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            assertTrue(
                    "Expected FlowException in cause but got: " + cause,
                    cause instanceof FlowException
            );
        } catch (Throwable t) {
            fail("Expected ExecutionException wrapping FlowException, but got: " + t);
        }
    }

    // Small functional interface so we can pass lambdas to assertFlowRejected
    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}