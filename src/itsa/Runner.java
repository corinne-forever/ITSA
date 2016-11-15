/**
 * 
 */
package itsa;

import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

import java.util.Scanner;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Properties;

/**
 * @author Matthew Dews
 *
 */
public class Runner {

  private static final String ITSA_PROPERTIES = "itsa.properties";
  private static List<String> twitterUsernames = Collections.emptyList();
  private static final String DATE_FORMAT = "yyyy-MM-dd";
  private static Date START_DATE, END_DATE;
  
  /**
   * @param args
   */
  public static void main(String[] args) {
    Scanner reader = new Scanner(System.in); 
    System.out.println("Select a job: \n"
                     + "1. Collect twitter data\n"
                     + "2. Train SentiStrength\n"
                     + "3. Analyze twitter data with SentiStrength\n"
                     + "4. Generate graphics");
    
    switch(reader.nextInt()) {
      case 1:
        System.out.println("WARNING: Function not fully implemented.");
        collectTwitterData();
        break;
      case 2:
        // train sentrength
        System.out.println("Function not yet implemented, sorry.");
        break;
      case 3:
        // analyze data
        System.out.println("Function not yet implemented, sorry.");
        break;
      case 4:
        // generate graphics
        System.out.println("Function not yet implemented, sorry.");
        break;
      default:
        System.out.println("Invalid input, exiting.");
        break;
    }
    reader.close();
    System.out.println("Exiting...");
  }
  
  private static void collectTwitterData() {
    initializeProperties();
    
//    try {
//      Twitter twitter = new TwitterFactory().getInstance();
//      try {
//          // get request token.
//          // this will throw IllegalStateException if access token is already available
//          RequestToken requestToken = twitter.getOAuthRequestToken();
//          System.out.println("Got request token.");
//          System.out.println("Request token: " + requestToken.getToken());
//          System.out.println("Request token secret: " + requestToken.getTokenSecret());
//          AccessToken accessToken = null;
//
//          BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
//          while (null == accessToken) {
//              System.out.println("Open the following URL and grant access to your account:");
//              System.out.println(requestToken.getAuthorizationURL());
//              System.out.print("Enter the PIN(if available) and hit enter after you granted access.[PIN]:");
//              String pin = br.readLine();
//              try {
//                  if (pin.length() > 0) {
//                      accessToken = twitter.getOAuthAccessToken(requestToken, pin);
//                  } else {
//                      accessToken = twitter.getOAuthAccessToken(requestToken);
//                  }
//              } catch (TwitterException te) {
//                  if (401 == te.getStatusCode()) {
//                      System.out.println("Unable to get the access token.");
//                  } else {
//                      te.printStackTrace();
//                  }
//              }
//          }
//          System.out.println("Got access token.");
//          System.out.println("Access token: " + accessToken.getToken());
//          System.out.println("Access token secret: " + accessToken.getTokenSecret());
//      } catch (IllegalStateException ie) {
//          // access token is already available, or consumer key/secret is not set.
//          if (!twitter.getAuthorization().isEnabled()) {
//              System.out.println("OAuth consumer key/secret is not set.");
//              System.exit(-1);
//          }
//      }
//      Status status = twitter.updateStatus(args[0]);
//      System.out.println("Successfully updated the status to [" + status.getText() + "].");
//      System.exit(0);
//    } catch (TwitterException te) {
//        te.printStackTrace();
//        System.out.println("Failed to get timeline: " + te.getMessage());
//        System.exit(-1);
//    } catch (IOException ioe) {
//        ioe.printStackTrace();
//        System.out.println("Failed to read the system input.");
//        System.exit(-1);
//    }
  }
  
  private static void initializeProperties() {
    Properties prop = new Properties();
    InputStream inputStream = null;
    
    try {
      inputStream = new FileInputStream(ITSA_PROPERTIES);    
      prop.load(inputStream);
      
      // Read in usernames
      String u = prop.getProperty("USERNAMES");
      // Cleanup the input so we can parse it as a comma separated list
      u = u.replaceAll("\\s+","");
      twitterUsernames = Arrays.asList(u.split(","));
      System.out.println("Loaded twitter usernames: " + twitterUsernames.toString());
      
      // Read start date for tweets
      SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
      try {
        START_DATE = sdf.parse(prop.getProperty("START_DATE"));
        System.out.println("Loaded start date: " + START_DATE.toString());
      } catch (ParseException e) {
        System.err.println("Failed to parse the date from START_DATE=" + prop.getProperty("START_DATE"));
        e.printStackTrace();
      }
      try {
        END_DATE = sdf.parse(prop.getProperty("END_DATE"));
        System.out.println("Loaded end date: " + END_DATE.toString());
      } catch (ParseException e) {
        System.err.println("Failed to parse the date from END_DATE=" + prop.getProperty("END_DATE"));
        e.printStackTrace();
      }
      
      
    } catch (IOException e) {
      System.err.println("Error reading " + ITSA_PROPERTIES);
      e.printStackTrace();
      return;
    } finally {
      if (inputStream != null) {
        try {
          inputStream.close();
        } catch (IOException e) {
          System.err.println("Error trying to close InputStream for " + ITSA_PROPERTIES);
          e.printStackTrace();
        }
      } else {
        System.err.println("Failed to load " + ITSA_PROPERTIES);
      }
    }
  }
}
