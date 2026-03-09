package com.identitymodulith.common.security;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * 자체 서명 인증서 생성 유틸리티 (개발/테스트용)
 */
public class SelfSignedCertificateGenerator {

    /**
     * RSA 키 쌍으로 자체 서명 X.509 인증서 생성
     *
     * @param keyPair RSA 키 쌍
     * @return 자체 서명된 X.509 인증서
     */
    public static X509Certificate generate(KeyPair keyPair) throws Exception {
        long now = System.currentTimeMillis();
        Date startDate = new Date(now);
        Date endDate = new Date(now + 365L * 24 * 60 * 60 * 1000); // 1년 유효

        X500Name dnName = new X500Name("CN=localhost, O=Identity-Modulith, C=KR");
        BigInteger serialNumber = BigInteger.valueOf(now);

        JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
            dnName,
            serialNumber,
            startDate,
            endDate,
            dnName,
            keyPair.getPublic()
        );

        ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256WithRSA")
            .build(keyPair.getPrivate());

        X509CertificateHolder certHolder = certBuilder.build(contentSigner);
        return new JcaX509CertificateConverter().getCertificate(certHolder);
    }
}

