//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package swiftsdk.http;

import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import swiftsdk.errors.HttpException;
import swiftsdk.util.PropertiesUtil;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

public class SFOSSHttpClientFactory implements HttpCaller {
    private static final String CHARTER_UTF8 = "UTF-8";
    private CloseableHttpClient httpClient = null;
    private CloseableHttpClient httpsClient = null;
    private int connectTimeOut = 15000;
    private int socketTimeOut = 15000;
    private int maxConnTotal = 64;
    private int maxConnPerRoute = 64;
    private PoolingHttpClientConnectionManager httpClientPoolingManager;
    private Long evictIdleTime = -1L;

    public SFOSSHttpClientFactory() {
    }

    public void setEvictIdleTime(Long evictIdleTime) {
        this.evictIdleTime = evictIdleTime;
    }

    public void setMaxConnectOption(int maxConnTotal, int maxConnPerRoute) {
        this.maxConnPerRoute = maxConnPerRoute;
        this.maxConnTotal = maxConnTotal;
    }

    public void setMaxConnTotal(int maxConnTotal) {
        this.maxConnTotal = maxConnTotal;
    }

    public void setMaxConnPerRoute(int maxConnPerRoute) {
        this.maxConnPerRoute = maxConnPerRoute;
    }

    private RequestConfig getRequestConfig(TimeOutOption timeOutOption) {
        int connectTimeOutOpt = PropertiesUtil.getIntProperty("http.conn.timeout", this.connectTimeOut);
        int socketTimeOutOpt = PropertiesUtil.getIntProperty("http.socket.timeout", this.socketTimeOut);
        if (timeOutOption != null) {
            connectTimeOutOpt = timeOutOption.getConnectTimeOut();
            socketTimeOutOpt = timeOutOption.getSocketTimeOut();
        }

        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(connectTimeOutOpt).setSocketTimeout(socketTimeOutOpt).build();
        return requestConfig;
    }

    public ResponseResult doHttp(HttpRequestBase httpRequestBase, boolean isReturnByte) throws HttpException {
        ResponseResult result = null;

        ResponseResult var5;
        try {
            this.initHttpClient();
            CloseableHttpResponse response = this.httpClient.execute(httpRequestBase);
            if (isReturnByte) {
                result = this.handleResultForByteArray(response);
            } else {
                result = this.handleResult(response);
            }

            var5 = result;
        } catch (Exception var9) {
            throw new HttpException(var9.getMessage(), var9);
        } finally {
            if (null != httpRequestBase) {
                httpRequestBase.releaseConnection();
            }

        }

        return var5;
    }

    public ResponseResult doHttp(HttpRequestBase httpRequestBase, TimeOutOption timeOutOption, boolean isReturnByte) throws HttpException {
        if (null != timeOutOption) {
            RequestConfig requestConfig = this.getRequestConfig(timeOutOption);
            httpRequestBase.setConfig(requestConfig);
        }

        return this.doHttp(httpRequestBase, isReturnByte);
    }

    public ResponseResult doHttps(HttpRequestBase httpRequestBase, boolean isReturnByte) throws HttpException {
        ResponseResult result = null;

        ResponseResult var5;
        try {
            this.initHttpsClient();
            CloseableHttpResponse response = this.httpsClient.execute(httpRequestBase);
            if (isReturnByte) {
                result = this.handleResultForByteArray(response);
            } else {
                result = this.handleResult(response);
            }

            var5 = result;
        } catch (Exception var9) {
            throw new HttpException(var9.getMessage(), var9);
        } finally {
            if (null != httpRequestBase) {
                httpRequestBase.releaseConnection();
            }

        }

        return var5;
    }

    public ResponseResult doHttps(HttpRequestBase httpRequestBase, TimeOutOption timeOutOption, boolean isReturnByte) throws HttpException {
        if (null != timeOutOption) {
            RequestConfig requestConfig = this.getRequestConfig(timeOutOption);
            httpRequestBase.setConfig(requestConfig);
        }

        return this.doHttps(httpRequestBase, isReturnByte);
    }

    private ResponseResult handleResultForByteArray(CloseableHttpResponse response) throws Exception {
        try {
            byte[] byteArray = null;
            int statusCode = response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                byteArray = EntityUtils.toByteArray(entity);
            }

            return new ResponseResult(response, (String)null, byteArray, statusCode);
        } catch (Exception var5) {
            throw var5;
        }
    }

    private ResponseResult handleResult(CloseableHttpResponse response) throws IOException {
        String responseStr = null;
        int statusCode = response.getStatusLine().getStatusCode();
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            responseStr = EntityUtils.toString(entity, Charset.forName("UTF-8"));
        }

        return new ResponseResult(response, responseStr, (byte[])null, statusCode);
    }

    private void initHttpClient() {
        if (null == this.httpClient) {
            synchronized(this) {
                if (null == this.httpClient) {
                    this.setMaxConnectOption(PropertiesUtil.getIntProperty("http.conn.max.total", this.maxConnTotal), PropertiesUtil.getIntProperty("http.conn.max.perroute", this.maxConnTotal));
                    this.httpClientPoolingManager = new PoolingHttpClientConnectionManager();
                    this.httpClientPoolingManager.setMaxTotal(this.maxConnTotal);
                    this.httpClientPoolingManager.setDefaultMaxPerRoute(this.maxConnPerRoute);
                    HttpClientBuilder httpClientBuilder = HttpClients.custom().setConnectionManager(this.httpClientPoolingManager);
                    if (this.evictIdleTime != -1L && this.evictIdleTime > 0L) {
                        httpClientBuilder.evictExpiredConnections().evictIdleConnections(this.evictIdleTime, TimeUnit.SECONDS);
                    }

                    SocketConfig socketConfig = SocketConfig.custom().setSoKeepAlive(false).setSoLinger(1).setSoReuseAddress(true).setSoTimeout(PropertiesUtil.getIntProperty("http.socket.timeout", 10000)).setTcpNoDelay(true).build();
                    RequestConfig config = RequestConfig.custom().setConnectTimeout(PropertiesUtil.getIntProperty("http.conn.timeout", 15000)).setSocketTimeout(PropertiesUtil.getIntProperty("http.socket.timeout", 15000)).setConnectionRequestTimeout(PropertiesUtil.getIntProperty("http.getconn.from.poll.timeout", 15000)).build();
                    httpClientBuilder.setDefaultSocketConfig(socketConfig).setDefaultRequestConfig(config);
                    this.httpClient = httpClientBuilder.build();
                }
            }
        }

    }

    private void initHttpsClient() throws Exception {
        if (null == this.httpsClient) {
            synchronized(this) {
                if (null == this.httpsClient) {
                    X509TrustManager x509mgr = new X509TrustManager() {
                        public void checkClientTrusted(X509Certificate[] xcs, String string) {
                        }

                        public void checkServerTrusted(X509Certificate[] xcs, String string) {
                        }

                        public X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }
                    };
                    SSLContext sslContext = SSLContext.getInstance("TLSv1");
                    sslContext.init((KeyManager[])null, new TrustManager[]{x509mgr}, (SecureRandom)null);
                    SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext, SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
                    SocketConfig socketConfig = SocketConfig.custom().setSoKeepAlive(false).setSoLinger(1).setSoReuseAddress(true).setSoTimeout(PropertiesUtil.getIntProperty("http.socket.timeout", 10000)).setTcpNoDelay(true).build();
                    RequestConfig config = RequestConfig.custom().setConnectTimeout(PropertiesUtil.getIntProperty("http.conn.timeout", 15000)).setSocketTimeout(PropertiesUtil.getIntProperty("http.socket.timeout", 15000)).setConnectionRequestTimeout(PropertiesUtil.getIntProperty("http.getconn.from.poll.timeout", 15000)).build();
                    this.setMaxConnectOption(PropertiesUtil.getIntProperty("http.conn.max.total", this.maxConnTotal), PropertiesUtil.getIntProperty("http.conn.max.perroute", this.maxConnTotal));
                    this.httpsClient = HttpClients.custom().setSSLSocketFactory(sslsf).setMaxConnTotal(this.maxConnTotal).setMaxConnPerRoute(this.maxConnPerRoute).setDefaultSocketConfig(socketConfig).setDefaultRequestConfig(config).build();
                }
            }
        }

    }

    public void shutdownHttpClient(CloseableHttpClient httpClient) {
        if (this.httpClientPoolingManager != null) {
            this.httpClientPoolingManager.close();
        }

        HttpClientUtils.closeQuietly(httpClient);
    }

    public void setConnectTimeOut(int connectTimeOut) {
        this.connectTimeOut = connectTimeOut;
    }

    public void setSocketTimeOut(int socketTimeOut) {
        this.socketTimeOut = socketTimeOut;
    }
}
