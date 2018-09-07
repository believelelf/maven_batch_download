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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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

    private static final String BASE_DIR = "E:\\maven_central\\";

    private static final boolean DOWNLOAD_SOURCES = true;

    private static final boolean DOWNLOAD_JAVADOC = false;

    private static final String DOWNLOAD_URL = "http://central.maven.org/maven2/";

    private static final OkHttpClient client = new OkHttpClient();


    public static void main(String[] args) {

        long time = System.currentTimeMillis();

        Map<String, Object> rootMap = scanResources(BASE_URL, DOWNLOAD_URL);

        if(logger.isInfoEnabled()){
            logger.info("scanResources end===> use time[{}]\r\n---------------------------------------------"
                    , System.currentTimeMillis() - time);
        }

        // download jar/pom from aliyun maven repo really
        List<Callable<Boolean>> tasks = addTasks(rootMap, CN_BASE_URL);

        time = System.currentTimeMillis();

        if(logger.isInfoEnabled()){
            logger.info("download resources starting===> current time[{}]\r\n---------------------------------------------"
                    , new Date());
        }

        invokeTasks(tasks);

        if(logger.isInfoEnabled()){
            logger.info("download resources end===> use time[{}]", System.currentTimeMillis() - time);
        }

    }

    private static void invokeTasks(List<Callable<Boolean>> tasks){
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
        try {
            final List<Future<Boolean>> futures = executorService.invokeAll(tasks);
        } catch (InterruptedException e) {
           logger.error("An error occurred when invoking all tasks");
        }
        executorService.shutdown();

    }

    private static List<Callable<Boolean>> addTasks(Map<String,Object> rootMap, String baseURL){
        List<Callable<Boolean>> tasks = new ArrayList<>();
        addTask(tasks, rootMap, baseURL);
        return tasks;
    }

    private static void addTask(List<Callable<Boolean>> tasks, Map<String,Object> rootMap, String baseURL) {
        for(Map.Entry<String, Object> entry: rootMap.entrySet()){
            String key = entry.getKey();
            Object value = entry.getValue();
            String url = baseURL + key;
            if(value instanceof Map){
                addTask(tasks, (Map)value, url + "/");
            }else if (key.endsWith(POM_SUFFIX) || key.endsWith(JAR_SUFFIX)){
                String filePath = BASE_DIR + url.substring(CN_BASE_URL.length()).replace("/", File.separator);
                logger.debug("add task ===>url::[{}] filePath::[{}]", url, filePath);
                tasks.add(new DownloadTask(url, filePath));
            }
        }
    }

    private static Map<String, Object> scanResources(String baseURL, String downloadURL){
        // maven repository root path
        Map<String, Object> rootMap = new LinkedHashMap<String, Object>();
        // download path
        Map<String, Object> downloadMap = genDownloadMap(rootMap, baseURL, downloadURL);
        // request downloadURL
        requestURL(downloadMap, downloadURL);

        return rootMap;
    }

    private static void requestURL(Map<String,Object> downloadMap, String downloadURL) {
        String content = requestHtml(downloadURL);
        if(content != null){
            List<String> paths = resolveContent(content, LINK_PATTERN);
            for(String path : paths){
                if(path.endsWith(DIR_SUFFIX)){
                    Map<String, Object> nowMap = new LinkedHashMap<>();
                    downloadMap.put(path.substring(0,(path.length()-1)), nowMap);
                    requestURL(nowMap, downloadURL + path);
                }else if(path.endsWith(POM_SUFFIX)){
                    downloadMap.put(path, null);
                }else if(path.endsWith(JAVADOC_SUFFIX) && DOWNLOAD_JAVADOC){
                    downloadMap.put(path, null);
                }else if(path.endsWith(SOURCE_SUFFIX) && DOWNLOAD_SOURCES){
                    downloadMap.put(path, null);
                }else if(path.endsWith(JAR_SUFFIX) && !path.endsWith(JAVADOC_SUFFIX) && !path.endsWith(SOURCE_SUFFIX)){
                    downloadMap.put(path, null);
                }
            }
        }
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
            Response response = client.newCall(request).execute();
            String content = response.body().string();
            if(logger.isDebugEnabled()){
                logger.debug("request html url ===>[{}]::end==>use time[{}]", url, System.currentTimeMillis() - time);
            }
            return content;
        } catch (IOException e) {
            logger.error("An error occurred when requesting resource from maven repo", e);
        }
        return null;
    }




    private static Map<String, Object> genDownloadMap(Map<String, Object> rootMap, String baseURL, String downloadURL){
        if(baseURL.equals(downloadURL)){
            return rootMap;
        }else{
            String [] paths = downloadURL.substring(baseURL.length()).split("/");
            Map<String, Object> nowMap = rootMap;
            for(String path : paths){
                Map<String,Object> newMap = new LinkedHashMap<String,Object>();
                nowMap.put(path, newMap);
                nowMap = newMap;
            }
            return nowMap;
        }
    }


    private static class DownloadTask implements Callable<Boolean> {

        private String url;
        private String filePath;

        public DownloadTask(String url, String filePath) {
            this.url = url;
            this.filePath = filePath;
        }

        @Override
        public Boolean call() throws Exception {

            logger.debug("request jar/pom url ===>[{}]::starting", url);
            long time = System.currentTimeMillis();
            Request request = new Request.Builder()
                    .url(url)
                    .build();
            InputStream inputStream  = null;
            try {
                Response response = client.newCall(request).execute();
                inputStream  = response.body().byteStream();
                FileUtils.copyToFile(inputStream, new File(filePath));
            } catch (IOException e) {
                logger.error("An error occurred when requesting resource from maven repo", e);
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
