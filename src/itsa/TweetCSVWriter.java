package itsa;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import twitter4j.Status;

/**
 * A simple wrapper for the CSV Printer
 * @author mdews
 *
 */
public class TweetCSVWriter {
    FileWriter fw;
    BufferedWriter bw;
    CSVPrinter printer;

    public TweetCSVWriter(String filename) throws IOException {
        fw = new FileWriter(filename);
        bw = new BufferedWriter(fw);
        printer = new CSVPrinter(bw, CSVFormat.EXCEL);
    }

    public void print(Object value) throws IOException {
        printer.print(value); 
    }
    
    public void println() throws IOException {
        printer.println();
    }
    
    public void printRecord(Iterable<?> arg0) throws IOException {
        printer.printRecord(arg0);
    }
    
    public void printRecord(Object... values) throws IOException {
        printer.printRecord(values);
    }

    public void close() throws IOException {
        printer.close();
        fw.close();
        bw.close();
    }
}
