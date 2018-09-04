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

    public static final Pattern LINK_PATTERN = Pattern.compile("<a href=\"(.*)\"\\s+title=");

    public static final String DIR_SUFFIX = "/";

    public static final String POM_SUFFIX = ".pom";

    public static final String SOURCE_SUFFIX = "-sources.jar";

    public static final String JAVADOC_SUFFIX = "-javadoc.jar";

    public static final String JAR_SUFFIX = ".jar";

    public static final boolean DOWNLOAD_SOURCES = true;

    public static final boolean DOWNLOAD_JAVADOC = false;

    private static final String DOWNLOAD_URL = "http://central.maven.org/maven2/abbot/";


    public static void main(String[] args) {

    }

    private static Map<String, Object> genDownloadMap(String baseURL, String downloadURL){
        if(baseURL.equals(downloadURL)){
            return new LinkedHashMap<String, Object>();
        }else{
            Map<String, Object> baseMap = new LinkedHashMap<String, Object>();
            String [] paths = downloadURL.substring(baseURL.length()).split("/");
            Map<String, Object> nowMap = baseMap;
            for(String path : paths){
                Map<String,Object> newMap = new LinkedHashMap<String,Object>();
                nowMap.put(path, newMap);
                nowMap = newMap;
            }
            return baseMap;
        }
    }

}
