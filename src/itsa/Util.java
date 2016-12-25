package itsa;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Util {

    public static boolean fileExists(String f) {
        File file = new File(f);
        return  file.exists();
    }
    
    // https://stackoverflow.com/questions/12366496/removing-the-url-from-text-using-java
    private static String removeUrl(String commentstr) {
        String urlPattern = "((https?|ftp|gopher|telnet|file|Unsure|http):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)";
        Pattern p = Pattern.compile(urlPattern,Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(commentstr);
        int i = 0;
        while (m.find()) {
            commentstr = commentstr.replaceAll(m.group(i),"").trim();
            i++;
        }
        return commentstr;
    }
}
