package itsa;

import java.io.File;
import java.util.Scanner;

public class Util {
    
    public static Scanner reader = new Scanner(System.in);
    
    public static boolean fileExists(String f) {
        File file = new File(f);
        return  file.exists();
    }
    
    public static String removeUrl(String string) {
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
