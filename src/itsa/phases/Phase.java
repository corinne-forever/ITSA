package itsa.phases;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import itsa.TweetCSVWriter;
import itsa.Util;


/**
 * 
 * @author mdews
 *
 */
public class Phase {
    
    private static HashMap<Integer, Phase> phases = new HashMap<Integer, Phase>();
    private int phaseNumber;
    private String phaseName;
    private final RecordModifierInterface recordModifier;
    
    
    /**
     * Check that the phaseNumber and phaseName were set and are not duplicates.
     * Add the new Phase to the global list of Phases
     */
    public Phase(int phaseNumber, String phaseName, RecordModifierInterface recordModifier) {
        this.phaseNumber    = phaseNumber;
        this.phaseName      = phaseName;
        this.recordModifier = recordModifier;
        
        phases.put(phaseNumber, this);
    }
    
    /**
     * Run the specified phase
     * @param phaseNumber The number of the phase to run
     * @param usernames The list of usernames to pass to the phase to run
     */
    public static void runPhase(int phaseNumber, List<String> usernames) {
        phases.get(phaseNumber).run(usernames);
    }
    
    private final void run(List<String> usernames) {
        for (String username : usernames) {
            run(username);
        }
    }
    
    /**
     * Setup 
     * @param username
     */
    private final void run(String username) {
        // Open reader for raw data
        String prevPhaseFilename = getFilenameForPhaseUsername(this.phaseNumber - 1, username);
        File prevPhaseFile = new File(prevPhaseFilename);
        
        if (!prevPhaseFile.exists()) {
            System.out.println("Could not find prerequisite file " + prevPhaseFilename);
            return;
        }
        
        CSVParser parser;
        try {
            parser = CSVParser.parse(prevPhaseFile, Charset.defaultCharset(), CSVFormat.EXCEL);
        } catch (IOException e) {
            System.err.println("Failed to create CSVParser for " + prevPhaseFilename);
            e.printStackTrace();
            return;
        }
        
        // Open tokenized writer
        String sentimentDataFilename = getFilenameForPhaseUsername(this.phaseNumber, username);
        
        if (!Util.fileExistsDialog(sentimentDataFilename)) {
            return;
        }
        
        TweetCSVWriter writer;
        try {
            writer = new TweetCSVWriter(sentimentDataFilename);
        } catch (IOException e) {
            System.err.println("Failed to create TweetCSVWriter for: " + sentimentDataFilename);
            e.printStackTrace();
            return;
        }
        
        List<CSVRecord> records;
        try {
            records = parser.getRecords();
        } catch (IOException e1) {
            System.err.println("Failed to get CSV records for: " + sentimentDataFilename);
            e1.printStackTrace();
            return;
        }
        ListIterator<CSVRecord> iterator = records.listIterator();
        
        // Copy header
        CSVRecord header = iterator.next();
        try {
            writer.printRecord(header);
        } catch (IOException e1) {
            System.err.println("Failed to write header for username: " + username);
            e1.printStackTrace();
            return;
        }
        
        // Process records and record tokenized results
        while (iterator.hasNext()) {
            CSVRecord csvRecord = iterator.next();
            // Put contents into ArrayList for easier manipulation
            // This allows modifiers to only touch the field they care about, the rest will be copied as is
            ArrayList<String> record = new ArrayList<String>();
            for (String s : csvRecord) {
                record.add(s);
            }
            ArrayList<String> newRecord = recordModifier.modify(record);
            
            try {
                writer.printRecord(newRecord);
            } catch (IOException e) {
                System.err.println("Failed to write a record");
                e.printStackTrace();
            }
        }
        
        try {         
            parser.close();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static String getFilenameForPhaseUsername(int phaseNumber, String username) {
        Phase phase = phases.get(phaseNumber);
        return phase.phaseNumber + "-" + username + "-" + phase.phaseName + ".csv";
    }
    
    @FunctionalInterface
    public static interface RecordModifierInterface{
        ArrayList<String> modify(ArrayList<String> csvRecord);
    }
}