package com.weiquding.maven.batch.download;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Html text
 */
public class HtmlTest{

    public static final String htmlText= "<!DOCTYPE html>\n" +
            "<html>\n" +
            "\n" +
            "<head>\n" +
            "\t<title>Central Repository: abbot/costello</title>\n" +
            "\t<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
            "\t<style>\n" +
            "body {\n" +
            "\tbackground: #fff;\n" +
            "}\n" +
            "\t</style>\n" +
            "</head>\n" +
            "\n" +
            "<body>\n" +
            "\t<header>\n" +
            "\t\t<h1>abbot/costello</h1>\n" +
            "\t</header>\n" +
            "\t<hr/>\n" +
            "\t<main>\n" +
            "\t\t<pre id=\"contents\">\n" +
            "<a href=\"../\">../</a>\n" +
            "<a href=\"1.4.0/\" title=\"1.4.0/\">1.4.0/</a>                                            2015-09-24 09:24         -      \n" +
            "<a href=\"maven-metadata.xml\" title=\"maven-metadata.xml\">maven-metadata.xml</a>                                2015-09-24 09:26       320      \n" +
            "<a href=\"maven-metadata.xml.md5\" title=\"maven-metadata.xml.md5\">maven-metadata.xml.md5</a>                            2015-09-24 09:26        32      \n" +
            "<a href=\"maven-metadata.xml.sha1\" title=\"maven-metadata.xml.sha1\">maven-metadata.xml.sha1</a>                           2015-09-24 09:26        40      \n" +
            "\t\t</pre>\n" +
            "\t</main>\n" +
            "\t<hr/>\n" +
            "</body>\n" +
            "\n" +
            "</html>";

    public static final Pattern linkPattern = Pattern.compile("<a href=\"(.*)\"\\s+title=");


    @Test
    public void testHtmlRegex(){
        Matcher matcher = linkPattern.matcher(htmlText);
        while (matcher.find()){
            String sub = matcher.group(1);
            System.out.println(sub);
        }
    }

    // 1.4.0/
    // maven-metadata.xml
    // maven-metadata.xml.md5
    // maven-metadata.xml.sha1
}
