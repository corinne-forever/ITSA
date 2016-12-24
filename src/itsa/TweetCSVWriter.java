package itsa;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import twitter4j.Status;

public class TweetCSVWriter {
    FileWriter fw;
    BufferedWriter bw;
    CSVPrinter printer;

    public TweetCSVWriter(String filename) throws IOException {
        fw = new FileWriter(filename);
        bw = new BufferedWriter(fw);
        printer = new CSVPrinter(bw, CSVFormat.EXCEL);

        printHeaders();
    }

    private void printHeaders() throws IOException {
        printer.print("Username");
        printer.print("Date");
        printer.print("Text");
        printer.println();
    }

    public void recordStatus(Status status) throws IOException {
        printer.print(status.getUser().getScreenName());
        printer.print(status.getCreatedAt().toString());
        printer.print(status.getText());
        printer.println();

    }

    public void close() throws IOException {
        printer.close();
        fw.close();
        bw.close();
    }
}
