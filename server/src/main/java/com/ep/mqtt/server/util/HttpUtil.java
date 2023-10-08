package com.ep.mqtt.server.util;

import java.io.IOException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

@Slf4j
public class HttpUtil {
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final static int READ_TIMEOUT = 5;
    private final static int CONNECT_TIMEOUT = 5;
    private final static int WRITE_TIMEOUT = 5;
    private final OkHttpClient okHttpClient;

    private HttpUtil() {
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
        // 读取超时
        clientBuilder.readTimeout(READ_TIMEOUT, TimeUnit.SECONDS);
        // 连接超时
        clientBuilder.connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS);
        // 写入超时
        clientBuilder.writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS);
        // 支持HTTPS请求，跳过证书验证
        clientBuilder.sslSocketFactory(createSSLSocketFactory(),
            (X509TrustManager)new TrustManager[] {new TrustAllCerts()}[0]);
        clientBuilder.hostnameVerifier((hostname, session) -> true);
        okHttpClient = clientBuilder.build();
    }

    /**
     * 生成安全套接字工厂，用于https请求的证书跳过
     */
    public SSLSocketFactory createSSLSocketFactory() {
        SSLSocketFactory ssfFactory;
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, new TrustManager[] {new TrustAllCerts()}, new SecureRandom());
            ssfFactory = sc.getSocketFactory();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ssfFactory;
    }

    private static class OkHttp3Holder {
        private final static HttpUtil INSTANCE = new HttpUtil();
    }

    public static HttpUtil getInstance() {
        return OkHttp3Holder.INSTANCE;
    }

    private String getResponseStr(Response response) {
        if (response.body() == null) {
            return null;
        }
        try {
            return response.body().string();
        } catch (IOException e) {
            throw new RuntimeException("IO异常", e);
        }
    }

    public String postJson(String url, String json, Map<String, String> headerParamMap) {
        Headers requestHeader = getRequestHeader(headerParamMap);
        RequestBody body = RequestBody.create(JSON, json);
        log.info("post json url: {} request body str: {}", url, json);
        Request request = new Request.Builder().headers(requestHeader).url(url).post(body).build();
        try {
            Response response = okHttpClient.newCall(request).execute();
            String responseStr = getResponseStr(response);
            log.info("post json url: {} response: {}, body: {}", url, response.toString(), responseStr);
            return responseStr;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 构造post的RequestHeader
     */
    private Headers getRequestHeader(Map<String, String> headerParamMap) {
        Headers.Builder headerBuilder = new Headers.Builder();
        if (headerParamMap == null) {
            return headerBuilder.build();
        }
        for (String key : headerParamMap.keySet()) {
            headerBuilder.add(key, headerParamMap.get(key));
        }
        return headerBuilder.build();
    }

    /**
     * 用于信任所有证书
     */
    static class TrustAllCerts implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) {}

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) {

        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }

}
