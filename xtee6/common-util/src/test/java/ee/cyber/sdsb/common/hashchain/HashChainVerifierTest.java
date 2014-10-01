package ee.cyber.sdsb.common.hashchain;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.crypto.dsig.DigestMethod;

import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ee.cyber.sdsb.common.util.ExpectedCodedException;

import static ee.cyber.sdsb.common.ErrorCodes.*;
import static ee.cyber.sdsb.common.util.CryptoUtils.calculateDigest;
import static ee.cyber.sdsb.common.util.CryptoUtils.getAlgorithmId;
import static ee.cyber.sdsb.common.util.MessageFileNames.MESSAGE;
import static ee.cyber.sdsb.common.util.MessageFileNames.attachment;

public class HashChainVerifierTest {
    private static final Logger LOG = LoggerFactory.getLogger(
            HashChainVerifierTest.class);

    private static final String HASH_CHAIN = "/hashchain.xml";

    @Rule
    public ExpectedCodedException thrown = ExpectedCodedException.none();

    @Test
    /** Simple test case, input is detached. */
    public void simpleCorrect() throws Exception {
        LOG.info("simpleCorrect()");

        Resolver resolver = new Resolver(
                HASH_CHAIN, "hc-verifier1-hashchain.xml",
                MESSAGE, "hc-verifier1-message.xml");

        Map<String, DigestValue> inputs = makeInputs(
                MESSAGE, null);

        HashChainVerifier.verify(
                load("hc-verifier1-hashchainresult.xml"),
                resolver, inputs);
    }

    @Test
    /** Simple test case, input is attached. */
    public void simpleAttachedInput() throws Exception {
        LOG.info("simpleAttachedInput()");

        Resolver resolver = new Resolver(
                HASH_CHAIN, "hc-verifier1-hashchain.xml");

        Map<String, DigestValue> inputs = makeInputs(
                MESSAGE, "hc-verifier1-message.xml");

        HashChainVerifier.verify(
                load("hc-verifier1-hashchainresult.xml"),
                resolver, inputs);
    }

    @Test
    /** Simple test case with unused inputs. */
    public void simpleUnusedInputs() throws Exception {
        LOG.info("simpleUnusedInputs()");

        thrown.expectErrorSuffix(X_HASHCHAIN_UNUSED_INPUTS);

        Resolver resolver = new Resolver(
                HASH_CHAIN, "hc-verifier1-hashchain.xml");

        Map<String, DigestValue> inputs = makeInputs(
                MESSAGE, "hc-verifier1-message.xml",
                "/unused1", null,
                "/unused2", null);

        HashChainVerifier.verify(
                load("hc-verifier1-hashchainresult.xml"),
                resolver, inputs);
    }

    @Test
    /**
     * Simple test case, hash chain result does not match
     * the result of hash chain calculation.
     */
    public void simpleDigestMismatch() throws Exception {
        LOG.info("simpleDigestMismatch()");

        thrown.expectErrorSuffix(X_INVALID_HASH_CHAIN_RESULT);

        Resolver resolver = new Resolver(
                HASH_CHAIN, "hc-verifier1-hashchain.xml");

        Map<String, DigestValue> inputs = makeInputs(
                MESSAGE, "hc-verifier1-hashchain.xml");

        HashChainVerifier.verify(
                load("hc-verifier1-hashchainresult.xml"),
                resolver, inputs);
    }

    @Test
    /** Simple test case, hash chain is split into two files. */
    public void simpleTwoHashChains() throws Exception {
        LOG.info("simpleTwoHashChains()");

        Resolver resolver = new Resolver(
                HASH_CHAIN, "hc-verifier4-hashchain1.xml",
                "/hashchain2.xml", "hc-verifier4-hashchain2.xml",
                MESSAGE, "hc-verifier4-message.xml");

        Map<String, DigestValue> inputs = makeInputs(
                MESSAGE, null);

        HashChainVerifier.verify(
                load("hc-verifier4-hashchainresult.xml"),
                resolver, inputs);
    }

    @Test
    public void attachments() throws Exception {
        LOG.info("attachments()");

        Resolver resolver = new Resolver(
                HASH_CHAIN, "hc-verifier2-hashchain.xml");

        Map<String, DigestValue> inputs = makeInputs(
                MESSAGE, new DigestValue(
                    DigestMethod.SHA256, new byte[] { (byte) 11 }),
                attachment(1), new DigestValue(
                    DigestMethod.SHA256, new byte[] { (byte) 12 }),
                attachment(2), new DigestValue(
                    DigestMethod.SHA256, new byte[] { (byte) 13 }),
                attachment(3), new DigestValue(
                    DigestMethod.SHA256, new byte[] { (byte) 14 }));

        HashChainVerifier.verify(
                load("hc-verifier2-hashchainresult.xml"),
                resolver, inputs);
    }

    @Test
    public void attachmentsCannotResolve() throws Exception {
        LOG.info("attachments()");

        Resolver resolver = new Resolver(
                HASH_CHAIN, "hc-verifier2-hashchain.xml") {
                    @Override
                    public boolean shouldResolve(String uri,
                            byte[] digestValue) {
                        switch (uri) {
                            case "/attachment1":
                            case "/attachment2":
                            case "/attachment3":
                                return false;
                            default:
                                return true;
                        }
                    }
        };

        Map<String, DigestValue> inputs = makeInputs(
                MESSAGE, new DigestValue(
                    DigestMethod.SHA256, new byte[] { (byte) 11 }));

        HashChainVerifier.verify(
                load("hc-verifier2-hashchainresult.xml"),
                resolver, inputs);
    }

    @Test
    public void transforms() throws Exception {
        LOG.info("transforms()");

        Resolver resolver = new Resolver(
                HASH_CHAIN, "hc-verifier3-hashchain.xml",
                MESSAGE, "hc-verifier3-message.xml");

        Map<String, DigestValue> inputs = makeInputs(
                MESSAGE, null);

        HashChainVerifier.verify(
                load("hc-verifier3-hashchainresult.xml"),
                resolver, inputs);
    }

    @Test
    public void transformsError() throws Exception {
        LOG.info("transformsError()");

        Resolver resolver = new Resolver(
                HASH_CHAIN, "hc-verifier3-hashchain-invalid.xml",
                MESSAGE, "hc-verifier3-message.xml");

        Map<String, DigestValue> inputs = makeInputs(
                MESSAGE, null);

        thrown.expectErrorSuffix(X_INVALID_HASH_CHAIN_REF);

        HashChainVerifier.verify(
                load("hc-verifier3-hashchainresult.xml"),
                resolver, inputs);
    }

    private static Map<String, DigestValue> makeInputs(Object ...items)
            throws Exception {
        Map<String, DigestValue> ret = new HashMap<>();

        for (int i = 0; i < items.length; i += 2) {
            String uri = (String) items[i];
            if (items[i + 1] == null) {
                ret.put(uri, null);
            } else if (items[i + 1] instanceof String) {
                String fileName = (String) items[i + 1];
                ret.put(uri,
                        new DigestValue(
                                DigestMethod.SHA256,
                                calculateDigest(
                                        getAlgorithmId(DigestMethod.SHA256),
                                        load(fileName))));
            } else if (items[i + 1] instanceof DigestValue) {
                ret.put(uri, (DigestValue) items[i + 1]);
            } else {
                byte[] data = (byte[]) items[i + 1];
                ret.put(uri,
                        new DigestValue(
                                DigestMethod.SHA256,
                                calculateDigest(
                                        getAlgorithmId(DigestMethod.SHA256),
                                        data)));
            }
        }

        return ret;
    }

    private static class Resolver implements HashChainReferenceResolver {

        private final Map<String, String> resources = new HashMap<>();

        Resolver(String ...items) {
            for (int i = 0; i < items.length; i += 2) {
                resources.put(items[i], items[i + 1]);
            }
        }

        @Override
        public InputStream resolve(String uri) {
            LOG.debug("resolve({})", uri);
            if (resources.containsKey(uri)) {
                String fileName = resources.get(uri);
                LOG.debug("Returning file {}", fileName);
                return load(fileName);
            } else {
                throw new IllegalArgumentException("Invalid URI: " + uri);
            }
        }

        @Override
        public boolean shouldResolve(String uri, byte[] digestValue) {
            LOG.debug("shouldResolve({})", uri);
            return true;
        }
    }

    private static InputStream load(String fileName) {
        LOG.debug("load({})", fileName);
        return Thread.currentThread()
                .getContextClassLoader()
                    .getResourceAsStream(fileName);
    }

    static {
        org.apache.xml.security.Init.init();
    }
}