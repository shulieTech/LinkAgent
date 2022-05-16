//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package swiftsdk.http;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;

public class HttpClientFactory {
    private static CloseableHttpClient httpclient = setHttpClient();
    private static CloseableHttpClient httpsclient = setHttpsClient();

    public HttpClientFactory() {
    }

    private static CloseableHttpClient setHttpClient() {
        int socketTimeout = 15000;
        int connectTimeout = 15000;
        SocketConfig socketConfig = SocketConfig.custom().setSoKeepAlive(false).setSoLinger(1).setSoReuseAddress(true).setSoTimeout(10000).setTcpNoDelay(true).build();
        RequestConfig config = RequestConfig.custom().setConnectTimeout(connectTimeout).setSocketTimeout(socketTimeout).setConnectionRequestTimeout(connectTimeout).build();
        CloseableHttpClient httpClient = HttpClientBuilder.create().setDefaultSocketConfig(socketConfig).setDefaultRequestConfig(config).build();
        return httpClient;
    }

    public static CloseableHttpClient getHttpClient() {
        return httpclient;
    }

    public static CloseableHttpClient getHttpsClient() {
        return httpsclient;
    }

    private static CloseableHttpClient setHttpsClient() {
        try {
            return creatHttpsClient();
        } catch (Exception var1) {
            return null;
        }
    }

    private static CloseableHttpClient creatHttpsClient() throws Exception {
        new ServerCrt();
        CertificateFactory cAf = CertificateFactory.getInstance("X.509");
        KeyStore caKs = KeyStore.getInstance("JKS");
        caKs.load((InputStream)null, (char[])null);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX");
        tmf.init(caKs);
        SSLContext context = SSLContext.getInstance("TLSv1");
        context.init((KeyManager[])null, tmf.getTrustManagers(), new SecureRandom());
        SSLConnectionSocketFactory sslSf = new SSLConnectionSocketFactory(context, new String[]{"TLSv1", "TLSv1.1", "TLSv1.2"}, (String[])null, SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
        return HttpClients.custom().setSSLSocketFactory(sslSf).build();
    }
}
