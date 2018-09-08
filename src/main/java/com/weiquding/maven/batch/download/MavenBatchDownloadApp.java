package com.weiquding.maven.batch.download;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * scan resources from maven central repository
 * download jar/pom from aliyun maven repository
 * @author wubai
 */
public class MavenBatchDownloadApp {

    private static final Logger logger = LoggerFactory.getLogger(MavenBatchDownloadApp.class);

    private static final String BASE_URL = "http://central.maven.org/maven2/";

    private static final Pattern LINK_PATTERN = Pattern.compile("<a href=\"(.*)\"\\s+title=");

    private static final String DIR_SUFFIX = "/";

    private static final String POM_SUFFIX = ".pom";

    private static final String SOURCE_SUFFIX = "-sources.jar";

    private static final String JAVADOC_SUFFIX = "-javadoc.jar";

    private static final String JAR_SUFFIX = ".jar";

    /**************************you can change these variables****************************/

    private static final String CN_BASE_URL = "https://maven.aliyun.com/repository/public/";

    private static final String BASE_DIR = "F:\\maven_central\\";

    private static final boolean DOWNLOAD_SOURCES = true;

    private static final boolean DOWNLOAD_JAVADOC = false;

    private static final String DOWNLOAD_URL = "http://central.maven.org/maven2/";


    public static void main(String[] args) {

        long time = System.currentTimeMillis();

        ExecutorService executorService = Executors
                .newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
        requestURL(executorService, DOWNLOAD_URL);
        executorService.shutdown();

        if(logger.isInfoEnabled()){
            logger.info("download resources end===> use time[{}]", System.currentTimeMillis() - time);
        }

    }


    private static void requestURL(ExecutorService executorService, String downloadURL) {
        String content = requestHtml(downloadURL);
        if(content != null){
            List<String> paths = resolveContent(content, LINK_PATTERN);
            List<String> dirs = new ArrayList<>();
            for(String path : paths){
                if(path.endsWith(DIR_SUFFIX)){
                    dirs.add(downloadURL + path);
                }else{
                    submitTask(executorService, downloadURL, path);
                }
            }
            if (dirs.isEmpty()){
                return;
            }
            List<Callable<List<String>>> callables = new ArrayList<>();
            for(String url : dirs){
                callables.add(new DownloadHtmlTask(executorService, url));
            }
            List<String> urls = new ArrayList<>();
            try {
                List<Future<List<String>>> futures = executorService.invokeAll(callables);
                for(Future<List<String >> future : futures){
                    urls.addAll(future.get());
                }
            } catch (InterruptedException | ExecutionException e) {
                logger.error("An error occurred when invoking [{}] from maven repo", downloadURL, e);
            }
            for (String url : urls){
                requestURL(executorService, url);
            }
        }
    }

    /**
     * request html
     * @param url
     * @return
     * @throws IOException
     */
    private static String requestHtml(String url) {
        logger.debug("request html url ===>[{}]::starting", url);
        long time = System.currentTimeMillis();
        Request request = new Request.Builder()
                .url(url)
                .build();
        try {
            OkHttpClient client = bulidOKHttpClient();
            Response response = client.newCall(request).execute();
            String content = response.body().string();
            if(logger.isDebugEnabled()){
                logger.debug("request html url ===>[{}]::end==>use time[{}]", url, System.currentTimeMillis() - time);
            }
            return content;
        } catch (IOException e) {
            logger.error("An error occurred when requesting [{}] from maven repo", url, e);
        }
        return null;
    }

    private static List<String> resolveContent(String content, Pattern linkPattern){
        Matcher matcher = linkPattern.matcher(content);
        List<String> paths = new ArrayList<String>();
        while (matcher.find()){
            String path = matcher.group(1);
            logger.debug("current path==>[{}]", path);
            if(path.endsWith(DIR_SUFFIX) || path.endsWith(POM_SUFFIX) || path.endsWith(JAR_SUFFIX)){
                paths.add(path);
            }
        }
        return paths;
    }


    private static void submitTask(ExecutorService executorService, String downloadURL, String path){
        if (path.endsWith(POM_SUFFIX) || path.endsWith(JAR_SUFFIX)){
            if(!DOWNLOAD_JAVADOC && path.endsWith(JAVADOC_SUFFIX)
                    || !DOWNLOAD_SOURCES && path.endsWith(SOURCE_SUFFIX) ){
                return;
            }
            String parentPath = downloadURL.substring(BASE_URL.length());
            String filePath = BASE_DIR + parentPath.replace("/", File.separator) + path;
            String url = CN_BASE_URL + parentPath + path;
            logger.debug("add task ===>url::[{}] filePath::[{}]", url, filePath);
            executorService.submit(new DownloadJarTask(url, filePath));
        }
    }

    private static OkHttpClient bulidOKHttpClient(){
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS)
                .build();
        return  client;
    }


    private static class DownloadHtmlTask implements Callable<List<String>>{

        private String url;
        private ExecutorService executorService;

        public DownloadHtmlTask(ExecutorService executorService, String url){
            this.executorService = executorService;
            this.url = url;
        }

        @Override
        public List<String> call() throws Exception {
            List<String> subURLs = new ArrayList<>();
            String content = requestHtml(url);
            List<String> paths = resolveContent(content, LINK_PATTERN);
            for (String path : paths) {
                if (path.endsWith(DIR_SUFFIX)) {
                    subURLs.add(url + path);
                } else {
                    submitTask(executorService, url, path);
                }
            }
            return subURLs;
        }
    }


    private static class DownloadJarTask implements Callable<Boolean> {

        private String url;
        private String filePath;

        public DownloadJarTask(String url, String filePath) {
            this.url = url;
            this.filePath = filePath;
        }

        @Override
        public Boolean call() throws Exception {

            logger.debug("request jar/pom url ===>[{}]::starting", url);
            long time = System.currentTimeMillis();
            InputStream inputStream  = null;
            try {
                File file = new File(filePath);
                if(file.exists() && file.length() > 0){
                    if(logger.isDebugEnabled()) {
                        logger.debug("request jar/pom url ===>[{}]::end==>use time[{}]:: file.exists", url, System.currentTimeMillis() - time);
                    }
                    return true;
                }
                Request request = new Request.Builder()
                        .url(url)
                        .build();
                OkHttpClient client = bulidOKHttpClient();
                Response response = client.newCall(request).execute();
                inputStream  = response.body().byteStream();
                FileUtils.copyToFile(inputStream, new File(filePath));
            } catch (IOException e) {
                logger.error("An error occurred when requesting [{}] from maven repo", url, e);
                return false;
            }finally {
                if(inputStream != null){
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        logger.error("close inputStrem error", e);
                    }
                }
            }
            if(logger.isDebugEnabled()){
                logger.debug("request jar/pom url ===>[{}]::end==>use time[{}]", url, System.currentTimeMillis() - time);
            }
            return true;
        }
    }
}
