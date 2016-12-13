/**
 * 
 */
package itsa;

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
import java.util.logging.Logger;

import org.bson.Document;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

import twitter4j.Paging;
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
  private static final int PAGE_SIZE = 20;
  private static MongoClient mongoClient;
  private static MongoDatabase database;
  private static MongoCollection<Document> collection;

  /**
   * @param args
   */
  public static void main(String[] args) {
    initialize();    
    runCLIInterface();
    shutdown();
  }

  /*
   * Initialization methods
   */
  
  private static void initialize() {
    System.out.println("Initializing...");
    initializeITSAProperties();
    initializeTwitterProperties();
    initializeMongo();
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

  private static void initializeTwitterProperties() {
    try {
      twitter = TwitterFactory.getSingleton();
      twitter.verifyCredentials();
    } catch (TwitterException e) {
      System.err.println("Error while verifying twitter credentials:\n"
          + e.getErrorMessage());
    }
  }

  private static void initializeMongo() {
    // Change log level so stdout is not filled with log messages
    Logger mongoLogger = Logger.getLogger( "org.mongodb.driver" );
    //mongoLogger.setLevel(Level.SEVERE); 
    
    // Connect to DB
    System.out.println("Connecting to Mongo DB...");
    mongoClient = new MongoClient("127.0.0.1");
    database = mongoClient.getDatabase("tweets");
    // System.out.println("Connected to Mongo DB successfully.");

    collection = database.getCollection("twitter");
  }

  private static void runCLIInterface() {
    Scanner reader = new Scanner(System.in); 
    Boolean shouldExit = false;
    
    while (!shouldExit) {
      System.out.println("Select a job: \n"
                       + "1. Collect twitter data\n"
                       + "1a. Drop twitter data from Mongo\n"
                       + "2. Train SentiStrength\n"
                       + "3. Analyze twitter data with SentiStrength\n"
                       + "4. SAVE sentiment results to csv\n"
                       + "5. Exit\n");
      // TODO update jobs to include normalization
      switch(reader.nextLine()) {
      case "1":
        collectAllStatuses();
        break;
      case "1a":
        System.out.println("Dropping twitter data");
        collection.drop();
        break;
      case "2":
        // train sentrength
        System.out.println("Function not yet implemented, sorry.");
        break;
      case "3":
        // analyze data
        System.out.println("Function not yet implemented, sorry.");
        break;
      case "4":
        // CSV generation
        System.out.println("Function not yet implemented, sorry.");
        break;
      case "5":
        // exit
        reader.close();
        System.out.println("EXITING");
        shouldExit = true;
        break;
      default:
        System.out.println("Invalid input.");
        break;
      } 
    }
  }

  /*
   * Status collection methods
   */
  
  private static void collectAllStatuses() {
    for (String u : usernames) {
      System.out.println("Collecting Statuses for user: " + u);
      collectStatusesForUser(u);
    }
  }

  private static void collectStatusesForUser(String username) {
    Document doc = new Document()
        .append("username", username)
        .append("tweets", new ArrayList<>());
    int tweetsRecordedInMongo = 0;
    collection.insertOne(doc); // Insert starter doc for user
    Status status = getFirstStatusBefore(END_DATE, username);
    long maxId = status.getId() + 1; // Add one so the first status is included in paging.
    
    try {
      int page = 1;
      List<Status> pagedStatuses = Collections.emptyList();
      Boolean reachedEnd = false;
      do {
        handleUserTimeLineRateLimit();
        pagedStatuses = twitter.getUserTimeline(username, new Paging(page, PAGE_SIZE, 1, maxId - 1));
        for (Status s : pagedStatuses) {
          if ((s.getCreatedAt()).before(START_DATE)) { // Stop if past the START date
            reachedEnd = true;
            break;
          }
          // Add status to Document
          Document tweet = new Document()
                                .append("text", s.getText())
                                .append("date",s.getCreatedAt().toString());

          collection.updateOne(Filters.eq("username", username),Updates.addToSet("tweets", tweet));
          System.out.println(++tweetsRecordedInMongo + " tweets recorded to mongo so far");
        }
        
      } while (!reachedEnd);

    } catch (TwitterException e) {
      System.err.println("Error in collectStatusesForUser:  " + username +":\n"
          + e.getErrorMessage());
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

    try {
      int page = 1; //Initial page
      handleUserTimeLineRateLimit();
      status = twitter.getUserTimeline(username, new Paging(1,1)).get(0); // Latest tweet
      // Edge case that the first tweet matches.
      if ((status.getCreatedAt()).before(date)) {
        return status;
      }

      do {
        handleUserTimeLineRateLimit();
        List<Status> statuses = twitter.getUserTimeline(username, new Paging(page, PAGE_SIZE, 1, status.getId() - 1));
        // iterate over statuses
        for (Status s : statuses) {
          status = s;
          if ((status.getCreatedAt()).before(date)) {
            return status;
          }
        }
        page++;
      } while (!(status.getCreatedAt()).before(date));

    } catch (TwitterException e) {
      System.err.println("Error in getFirstStatusBefore:  " + date + ", " + username +":\n"
          + e.getErrorMessage());
      System.exit(1);
    }

    return status;
  }
  
  private static void handleUserTimeLineRateLimit() {
  //System.out.println("Remaining: " + twitter.getUserTimeline().getRateLimitStatus().getRemaining());
    try {
      if(twitter.getUserTimeline().getRateLimitStatus().getRemaining() <= 3){
        try {
          System.out.println("Stopping for " + twitter.getUserTimeline().getRateLimitStatus().getSecondsUntilReset()
                            + " seconds for rate limiting");
          TimeUnit.SECONDS.sleep(twitter.getUserTimeline().getRateLimitStatus().getSecondsUntilReset() + 2);
          System.out.println("Resuming operation after rate limit sleep");
        } catch (InterruptedException e) {
          System.err.println("Something went wrong while sleeping");
          e.printStackTrace();
        }
      }
    } catch (TwitterException e) {
      System.err.println("Twitter error during Rate Limit check");
      e.printStackTrace();
    }
  }

  /**
   * Find the first status posted after @date by @username
   * Does not handle edge cases properly :)
   * Will return null if there are no tweets after a given date
   * @param date
   * @param username
   * @return Status
   */
  private static Status getFirstStatusAfter(Date date, String username) {
    Status status = null;
    Status nextStatus = null;

    try {
      int page = 1; //Initial page
      nextStatus = twitter.getUserTimeline(username, new Paging(1,1)).get(0); // Latest tweet
      // Edge case that the first tweet matches.
      if ((nextStatus.getCreatedAt()).before(date)) {
        return null;
      }

      do {
        List<Status> statuses = twitter.getUserTimeline(username, new Paging(page, PAGE_SIZE, 1, nextStatus.getId() - 1));
        // iterate over statuses
        for (Status s : statuses) {
          status = nextStatus;
          nextStatus = s;
          if ((nextStatus.getCreatedAt()).before(date)) {
            return status;
          }
        }
        page++;
      } while (!(nextStatus.getCreatedAt()).before(date));

    } catch (TwitterException e) {
      System.err.println("Error in getFirstStatusAfter:  " + date + ", " + username +":\n"
          + e.getErrorMessage());
      System.exit(1);
    }
    
    return status;
  }
  
  /*
   * Shutdown methods
   */
  
  private static void shutdown() {
    shutdownMongo();
  }
  
  private static void shutdownMongo() {
    mongoClient.close();
  }
}
