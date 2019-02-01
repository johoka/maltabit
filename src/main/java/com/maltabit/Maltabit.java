package com.maltabit;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.http.*;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Maltabit {

    private static final String DEFAULT_ENCODE = "utf-8";

    public static void main(String[] args) throws Exception{
        Map<String, Object> param = new HashMap<>();
//        param.put("username", "18665893524");
//        param.put("password", "abc123456");
        param.put("username", "15928462201");
        param.put("password", "abc123456");
        String url = "http://maltabit.com/Login/submit.html";
        Map<String, Object> loginResult = post(url, param);
        Header headers[] = (Header[]) loginResult.get("headers");
        String cookieVal = headers[0].getValue().split(";")[0] + ";" + " think_language=zh-CN";
        //String cookieVal = "";
        //Header headers[] = new Header[]{};
        while(true){
            Thread.sleep(500);
            String getDepthUrl = "http://maltabit.com/Ajax/getDepth?market=mairc_usdt&trade_moshi=1&t=" + Math.random();
            String guadanStr = get(getDepthUrl, cookieVal);
            //JSONObject guadan = JSONObject.fromObject("{\"depth\":{\"buy\":[[0.245,9113.805],[0.2399,1700],[0.239,320],[0.2364,200],[0.235,174],[0.2324,9.1212],[0.231,300],[0.2301,895],[0.23,1417.9332],[0.2292,600],[0.2291,200],[0.229,175.7967],[0.2289,164.887],[0.2288,190],[0.228,200]],\"sell\":[[0.2638,221],[0.263,1586.506],[0.262,1456.899],[0.26,897.227],[0.2594,20000],[0.25939,702.199],[0.2592,400],[0.259,1280],[0.2585,264],[0.258,3000],[0.2511,2071],[0.25,1000],[0.2478,1000],[0.24701,6],[0.2457,753]]}}");
            JSONObject guadan = JSONObject.fromObject(guadanStr.replaceAll("\uFEFF",""));
            JSONObject depth = guadan.getJSONObject("depth");
            JSONArray buy = depth.getJSONArray("buy");
            JSONArray sell = depth.getJSONArray("sell");

            JSONArray buy1 = (JSONArray)buy.get(0);
            Double buy1Price = (Double) buy1.get(0);
            Double buy1Num = Double.parseDouble(buy1.get(1).toString());
            System.out.println(buy1Price);
            System.out.println(buy1Num);

            JSONArray sell1 = (JSONArray)sell.get(sell.size()-1);
            Double sell1Price = (Double) sell1.get(0);
            Double sell1Num = Double.parseDouble(sell1.get(1).toString());
            System.out.println(buy1Price + "：" + buy1Num);
            System.out.println(sell1Price + "：" + sell1Num);
//            System.out.println(sell1Price);
//            System.out.println(sell1Num);

            if(buy1Price<0.2d){
                continue;
            }
            //Double profitRate = 0.9965;
            Double profitRate = 0.997;
            if(buy1Price/sell1Price>=profitRate){
                Double minNum = buy1Num>sell1Num?sell1Num:buy1Num;
                if(minNum>400d){
                    minNum = 400d;
                }
                String buyAndSellUrl = "http://maltabit.com/Trade/upTrade.html";
                Map<String, Object> buyParam = new HashMap<>();
                buyParam.put("price", sell1Price);
                buyParam.put("num", minNum);
                buyParam.put("market", "mairc_usdt");
                buyParam.put("type", "1");
                System.out.println(get(buyAndSellUrl,cookieVal,buyParam));

                Map<String, Object> sellParam = new HashMap<>();
                sellParam.put("price", buy1Price);
                sellParam.put("num", minNum);
                sellParam.put("market", "mairc_usdt");
                sellParam.put("type", "2");
                System.out.println(get(buyAndSellUrl,cookieVal,sellParam));
            }
        }


    }


    public static String get(String uri, String cookieVal) throws Exception{
        HttpGet httpGet = new HttpGet(uri);
        RequestConfig config = RequestConfig.custom().setConnectTimeout(30000).build();
        httpGet.setConfig(config);
        httpGet.setHeader("Cookie",cookieVal);
        Map<String,Object> result = execute(httpGet, false);
        String data = (String) result.get("data");
        return data;
    }

    public static String get(String uri,String cookieVal,Map<String, Object> params) {
        try {
            return get(toGetParams(uri, params), cookieVal);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }



    public static Map<String,Object> post(String uri, Map<String, Object> params) {
        HttpPost httppost = new HttpPost(uri);
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            Object value = entry.getValue();
            nvps.add(new BasicNameValuePair(entry.getKey(), value != null ? value.toString() : null));
        }
        httppost.setEntity(new UrlEncodedFormEntity(nvps, Consts.UTF_8));
        httppost.setConfig(RequestConfig.DEFAULT);
        return execute(httppost, false);
    }

    private static Map<String,Object> execute(HttpUriRequest request, boolean isTimeOutControl) {
        Map<String,Object> result = new HashMap<>();
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        //HttpClient
        CloseableHttpClient closeableHttpClient = httpClientBuilder.build();
        HttpResponse rsp;
        int status = 0;
        try {
            rsp = closeableHttpClient.execute(request);
            Header headers[] = rsp.getHeaders("Set-Cookie");
            result.put("headers",headers);
            status = rsp.getStatusLine().getStatusCode();
            if (status == 200) {
                HttpEntity entity = rsp.getEntity();
                String data = EntityUtils.toString(entity);
                result.put("data",data);
            }
        } catch (Exception e) {
            e.printStackTrace();
            //logger.error("HttpClientUtil found ClientProtocolException:" + e.getMessage());
            //throw new RuntimeException(e);
        } finally {
//            client.getConnectionManager().shutdown();
            try {
                if (null != closeableHttpClient) {
                    closeableHttpClient.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    private static String toGetParams(String uri, Map<String, Object> params) throws UnsupportedEncodingException {
        if(StringUtils.isBlank(uri)){
            return null;
        }
        if(params == null || params.size() == 0){
            return uri;
        }
        StringBuffer param = new StringBuffer();
        int i = 0;
        for (String key : params.keySet()) {
            if (params.get(key) == null)
                continue;
            if (i == 0)
                param.append("?");
            else
                param.append("&");
            param.append(key).append("=").append(urlEncode(String.valueOf(params.get(key))));
            i++;
        }
        return uri + param.toString();
    }

    /**
     * url转码
     *
     * @param value
     * @return
     * @throws UnsupportedEncodingException
     */
    private static String urlEncode(String value) throws UnsupportedEncodingException {
        return URLEncoder.encode(value, DEFAULT_ENCODE);
    }

}
