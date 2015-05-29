package ee.ria.xroad.signer.util;

import java.util.HashMap;
import java.util.Map;

import ee.ria.xroad.common.util.CryptoUtils;

final class DigestPrefixCache {

    private static final byte[] SHA1_DIGEST_PREFIX = new byte[] {
        0x30, 0x21, 0x30, 0x09, 0x06, 0x05, 0x2b,
        0x0e, 0x03, 0x02, 0x1a, 0x05, 0x00, 0x04, 0x14};

    private static final byte[] SHA224_DIGEST_PREFIX = new byte[] {
        0x30, 0x2d, 0x30, 0x0d, 0x06, 0x09, 0x60, (byte) 0x86, 0x48,
        0x01, 0x65, 0x03, 0x04, 0x02, 0x04, 0x05, 0x00, 0x04, 0x1c};

    private static final byte[] SHA256_DIGEST_PREFIX = new byte[] {
        0x30, 0x31, 0x30, 0x0d, 0x06, 0x09, 0x60, (byte) 0x86, 0x48,
        0x01, 0x65, 0x03, 0x04, 0x02, 0x01, 0x05, 0x00, 0x04, 0x20};

    private static final byte[] SHA384_DIGEST_PREFIX = new byte[] {
        0x30, 0x41, 0x30, 0x0d, 0x06, 0x09, 0x60, (byte) 0x86, 0x48,
        0x01, 0x65, 0x03, 0x04, 0x02, 0x02, 0x05, 0x00, 0x04, 0x30};

    private static final byte[] SHA512_DIGEST_PREFIX = new byte[] {
        0x30, 0x51, 0x30, 0x0d, 0x06, 0x09, 0x60, (byte) 0x86, 0x48,
        0x01, 0x65, 0x03, 0x04, 0x02, 0x03, 0x05, 0x00, 0x04, 0x40};

    private DigestPrefixCache() {
    }

    /**
     * DigestInfo ::= SEQUENCE {
     *      digestAlgorithm AlgorithmIdentifier,
     *      digest OCTET STRING
     * }
     */
    private static final Map<Integer, byte[]> CACHE =
            new HashMap<Integer, byte[]>() {
        {
            put(CryptoUtils.SHA1_DIGEST_LENGTH, SHA1_DIGEST_PREFIX);
            put(CryptoUtils.SHA224_DIGEST_LENGTH, SHA224_DIGEST_PREFIX);
            put(CryptoUtils.SHA256_DIGEST_LENGTH, SHA256_DIGEST_PREFIX);
            put(CryptoUtils.SHA384_DIGEST_LENGTH, SHA384_DIGEST_PREFIX);
            put(CryptoUtils.SHA512_DIGEST_LENGTH, SHA512_DIGEST_PREFIX);
        }
    };

    static byte[] getPrefix(byte[] digest) {
        if (CACHE.containsKey(digest.length)) {
            return CACHE.get(digest.length);
        }

        throw new RuntimeException("Invalid digest length: " + digest.length);
    }
}
