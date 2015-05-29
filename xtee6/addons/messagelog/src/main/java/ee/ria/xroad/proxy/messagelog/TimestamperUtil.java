package ee.ria.xroad.proxy.messagelog;

import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.cmp.PKIFreeText;
import org.bouncycastle.asn1.cmp.PKIStatus;
import org.bouncycastle.asn1.tsp.TimeStampResp;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampResponse;
import org.bouncycastle.tsp.TimeStampToken;

@Slf4j
final class TimestamperUtil {

    private TimestamperUtil() {
    }

    @SuppressWarnings("unchecked")
    static TimeStampToken addSignerCertificate(
            TimeStampResponse tsResponse, X509Certificate signerCertificate)
                    throws Exception {
        CMSSignedData cms = tsResponse.getTimeStampToken().toCMSSignedData();

        List<X509Certificate> collection = Arrays.asList(signerCertificate);
        collection.addAll(cms.getCertificates().getMatches(null));

        return new TimeStampToken(CMSSignedData.replaceCertificatesAndCRLs(cms,
                new JcaCertStore(collection), cms.getAttributeCertificates(),
                cms.getCRLs()));
    }

    static InputStream makeTsRequest(TimeStampRequest req, String tspUrl)
            throws Exception {
        byte[] request = req.getEncoded();

        URL url = new URL(tspUrl);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();

        con.setDoOutput(true);
        con.setDoInput(true);
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-type", "application/timestamp-query");

        con.setRequestProperty("Content-length", String.valueOf(request.length));
        OutputStream out = con.getOutputStream();
        out.write(request);
        out.flush();

        if (con.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new RuntimeException("Received HTTP error: "
                    + con.getResponseCode() + " - " + con.getResponseMessage());
        }

        return con.getInputStream();
    }

    static TimeStampResponse getTimestampResponse(InputStream in)
            throws Exception {
        TimeStampResp response = TimeStampResp.getInstance(
                new ASN1InputStream(in).readObject());
        if (response == null) {
            throw new RuntimeException("Could not read time-stamp response");
        }

        BigInteger status = response.getStatus().getStatus();

        log.trace("getTimestampDer() - TimeStampResp.status: {}", status);

        if (!PKIStatus.granted.getValue().equals(status)
                && !PKIStatus.grantedWithMods.getValue().equals(status)) {
            PKIFreeText statusString = response.getStatus().getStatusString();

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < statusString.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append("\"" + statusString.getStringAt(i) + "\"");
            }

            log.error("getTimestampDer() - TimeStampResp.status is not "
                    + "\"granted\" neither \"grantedWithMods\": {}, {}",
                    status, sb);
            throw new RuntimeException("TimeStampResp.status: " + status
                    + ", .statusString: " + sb);
        }

        return new TimeStampResponse(response);
    }
}
