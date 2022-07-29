package com.yahoo.egads.utilities;
import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @Author ZhangLe
 * @Date 2020/11/25 12:33
 */
public class HttpsClient {

    private CloseableHttpClient httpClient;
    private HttpGet httpGet;
    public static final String CONTENT_TYPE = "Content-Type";
    private String url;

    public HttpsClient() {

    }
    /**
     * 发送get请求
     * @param url 发送链接 拼接参数  http://localhost:8090/order?a=1
     * @return
     * @throws IOException
     */
    public String sendGet(String url) throws IOException {
        httpClient = HttpClients.createDefault();
        httpGet = new HttpGet(url);
        CloseableHttpResponse response = httpClient.execute(httpGet);
        String resp;
        try {
            HttpEntity entity = response.getEntity();
            resp = EntityUtils.toString(entity, "utf-8");
            EntityUtils.consume(entity);
        } finally {
            response.close();
        }
        LoggerFactory.getLogger(getClass()).info(" resp:{}", resp);
        return resp;
    }

}
