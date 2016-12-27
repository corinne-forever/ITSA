package itsa;

import java.io.File;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Util {
    
    public static Scanner reader = new Scanner(System.in);
    
    public static boolean fileExists(String f) {
        File file = new File(f);
        return  file.exists();
    }
    
    // https://stackoverflow.com/questions/12366496/removing-the-url-from-text-using-java
    public static String removeUrl(String string) {
//        String urlPattern = "((https?|ftp|gopher|telnet|file|Unsure|http):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)";
//        Pattern p = Pattern.compile(urlPattern,Pattern.CASE_INSENSITIVE);
//        Matcher m = p.matcher(commentstr);
//        int i = 0;
//        while (m.find()) {
//            commentstr = commentstr.replaceAll(m.group(i),"").trim();
//            i++;
//        }
//        return commentstr;
        return string.replaceAll("https?://\\S+\\s?", "");
    }
    
    /**
     * appends (y/n)? to message
     * @param fmt
     * @param args
     * @return Returns true if the user responded yes, no otherwise
     */
    public static boolean yesNoDialog(String message) {
        String response; 
        
        do {
            System.out.println(message + " (y/n)?");
            response = reader.nextLine();
        }
        while (!response.equals("y") && !response.equals("n"));
        return response.equals("y");
    }
    
    public static boolean fileExistsDialog(String filename) {
        if (fileExists(filename)) {
            return yesNoDialog("The file \"" + filename + "\" already exists. Do you want to overwrite it? No output will be collected otherwise.");
        }
        return true;
    }
    
}
