/**
 * 
 */
package itsa;

import java.io.Console;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import twitter4j.Paging;
import twitter4j.RateLimitStatus;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;

/**
 * @author Matthew Dews
 *
 */
public class Runner {

  private static final String ITSA_PROPERTIES = "itsa.properties";
  private static List<String> usernames = Collections.emptyList();
  private static final String DATE_FORMAT = "yyyy-MM-dd";
  private static Date START_DATE, END_DATE;
  private static Twitter twitter = null;
  private static final int PAGE_SIZE = 100;
  private static TweetCSVWriter writer;
  private static Console console;

  // TODO refactor to use System.console()
  
  /**
   * @param args
   */
  public static void main(String[] args) {
    initialize();    
    runCLI();
    shutdown();
  }

  /*
   * Initialization methods
   */

  private static void initialize() {
//    if ((console = System.console()) == null) {
//      System.err.println("Unable to initialize console\n"
//                       + "This may be because the program wasn't started interactively or the operating system doesn't support it");
//    }
        
    System.out.println("Initializing...");
    initializeITSAProperties();
    initializeTwitter4j();
    System.out.println("Finished initializing");
  }

  private static void initializeITSAProperties() {
    Properties prop = new Properties();
    InputStream inputStream = null;

    try {
      inputStream = new FileInputStream(ITSA_PROPERTIES);    
      prop.load(inputStream);

      // Read in usernames
      String u = prop.getProperty("USERNAMES");
      // Cleanup the input so we can parse it as a comma separated list
      u = u.replaceAll("\\s+","");
      usernames = Arrays.asList(u.split(","));
      //System.out.println("Loaded twitter usernames: " + usernames.toString());

      // Read start date for tweets
      SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
      try {
        START_DATE = sdf.parse(prop.getProperty("START_DATE"));
        //System.out.println("Loaded start date: " + START_DATE.toString());
      } catch (ParseException e) {
        System.err.println("Failed to parse the date from START_DATE=" + prop.getProperty("START_DATE"));
        e.printStackTrace();
        System.exit(1);
      }
      try {
        END_DATE = sdf.parse(prop.getProperty("END_DATE"));
        //System.out.println("Loaded end date: " + END_DATE.toString());
      } catch (ParseException e) {
        System.err.println("Failed to parse the date from END_DATE=" + prop.getProperty("END_DATE"));
        e.printStackTrace();
        System.exit(1);
      }


    } catch (IOException e) {
      System.err.println("Error reading " + ITSA_PROPERTIES);
      e.printStackTrace();
      System.exit(1);
    } finally {
      if (inputStream != null) {
        try {
          inputStream.close();
        } catch (IOException e) {
          System.err.println("Error trying to close InputStream for " + ITSA_PROPERTIES);
          e.printStackTrace();
          System.exit(1);
        }
      } else {
        System.err.println("Failed to load " + ITSA_PROPERTIES);
        System.exit(1);
      }
    }
  }

  
  
  private static void initializeTwitter4j() {
    try {
      twitter = TwitterFactory.getSingleton();
      twitter.verifyCredentials();
    } catch (TwitterException e) {
      System.err.println("Error while verifying twitter credentials:\n"
          + e.getErrorMessage());
    }
  }


  private static void runCLI() {
    Scanner reader = new Scanner(System.in); 
    Boolean shouldExit = false;

    while (!shouldExit) {
      System.out.println("\nType the job you wish to run: \n"
          + "view: View current parameters\n"
          + "1: Collect twitter data\n"
          + "2: Regularize and normalize data\n"
          + "train: Train SentiStrength\n"
          + "3: Analyze twitter data with SentiStrength\n"
          + "exit: Exit\n");
      switch(reader.nextLine()) {
      case "A":
        System.out.println("Function not yet implemented, sorry.");
        break;
      case "1":
        // TODO prompt user for verification?
        collectAllStatuses();
        break;
      case "2":
        System.out.println("Function not yet implemented, sorry.");
        break;
      case "3":
        // analyze data
        System.out.println("Function not yet implemented, sorry.");
        break;

      case "exit":
        reader.close();
        shouldExit = true;
        break;
      default:
        System.err.println("Invalid input.");
        break;
      } 
    }
  }

  /*
   * Status collection methods
   */

  private static void collectAllStatuses() {
    // Initialize Tweet CSV Writer    
    for (String u : usernames) {
      System.out.println("Collecting Statuses for user: " + u);
      collectStatusesForUser(u);
    }
  }

  private static void collectStatusesForUser(String username) {
    try {
      writer = new TweetCSVWriter("1-" + username + "-raw_data.csv");
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(1);
    }
    Status firstStatus = getFirstStatusBefore(END_DATE, username);
    long maxId = firstStatus.getId() + 1; // Add one so the first status is included in paging.
    
    System.out.println("Getting statuses from " + username + " before " + END_DATE);
    
    try {
      int page = 1;
//      System.out.println("Collecting timeline page " + page + " for user " + username);
      ResponseList<Status> statuses = twitter.getUserTimeline(username, new Paging(page, PAGE_SIZE, page, maxId - 1));
      Boolean reachedEnd = false;
      
      do {
        for (Status s : statuses) {
          System.out.println(s.getText());
          if ((s.getCreatedAt()).before(START_DATE)) { // Stop if past the START date
            reachedEnd = true;
            break;
          }
          try {
            writer.recordStatus(s);
          } catch (IOException e) {
            System.err.println("Failed to write tweet: " + s.getText());
            e.printStackTrace();
          };
        } 
        if (reachedEnd) {
          // Don't bother loading a new set of statuses if we are done
          break;
        }
        else {
          page++;
          handleUserTimelineRateLimit(statuses);
//          System.out.println("Collecting timeline page " + page + " for user " + username);
          statuses = twitter.getUserTimeline(username, new Paging(page, PAGE_SIZE, page, maxId - 1));
        }        
      } while (!reachedEnd && statuses.size() > 0); // Halt if end date is released or no more tweets are loaded

    } catch (TwitterException e) {
      System.err.println("Error in collectStatusesForUser:  " + username +":\n"
          + e.getErrorMessage());
    }
    
    try {
      writer.close();
    } catch (IOException e) {
      System.err.println("Failed to close TweetCSVWriter");
      e.printStackTrace();
    }
  }

  /**
   * Find the first status posted before @date by @username
   * Does not handle edge cases properly :)
   * @param date
   * @param username
   * @return Status
   */
  private static Status getFirstStatusBefore(Date date, String username) {
    Status status = null;
    System.out.println("Searching for first status from " + username + " before " + date);
    try {
      int page = 1; //Initial page
      ResponseList<Status> statuses = twitter.getUserTimeline(username, new Paging(1,1));

      do {
        // iterate over statuses
        for (Status s : statuses) {
          status = s;
          if ((status.getCreatedAt()).before(date)) {
            return status;
          }
        }
        handleUserTimelineRateLimit(statuses); // Wait if necessary
        page++;
        statuses = twitter.getUserTimeline(username, new Paging(page, PAGE_SIZE, 1, status.getId() - 1));
      } while (!(status.getCreatedAt()).before(date));

    } catch (TwitterException e) {
      System.err.println("Error in getFirstStatusBefore:  " + date + ", " + username +":\n"
          + e.getErrorMessage());
      System.exit(1);
    }

    return status;
  }

  private static void handleUserTimelineRateLimit(ResponseList<Status> rl) {
    try {
      RateLimitStatus rateLimitStatus = rl.getRateLimitStatus();
      
      int secondsUntilReset = rateLimitStatus.getSecondsUntilReset();
      if(rateLimitStatus.getRemaining() == 0){
        try {
          System.out.println("Sleeping for " + secondsUntilReset + " seconds for rate limiting");
          TimeUnit.SECONDS.sleep(secondsUntilReset + 2);
          System.out.println("Done sleeping");
        } catch (InterruptedException e) {
          System.err.println("Something went wrong while sleeping");
          e.printStackTrace();
        }
      }
    } 
//    catch (TwitterException e) {
//      System.err.println("Twitter error during Rate Limit check");
//      e.printStackTrace();
//    } 
    catch (NullPointerException e) {
      System.err.println("Null pointer exception during rate limiting. Sleeping for full duration.");
      e.printStackTrace();
    }
  }

  /*
   * Shutdown methods
   */

  private static void shutdown() {
    
  }

}
