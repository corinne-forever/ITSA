/**
 * 
 */
package itsa;

import java.util.Scanner;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Properties;

import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.MongoClient;
import org.bson.Document;

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
      System.out.println("Loaded twitter usernames: " + usernames.toString());

      // Read start date for tweets
      SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
      try {
        START_DATE = sdf.parse(prop.getProperty("START_DATE"));
        System.out.println("Loaded start date: " + START_DATE.toString());
      } catch (ParseException e) {
        System.err.println("Failed to parse the date from START_DATE=" + prop.getProperty("START_DATE"));
        e.printStackTrace();
        System.exit(1);
      }
      try {
        END_DATE = sdf.parse(prop.getProperty("END_DATE"));
        System.out.println("Loaded end date: " + END_DATE.toString());
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
    //http://thinktostart.com/build-your-own-twitter-archive-and-analyzing-infrastructure-with-mongodb-java-and-r-part-1/
    // Connect to DB
    System.out.println("Connecting to Mongo DB...");
    MongoClient mongoClient = new MongoClient("127.0.0.1");
    MongoDatabase database = mongoClient.getDatabase("tweets");
    // System.out.println("Connected to Mongo DB successfully.");

    collection = database.getCollection("twitter");
  }

  private static void runCLIInterface() {
    Scanner reader = new Scanner(System.in); 
    Boolean shouldExit = false;
    
    while (!shouldExit) {
      System.out.println("Select a job: \n"
                       + "1. Collect twitter data\n"
                       + "2. Train SentiStrength\n"
                       + "3. Analyze twitter data with SentiStrength\n"
                       + "4. SAVE sentiment results to csv\n"
                       + "5. Exit\n");
      // TODO update jobs to include normalization
      switch(reader.nextInt()) {
      case 1:
        System.out.println("WARNING: Function not fully implemented.");
        collectAllStatuses();
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
        // CSV generation
        System.out.println("Function not yet implemented, sorry.");
        break;
      case 5:
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

  private static void collectAllStatuses() {
    for (String u : usernames) {
      collectStatusesInRange(END_DATE, START_DATE, u);
      Status latestStatus = getFirstStatusBefore(END_DATE, u);
      System.out.println("Latest Tweet from " + latestStatus.getUser().getName() + ":\n"
          + latestStatus.getText() + "\n"
          + "Before date " + END_DATE);
    }
  }

  private static List<Status> collectStatusesInRange(Date latest, Date earliest, String username) {
    Document doc = new Document()
        .append("username", username)
        .append("numTweets", 0);
    collection.insertOne(doc);
    Status status = getFirstStatusBefore(END_DATE, username);
    long maxId = status.getId() + 1; // Add one so the first status is included in paging.
    ArrayList<Status> completeStatuses = new ArrayList<Status>();
    try {
      int page = 1;
      List<Status> pagedStatuses = Collections.emptyList();
      do {
        pagedStatuses = twitter.getUserTimeline(username, new Paging(page, PAGE_SIZE, 1, maxId - 1));
        for (Status s : pagedStatuses) {
          status = s;
          if ((status.getCreatedAt()).before(earliest)) {
            //return status;
          }
        }
      } while (false);

    } catch (TwitterException e) {

    }
    return null;
  }

  /**
   * Find the first status posted before @date by @username
   * @param date
   * @param username
   * @return id
   */
  private static Status getFirstStatusBefore(Date date, String username) {
    Status status = null;

    try {
      int page = 1; //Initial page
      status = twitter.getUserTimeline(username, new Paging(1,1)).get(0); // Latest tweet
      // Edge case that the first tweet matches.
      if ((status.getCreatedAt()).before(date)) {
        return status;
      }

      do {
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
  
  // Will return null if there are no tweets after a certain date :/ 
  private static Status getFirstStatusAfter(Date date, String username) {
    Status status = null;
    Status nextStatus = null;

    try {
      int page = 1; //Initial page
      nextStatus = twitter.getUserTimeline(username, new Paging(1,1)).get(0); // Latest tweet
      // Edge case that the first tweet matches.
      // TODO fix this
      if ((nextStatus.getCreatedAt()).before(date)) {
        return null;
      }

      do {
        List<Status> statuses = twitter.getUserTimeline(username, new Paging(page, PAGE_SIZE, 1, status.getId() - 1));
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
  
  private static void shutdown() {
    shutdownMongo();
  }
  
  private static void shutdownMongo() {
    mongoClient.close();
  }
}
