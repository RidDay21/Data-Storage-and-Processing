package ru.nsu.laptev;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Base64;

public record KeyData(KeyPair keyPair, X509Certificate certificate) {
    public String toPem() throws Exception {
        return privateKeyToPem() + "\n" + publicKeyToPem() + "\n" + certToPem() + "\n";
    }

    private String privateKeyToPem() {
        String encoded = Base64.getMimeEncoder(64, "\n".getBytes())
                .encodeToString(keyPair.getPrivate().getEncoded());
        return "-----BEGIN PRIVATE KEY-----\n" + encoded + "\n-----END PRIVATE KEY-----";
    }

    private String publicKeyToPem() {
        String encoded = Base64.getMimeEncoder(64, "\n".getBytes())
                .encodeToString(keyPair.getPublic().getEncoded());
        return "-----BEGIN PUBLIC KEY-----\n" + encoded + "\n-----END PUBLIC KEY-----";
    }

    private String certToPem() throws Exception {
        String encoded = Base64.getMimeEncoder(64, "\n".getBytes())
                .encodeToString(certificate.getEncoded());
        return "-----BEGIN CERTIFICATE-----\n" + encoded + "\n-----END CERTIFICATE-----";
    }
}









//openssl genrsa -out secret/ca_private_key.pem 4096