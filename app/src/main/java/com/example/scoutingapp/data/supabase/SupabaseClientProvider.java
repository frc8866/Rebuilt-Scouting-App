package com.example.scoutingapp.data.supabase;

import com.example.scoutingapp.BuildConfig;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;

/**
 * Java equivalent of the Kotlin SupabaseClientProvider object. The original used the
 * Supabase Kotlin SDK (Kotlin-only, no Java client) — replaced here with a plain OkHttp
 * client that talks to Supabase's PostgREST HTTP API directly. See PostgrestClient for
 * the request helpers that replace `.from(table).select()` etc.
 *
 * NOTE: this preserves the original's trust-all-certificates / hostname-verification-disabled
 * TLS configuration for identical behavior. That configuration disables certificate validation,
 * which is a security risk in production — flagging as-is per "identical functionality" request.
 */
public final class SupabaseClientProvider {

    public static final String SUPABASE_URL = BuildConfig.SUPABASE_URL;
    public static final String SUPABASE_ANON_KEY = BuildConfig.SUPABASE_ANON_KEY;

    public static final OkHttpClient okHttpClient;

    static {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) {}

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) {}

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }
                    }
            };

            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new SecureRandom());

            okHttpClient = new OkHttpClient.Builder()
                    .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0])
                    .hostnameVerifier((hostname, session) -> true)
                    .build();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private SupabaseClientProvider() {}
}
