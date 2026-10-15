package net.newpipe.app;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Permissive SSL helper designed for legacy Windows 7 / Canaima netbooks.
 * On devices where the CMOS RTC battery is dead or discharged, the system clock
 * commonly resets to 2010 or 2011, triggering CertificateNotYetValidException
 * on modern Google and YouTube SSL certificates.
 */
public final class SslHelper {
    private static volatile SSLSocketFactory sslSocketFactory;
    private static volatile HostnameVerifier hostnameVerifier;
    private static volatile boolean initialized = false;

    private SslHelper() {
    }

    public static synchronized void installPermissiveSsl() {
        if (initialized) {
            return;
        }
        try {
            TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }

                    @Override
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        // Accept certificates regardless of local system clock skew.
                    }
                }
            };

            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new SecureRandom());
            sslSocketFactory = sc.getSocketFactory();
            hostnameVerifier = (String hostname, SSLSession session) -> true;

            HttpsURLConnection.setDefaultSSLSocketFactory(sslSocketFactory);
            HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier);
            initialized = true;
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            System.err.println("Failed to install permissive SSL: " + e.getMessage());
        }
    }

    public static SSLSocketFactory getSocketFactory() {
        if (!initialized) {
            installPermissiveSsl();
        }
        return sslSocketFactory;
    }

    public static HostnameVerifier getHostnameVerifier() {
        if (!initialized) {
            installPermissiveSsl();
        }
        return hostnameVerifier;
    }
}
