package itsa;

import java.io.File;

public class Util {

    public static boolean fileExists(String f) {
        File file = new File(f);
        return  file.exists();
    }

}
