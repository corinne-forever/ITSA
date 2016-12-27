/**
 * 
 */
package itsa;

import itsa.Util;
import itsa.phases.Phase;

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
import uk.ac.wlv.sentistrength.SentiStrength;

/**
 * @author Matthew Dews
 *
 */
public class Runner {

    private static final String ITSA_PROPERTIES = "itsa.properties";
    private static Date START_DATE, END_DATE;
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static List<String> usernames = Collections.emptyList();
    
    // CSV Headers
    private static final String USERNAME_HEADER = "Username", 
                                DATE_HEADER = "Date Time",
                                TWEET_ORIGINAL_HEADER = "Original Tweet",
                                TWEET_TOKENIZED_HEADER = "Tokenized tweet",
                                TWEET_POS_SCORE = "Positive score",
                                TWEET_NEG_SCORE = "Negative score",
                                TWEET_SENTI_EXPLANATION_HEADER = "SentiStrength Explanation";
                                
    private static final String[] headers = {USERNAME_HEADER, 
                                             DATE_HEADER,
                                             TWEET_ORIGINAL_HEADER, 
                                             TWEET_TOKENIZED_HEADER,
                                             TWEET_POS_SCORE, 
                                             TWEET_NEG_SCORE,
                                             TWEET_SENTI_EXPLANATION_HEADER
                                            };
    

    private static Twitter twitter;
    private static final int PAGE_SIZE = 100;
    
    private static TweetCSVWriter writer;
    private static Scanner reader = Util.reader;
    
    private static SentiStrength sentiStrength;
    private static String[] sentiStrengthParameters =  {"sentidata", "./lib/sentistrength_data/", "explain"};
    
    private static SimpleDateFormat dateTimeExcelFormat = new SimpleDateFormat("YYYY-MM-dd HH:mm");

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
        initializeITSAProperties();
        initializeTwitter4j();
        initializeSentiStrength();
        initializePhases();
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

    private static void initializeSentiStrength() {
        sentiStrength = new SentiStrength();
        sentiStrength.initialise(sentiStrengthParameters);
    }
    
    @SuppressWarnings("unused") // The phases are added to the static list of phases
    private static void initializePhases() { 
        Phase phase1 = new Phase(1, "original", null); // we handle the first phase ourselves
        Phase phase2 = new Phase(2, "tokenized", 
                (ArrayList<String> record) -> {
                                               String original = record.get(2);
                                               original = Util.removeUrl(original);
                                               record.add(3, original);
                                               return record;
                                               }
                );
        Phase phase3 = new Phase(3, "sentiment",
                (ArrayList<String> record) -> {
                                               String tokenizedText = record.get(3);
                                               String explanation = sentiStrength.computeSentimentScores(tokenizedText);
                                               record.add(4, getPostiveScore(explanation));
                                               record.add(5, getNegativeScore(explanation));
                                               record.add(6, removeScore(explanation));
                                               return record;
                                               }                
                );
    }
    
    private static void shutdown() {
        reader.close();
    }
    
    private static void runCLI() {
        Boolean shouldExit = false;

        while (!shouldExit) {
            
            System.out.println("\nType the job you wish to run: \n"
                             + "1: Collect twitter data\n"
                             + "2: Tokenize tweets\n"
                             + "3: Analyze twitter data with SentiStrength\n"
                             + "exit: Exit\n");
            switch(reader.nextLine()) {
            case "1":
                System.out.println("Collecting tweets from users: " + usernames.toString());
                collectAllStatuses();
                break;
                
            case "2":
                System.out.println("WARNING: This phase currently applies no changes to the data, "
                                 + "but is still a prerequisite for the next stage.");
                System.out.println("Tokenizing tweets for users: " + usernames.toString());
                Phase.runPhase(2, usernames);
                break;
                
            case "3":
                System.out.println("Computing sentiment strength for users: " + usernames.toString());
                Phase.runPhase(3, usernames);
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
        for (String u : usernames) {
            System.out.println("Collecting Statuses for user: " + u);
            collectStatusesForUser(u);
        }
    }

    private static void collectStatusesForUser(String username) {
        //System.out.println("Collecting data for user " + username);
        String filename = Phase.getFilenameForPhaseUsername(1, username);
        if (!Util.fileExistsDialog(filename)) {
            return;
        }
        
        try {
            writer = new TweetCSVWriter(filename);
        } catch (IOException e) {
            System.err.println("Failed to create TweetCSVWriter for username: " + username);
            e.printStackTrace();
            return;
        }
        
        //Print header
        try {
            writer.printRecord((Object[])headers);
        } catch (IOException e1) {
            System.err.println("Failed to write header for username: " + username);
            e1.printStackTrace();
            return;
        }
        
        Status firstStatus = getFirstStatusBefore(END_DATE, username);
        long maxId = firstStatus.getId() + 1; // Add one so the first status is included in paging.

        // System.out.println("Getting statuses from " + username + " before " + END_DATE);

        try {
            int page = 1;
            //      System.out.println("Collecting timeline page " + page + " for user " + username);
            ResponseList<Status> statuses = twitter.getUserTimeline(username, new Paging(page, PAGE_SIZE, page, maxId - 1));
            Boolean reachedEnd = false;

            do {
                for (Status s : statuses) {
                    if ((s.getCreatedAt()).before(START_DATE)) { // Stop if past the START date
                        reachedEnd = true;
                        break;
                    }
                    
                    try {
                        writer.printRecord(s.getUser().getScreenName(),
                                           dateTimeExcelFormat.format(s.getCreatedAt()),
                                           s.getText());
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
//        System.out.println("Searching for first status from " + username + " before " + date);
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
        catch (NullPointerException e) {
            System.err.println("Null pointer exception during rate limiting. Sleeping for full duration.");
            e.printStackTrace();
        }
    }
    
    private static String getPostiveScore(String s) {
        return s.substring(0, 1);
    }
    
    private static String getNegativeScore(String s) {
        return s.substring(2, 4);
    }
    
    private static String removeScore(String s) {
        return s.substring(5);
    }
}
