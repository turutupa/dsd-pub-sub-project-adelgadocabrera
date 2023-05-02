package utils;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * @author Alberto Delgado on 3/17/22
 * @project dsd-pub-sub
 * <p>
 * Parser for Kaggle data sets.
 */
public class KaggleParser {
    private final String topic;
    private final BufferedReader br;

    private KaggleParser(BufferedReader br, String topic) {
        this.br = br;
        this.topic = topic;
    }

    /**
     * Creates a KaggleParser
     *
     * @param filename
     * @param topic
     * @return
     */
    public static KaggleParser from(String filename, String topic) {
        try {
            FileInputStream fileStream = new FileInputStream(filename);
            InputStreamReader inputStream = new InputStreamReader(fileStream, StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(inputStream);
            return new KaggleParser(br, topic);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Returns the next line containing a topic
     *
     * @return
     */
    public String next() {
        String line;
        try {
            while ((line = br.readLine()) != null) {
                if (!line.contains("/" + topic + "/")) continue;
                return line;
            }
            return null; // no more data!
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Closes the buffered reader
     *
     * @return
     */
    public boolean close() {
        try {
            br.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
