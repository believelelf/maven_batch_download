package com.weiquding.maven.batch.download;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author wubai
 */
public class MavenBatchDownloadApp {

    private static final String BASE_URL = "http://central.maven.org/maven2/";

    private static final String BASE_DIR = "F:\\maven_central\\";

    private static final String CONTENT_TYPE = "text/html" ;

    private static final Pattern LINK_PATTERN = Pattern.compile("<a href=\"(.*)\"\\s+title=");

    private static final String DIR_SUFFIX = "/";

    private static final String POM_SUFFIX = ".pom";

    private static final String SOURCE_SUFFIX = "-sources.jar";

    private static final String JAVADOC_SUFFIX = "-javadoc.jar";

    private static final String JAR_SUFFIX = ".jar";

    private static final boolean DOWNLOAD_SOURCES = true;

    private static final boolean DOWNLOAD_JAVADOC = false;

    private static final String DOWNLOAD_URL = "http://central.maven.org/maven2/abbot/";


    public static void main(String[] args) {

    }

    private static Map<String, Object> scanResources(){
        Map<String, Object> baseMap = new LinkedHashMap<String, Object>();
        Map<String, Object> downloadMap = genDownloadMap(baseMap, BASE_URL, DOWNLOAD_URL);
        //TODO to implement scan maven resources

        return baseMap;
    }


    private static Map<String, Object> genDownloadMap(Map<String, Object> baseMap, String baseURL, String downloadURL){
        if(baseURL.equals(downloadURL)){
            return baseMap;
        }else{
            String [] paths = downloadURL.substring(baseURL.length()).split("/");
            Map<String, Object> nowMap = baseMap;
            for(String path : paths){
                Map<String,Object> newMap = new LinkedHashMap<String,Object>();
                nowMap.put(path, newMap);
                nowMap = newMap;
            }
            return nowMap;
        }
    }



}
