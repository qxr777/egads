package com.yahoo.egads.utilities;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.yahoo.egads.data.TimeSeries;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.StringTokenizer;

public class APIUtils {
    public static ArrayList<TimeSeries> createTimeSeries(JSONArray jjson, Properties config) {
        // Input file which needs to be parsed
        //String fileToParse = csv_file;
        //BufferedReader fileReader = null;
        ArrayList<TimeSeries> output = new ArrayList<TimeSeries>(1);

        // Delimiter used in CSV file
        final String delimiter = ",";
        Long interval = null;
        Long prev = null;
        Integer aggr = 1;
        JSONObject line = null;
        boolean fillMissing = false;
        if (config.getProperty("FILL_MISSING") != null && config.getProperty("FILL_MISSING").equals("1")) {
            fillMissing = true;
        }
        if (config.getProperty("AGGREGATION") != null) {
            aggr = new Integer(config.getProperty("AGGREGATION"));
        }
        try {
            // Create the file reader.
            //fileReader = new BufferedReader(new FileReader(fileToParse));

            // Read the file line by line
            boolean firstLine = true;
            int k = 0;
            while (k<jjson.toArray().length) {
                // Get all tokens available in line.
                line = jjson.getJSONObject(k);
                String[] tokens = new String[2];
                tokens[0] = line.getString("sentAt").substring(0, 10);
                tokens[1] = line.getString("payload");
                Long curTimestamp = null;

                // Check for the case where there is more than one line preceding the data
                if (tokens.length > 1) {
                    curTimestamp = (new Double(tokens[0])).longValue();
                }
                TimeSeries ts = new TimeSeries();
                ts.meta.fileName = null;
                output.add(ts);
                ts.meta.name = "timestamp";
                for (int i = 1; i < tokens.length; i++) {
                    // A naive missing data handler.
                    if (interval != null && prev != null && interval > 0 && fillMissing == true) {
                        if ((curTimestamp - prev) != interval) {
                            int missingValues = (int) ((curTimestamp - prev) / interval);

                            Long curTimestampToFill = prev + interval;
                            for (int j = (missingValues - 1); j > 0; j--) {
                                Float valToFill = new Float(tokens[i]);
                                if (output.get(i - 1).size() >= missingValues) {
                                    valToFill = output.get(i - 1).data.get(output.get(i - 1).size() - missingValues).value;
                                }
                                output.get(i - 1).append(curTimestampToFill, valToFill);
                                curTimestampToFill += interval;
                            }
                        }
                    }
                    // Infer interval.
                    if (interval == null && prev != null) {
                        interval = curTimestamp - new Long(prev);
                    }
                    output.get(i - 1).append(curTimestamp,
                            new Float(tokens[i]));

                }
                prev = curTimestamp;
                k++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Handle aggregation.
        if (aggr > 1) {
            for (TimeSeries t : output) {
                t.data = t.aggregate(aggr);
                t.meta.name += "_aggr_" + aggr;
            }
        }
        return output;
    }

    // Checks if the string is numeric.
    public static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    // Parses the string array property into an integer property.
    public static int[] splitInts(String str) throws IllegalArgumentException {
        StringTokenizer tokenizer = new StringTokenizer(str, ",");
        int n = tokenizer.countTokens();
        int[] list = new int[n];
        for (int i = 0; i < n; i++) {
            String token = tokenizer.nextToken();
            list[i] = Integer.parseInt(token);
        }
        return list;
    }

    // Initializes properties from a string (key:value, separated by ";").
    public static void initProperties(String config, Properties p) {
        String delims1 = ";";
        String delims2 = ":";

        StringTokenizer st1 = new StringTokenizer(config, delims1);
        while (st1.hasMoreElements()) {
            String[] st2 = (st1.nextToken()).split(delims2);
            p.setProperty(st2[0], st2[1]);
        }
    }
}
