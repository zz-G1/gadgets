import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zz
 * http请求工具类
 * Date: 21/09/13
 */

public class HttpClientUtils {
    /**
     *
     * @param headers
     * @return
     */
    public static Map<String, List<String>> convertHeaders(Header[] headers){
       Map<String, List<String>> results=new HashMap<String, List<String>>();
       for(Header header:headers){
           List<String> list=results.get(header.getName());
           if (list==null){
               list=new ArrayList<String>();
               results.put(header.getName(),list);
           }
           list.add(header.getValue());
       }
           return results;
    }

    /**
     * http的get请求
     * @param url
     * @return
     */
    public static String get(String url){
        return get(url,"UTF-8");
    }
    public static Logger logger=LoggerFactory.getLogger(HttpClientUtils.class);

    /**
     * http的get请求
     * @param url
     * @return
     */
    public static String get(String url,String charset){
        HttpGet httpGet=new HttpGet(url);
        return executeRequest(httpGet,charset);
    }

    /**
     * http的get请求，增加异步请求头参数
     * @param url
     * @return
     */
    public static String ajaxGet(String url){
        return ajaxGet(url,"UTF-8");
    }

    /**
     * http的get的请求，增加异步请求头参数
     * @param url
     * @param charset
     * @return
     */
    public static String ajaxGet(String url,String charset){
        HttpGet httpGet=new HttpGet(url);
        httpGet.setHeader("X-Requset-With","XMLHttpRequest");
        return executeRequest(httpGet,charset);
    }

    /**
     *
     * @param httpClient
     * @param url
     * @return
     */
    public static String ajaxGet(CloseableHttpClient httpClient,String url){
        HttpGet httpGet=new HttpGet(url);
        httpGet.setHeader("X-Requset-With","XMLHttpRequest");
        return executeRequest(httpClient,httpGet,"UTF-8");
    }

    /**
     * http的post请求，传递map格式参数
     * @param url
     * @param dataMap
     * @return
     */
    public static String post(String url,Map<String, String> dataMap){
        return post(url,dataMap,"UTF-8");
    }

    /**
     * http的post请求，传递map格式参数
     * @param url
     * @param dataMap
     * @param charset
     * @return
     */
    public static String post(String url,Map<String, String> dataMap,String charset){
        HttpPost httpPost=new HttpPost(url);
        try{
            if (dataMap!=null){
                List<NameValuePair> nameValuePairs=new ArrayList<NameValuePair>();
                for (Map.Entry<String, String> entry:dataMap.entrySet()){
                    nameValuePairs.add(new BasicNameValuePair(entry.getKey(),entry.getValue()));
                }
                //UrlEncodedFormEntity这个类是用来把输入数据编码成合适的内容
                UrlEncodedFormEntity formEntity=new UrlEncodedFormEntity(nameValuePairs, charset);
                formEntity.setContentEncoding(charset);
                httpPost.setEntity(formEntity);
            }
        }catch (UnsupportedEncodingException e){
            e.printStackTrace();
        }
        return executeRequest(httpPost,charset);
    }

    /**
     * http的post请求，增加异步请求头参数，传递map格式参数
     * @param url
     * @param dataMap
     * @return
     */
    public static String ajaxPost(String url,Map<String,String> dataMap){
        return ajaxPost(url,dataMap,"UTF-8");
    }

    /**
     * http的post请求，增加异步请求头参数，传递map格式参数
     * @param url
     * @param dataMap
     * @param charset
     * @return
     */
    public static String ajaxPost(String url,Map<String, String> dataMap,String charset){
        HttpPost httpPost = new HttpPost(url);
        httpPost.setHeader("X-Requested-With", "XMLHttpRequest");
        try {
            if (dataMap != null) {
                List<NameValuePair> nvps = new ArrayList<NameValuePair>();
                for (Map.Entry<String, String> entry : dataMap.entrySet()) {
                    nvps.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
                }
                UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(nvps, charset);
                formEntity.setContentEncoding(charset);
                httpPost.setEntity(formEntity);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return executeRequest(httpPost, charset);
    }

    /**
     *  http的post请求，增加异步请求头参数，传递json格式参数
     * @param url
     * @param jsonString
     * @return
     */
    public static String ajaxPostJson(String url,String jsonString){
        return ajaxPostJson(url,jsonString,"UTF-8");
    }

    public static String ajaxPostJson(String url,String jsonString,String charset){
        HttpPost httpPost=new HttpPost(url);
        httpPost.setHeader("X-Requested-With", "XMLHttpRequest");

        StringEntity stringEntity=new StringEntity(jsonString,charset);//解决中文乱码问题
        stringEntity.setContentEncoding(charset);
        stringEntity.setContentType("application/json");
        httpPost.setEntity(stringEntity);
        return executeRequest(httpPost,charset);
    }

    /**
     * 执行一个HTTP请求传递httpget或httppost参数
     * @param httpUriRequest
     * @return
     */
    public static String executeRequset(HttpUriRequest httpUriRequest){
        return executeRequest(httpUriRequest,"UTF-8");

    }



    /**
     * 执行一个HTTP请求传递httpget或httppost参数
     * @param httpUriRequest
     * @param charset
     * @return
     */
    public static String executeRequest(HttpUriRequest httpUriRequest,String charset){
        CloseableHttpClient httpClient;
        if("https".equals(httpUriRequest.getURI().getScheme())){
            httpClient= createSSLInsecureClient();
        }else{
            httpClient=HttpClients.createDefault();
        }
        String result="";
        try{
            try{
                CloseableHttpResponse response=httpClient.execute(httpUriRequest);
                HttpEntity entity=null;
                try{
                    entity=response.getEntity();
                    result=EntityUtils.toString(entity,charset);
                }finally {
                    EntityUtils.consume(entity);
                    response.close();
                }
            }finally {
                httpClient.close();
            }
        }catch (IOException e){
            e.printStackTrace();
        }
        return result;

    }


    /**
     * 执行一个HTTP请求传递httpget或httppost参数
     * @param httpClient
     * @param httpUriRequest
     * @param charset
     * @return
     */
    private static String executeRequest(CloseableHttpClient httpClient, HttpUriRequest httpUriRequest,String charset){
        String result="";
        try{
            try{
                CloseableHttpResponse response=httpClient.execute(httpUriRequest);
                HttpEntity entity=null;
                try{
                    entity=response.getEntity();
                    result=EntityUtils.toString(entity,charset);
                }finally {
                    EntityUtils.consume(entity);
                    response.close();
                }
            }finally {
                httpClient.close();
            }
        }catch (IOException e){
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 创建SSL链接
     * @return
     */
    public static CloseableHttpClient createSSLInsecureClient(){
        try{
            SSLContext sslContext=new SSLContextBuilder().loadTrustMaterial(new TrustStrategy() {
                @Override
                public boolean isTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                    return true;
                }
            }).build();
            SSLConnectionSocketFactory sslsf=new SSLConnectionSocketFactory(sslContext, new HostnameVerifier() {
                @Override
                public boolean verify(String s, SSLSession sslSession) {
                    return true;
                }
            });
            return HttpClients.custom().setSSLSocketFactory(sslsf).build();
        }catch (GeneralSecurityException e){
            throw new RuntimeException(e);
        }
    }


}
