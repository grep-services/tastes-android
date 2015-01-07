package com.instamenu.util;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;

public class NetworkProcessor {

    private static final String protocol = "http";
    private static final String host = "54.65.1.56";
    private static final int port = 3639;
    private static final String encoding = HTTP.UTF_8;
    private static final String img_name = "image";
    private static final String file_name = "image.jpg";
    private static final String mime_type = "application/octet-stream";
    //private static final String mime_type = "multipart/form-data";
    /*
    public String getGet() {
        String result = null;
        HttpClient httpClient = new DefaultHttpClient();
        HttpUriRequest httpUriRequest;
        URI uri = null;
        try {
            uri = URIUtils.createURI(protocol, host, port, "/tag/get/", null, null);
            if(uri!=null) httpUriRequest = new HttpGet(uri);
            else return null;

            HttpResponse httpResponse;
            httpResponse = httpClient.execute(httpUriRequest);
            HttpEntity httpEntity=httpResponse.getEntity();
            if(httpEntity!=null) result=EntityUtils.toString(httpEntity);
        } catch(Exception e) {
            LogWrapper.e("GET", e.getMessage());
        }

        return result;
    }
    */
    // 전체적으로 null check 필요한 부분들 exception에서 걸리면 설정해준다.
    public String getResponse(String path, List<NameValuePair> parameters, byte[] file)
    {
        String response = null;

        HttpClient httpClient = new DefaultHttpClient();// 나중에 ThreadSafeClient 쓰든가 해본다.(4.x 에서 Pool 어쩌고로 바꼈다고 하던데...)

        try {
            // get도 있으면 몰라도 post에서는 query null이므로(나중 추가) 그냥 uri method 없애고 이렇게 간단하게 간다.
            HttpPost httpPost = new HttpPost(URIUtils.createURI(protocol, host, port, path, null, null));// uri null은... path not null인데 null 안될듯.

            if(file == null) {
                if(parameters != null) {// 안하면 exception. 아마 UrlEncodedFormEntity 일듯.
                    UrlEncodedFormEntity entity = new UrlEncodedFormEntity(parameters, encoding);
                    httpPost.setEntity(entity);
                }
            } else {
                // 이 부분도 상위 버전에서는 builder로 바꼈다고 함.
                // 그리고 그냥 mode, charset 없이 해도 상관없긴 함.
                MultipartEntity multipartEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE, null, Charset.forName(encoding));

                multipartEntity.addPart(img_name, new ByteArrayBody(file, mime_type, file_name));

                for(NameValuePair parameter:parameters) {
                    // 여기에 encoding을 이렇게 넣어줘야 제대로 들어간다.
                    multipartEntity.addPart(parameter.getName(), new StringBody(parameter.getValue(), Charset.forName(encoding)));
                }

                httpPost.setEntity(multipartEntity);// 이 method는 HttpPost에만 define되어있음..
            }

            HttpResponse httpResponse = httpClient.execute(httpPost);
            HttpEntity httpEntity = httpResponse.getEntity();
            // 여기에 이렇게 encoding을 넣어줘야 제대로 날아온다.
            response = EntityUtils.toString(httpEntity, encoding);// entity null이면 IllegalArgumentException 뜬다. null check 하지말고 필요하면 exception으로 처리한다.

        } catch(Exception e) {// 나중에 connection refused(org.apache.http.conn.HttpHostConnectException) 설정해주기.
            LogWrapper.e("REQUEST", e.getMessage());
        }

        LogWrapper.e("RESPONSE", response);

        return response;
    }
}