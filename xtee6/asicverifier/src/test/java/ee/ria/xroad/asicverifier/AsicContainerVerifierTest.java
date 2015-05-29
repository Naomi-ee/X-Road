package ee.ria.xroad.asicverifier;

import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;

import lombok.RequiredArgsConstructor;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import ee.ria.xroad.common.ExpectedCodedException;
import ee.ria.xroad.common.SystemProperties;
import ee.ria.xroad.common.TestCertUtil;
import ee.ria.xroad.common.asic.AsicContainerVerifier;
import ee.ria.xroad.common.conf.globalconf.GlobalConf;
import ee.ria.xroad.common.conf.globalconf.TestGlobalConfImpl;

import static ee.ria.xroad.common.ErrorCodes.*;

/**
 * Tests to verify correct ASiC container verifier behavior.
 */
@RunWith(Parameterized.class)
@RequiredArgsConstructor
public class AsicContainerVerifierTest {

    private final String containerFile;
    private final String errorCode;

    @Rule
    public ExpectedCodedException thrown = ExpectedCodedException.none();

    /**
     * Set up configuration.
     */
    @BeforeClass
    public static void setUpConf() {
        System.setProperty(SystemProperties.CONFIGURATION_PATH,
                "../common-util/src/test/resources/globalconf_good");
        System.setProperty(SystemProperties.CONFIGURATION_ANCHOR_FILE,
                "../common-util/src/test/resources/configuration-anchor1.xml");
        GlobalConf.reload(new TestGlobalConfImpl(false) {
            @Override
            public X509Certificate getCaCert(String instanceIdentifier,
                    X509Certificate memberCert) throws Exception {
                return TestCertUtil.getCaCert();
            }
        });
    }

    /**
     * @return test input data
     */
    @Parameters(name = "{index}: verify(\"{0}\") should throw \"{1}\"")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                // TODO {"valid-signed-message.asice", null},
                // TODO {"valid-signed-hashchain.asice", null},
                // TODO {"valid-batch-ts.asice", null},
                {"wrong-message.asice", X_INVALID_SIGNATURE_VALUE},
                {"invalid-digest.asice", X_INVALID_SIGNATURE_VALUE},
                {"invalid-signed-hashchain.asice",
                    X_MALFORMED_SIGNATURE + "." + X_INVALID_HASH_CHAIN_REF},
                /* TODO {"invalid-hashchain-modified-message.asice",
                    X_MALFORMED_SIGNATURE + "." + X_HASHCHAIN_UNUSED_INPUTS},*/
                // This verification actually passes, since the hash chain
                // is not verified and the signature is correct otherwise
                // TODO {"invalid-not-signed-hashchain.asice", null},
                {"invalid-incorrect-references.asice", X_MALFORMED_SIGNATURE},
                {"invalid-ts-hashchainresult.asice", X_MALFORMED_SIGNATURE}
        });
    }

    /**
     * Test to ensure container file verification result is as expected.
     * @throws Exception in case of any unexpected errors
     */
    @Test
    public void test() throws Exception {
        // TODO fix test data -- generate new messages with updated certificates
        thrown.expectError(errorCode);
        verify(containerFile);
    }

    private static void verify(String fileName) throws Exception {
        AsicContainerVerifier verifier = new AsicContainerVerifier(
                "src/test/resources/" + fileName);
        verifier.verify();
    }
}
