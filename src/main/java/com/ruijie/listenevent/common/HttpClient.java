package com.ruijie.listenevent.common;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import javax.net.ssl.*;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Map;

/**
 * <p>Title:      </p>
 * <p>Description: http client
 * <p>Copyright: Copyright (c) 2019     </p>
 * <p>Company: Ruijie Co., Ltd.          </p>
 * <p>Create Time: 2019/10/30 13:57       </p>
 *
 * @author lijianhua
 * <p>Update Time:                      </p>
 * <p>Updater:                          </p>
 * <p>Update Comments:                  </p>
 */
@Slf4j
public class HttpClient {

   private OkHttpClient okHttpClient;

    private static final TrustManager[] trustAllCerts = new TrustManager[] {
            new X509TrustManager() {
                @Override
                public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                }

                @Override
                public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                }

                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return new java.security.cert.X509Certificate[]{};
                }
            }
    };
    private static final SSLContext trustAllSslContext;
    static {
        try {
            trustAllSslContext = SSLContext.getInstance("SSL");
            trustAllSslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }
    private static final SSLSocketFactory trustAllSslSocketFactory = trustAllSslContext.getSocketFactory();

    public static OkHttpClient trustAllSslClient(OkHttpClient client) {

        OkHttpClient.Builder builder = client.newBuilder();
        builder.sslSocketFactory(trustAllSslSocketFactory, (X509TrustManager)trustAllCerts[0]);
        builder.hostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        });
        return builder.build();
    }

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

   public HttpClient(OkHttpClient okHttpClient){
       this.okHttpClient = okHttpClient;
   }

    public HttpClient(OkHttpClient okHttpClient, Boolean truestSsl){
       this(okHttpClient);
       if(truestSsl){
          this.okHttpClient =  trustAllSslClient(this.okHttpClient);
       }
    }

    public OkHttpClient getOkHttpClient() {
        return okHttpClient;
    }

    public String get(String url, Map<String,String> maps) throws IOException {
       Request.Builder builder = new Request.Builder().get().url(url);
       maps.keySet().stream().forEach(i->builder.addHeader(i,maps.get(i)));
       Request request = builder.build();
       try (Response response = okHttpClient.newCall(request).execute()) {
           return response.body().string();
       }
   }

   public String post(String url, String json, Map<String,String> maps) throws IOException {
       RequestBody body = RequestBody.create(JSON, json);
       Request.Builder builder = new Request.Builder().post(body).url(url);
       maps.keySet().stream().forEach(i->builder.addHeader(i,maps.get(i)));
       Request request = builder.build();
       try (Response response = okHttpClient.newCall(request).execute()) {
           if(response.code() == 404){
               log.error("请求url不存在:{}",url);
           }
           return response.body().string();
       }
   }

    public String post(String url, RequestBody requestBody, Map<String,String> maps) throws IOException {
        Request.Builder builder = new Request.Builder().post(requestBody).url(url);
        maps.keySet().stream().forEach(i->builder.addHeader(i,maps.get(i)));
        Request request = builder.build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            return response.body().string();
        }
    }


    public String put(String url, String json, Map<String,String> maps) throws IOException {
        RequestBody body = RequestBody.create(JSON, json);
        Request.Builder builder = new Request.Builder().put(body).url(url);
        maps.keySet().stream().forEach(i->builder.addHeader(i,maps.get(i)));
        Request request = builder.build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            return response.body().string();
        }
    }
}
