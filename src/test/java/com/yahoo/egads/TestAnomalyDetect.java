/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the GPL License.
 * See the accompanying LICENSE file for terms.
 */

package com.yahoo.egads;

import com.alibaba.fastjson.JSONObject;
import com.yahoo.egads.control.AnomalyDetector;
import com.yahoo.egads.control.ModelAdapter;
import com.yahoo.egads.control.ProcessableObjectFactory;
import com.yahoo.egads.models.tsmm.OlympicModel;
import com.yahoo.egads.models.adm.*;
import com.yahoo.egads.data.Anomaly.IntervalSequence;
import com.yahoo.egads.data.*;

import java.util.*;
import java.io.FileInputStream;
import java.io.InputStream;

import com.yahoo.egads.utilities.HttpsClient;
import org.testng.Assert;
import org.testng.annotations.Test;
// Tests the basic anoamly detection piece of EGADS.
public class TestAnomalyDetect {

    @Test
    public void testAutoModel() throws Exception {
        String configFile = "src/test/resources/sample_config.ini";
        InputStream is = new FileInputStream(configFile);
        Properties p = new Properties();
        p.load(is);
        HttpsClient getMassage = new HttpsClient();
        String massage = getMassage.sendGet("http://192.168.100.100:8080/messages?measurement=/SenseHatB/socc3dzf83ty/Temperature&period=7200");
        JSONObject object = JSONObject.parseObject(massage);
        com.alibaba.fastjson.JSONArray data1 = com.alibaba.fastjson.JSONArray.parseArray(object.get("data").toString());
        ArrayList<TimeSeries> actual_metric = com.yahoo.egads.utilities.APIUtils.createTimeSeries(data1, p);
        ArrayList<TimeSeries> second_metric = com.yahoo.egads.utilities.APIUtils.createTimeSeries(data1, p);
        TimeSeries.DataSequence td = actual_metric.get(0).data;
        ModelAdapter ma = ProcessableObjectFactory.buildTSModel(actual_metric.get(0), p);
        AnomalyDetector ad = ProcessableObjectFactory.buildAnomalyModel(actual_metric.get(0), p);
        ma.reset();
        ma.train();
        ArrayList<TimeSeries.DataSequence> list = ma.forecast(
                ma.metric.startTime(), ma.metric.lastTime());
        ArrayList<Anomaly> anomalyList = new ArrayList<>();
        IntervalSequence intervals = new IntervalSequence();
        for (TimeSeries.DataSequence ds : list) {
            ad.reset();
            ad.tune(ds);

            anomalyList = ad.detect(ad.metric, ds);
            Anomaly anomaly = anomalyList.get(0);
            intervals = anomaly.intervals;
        }
        Assert.assertTrue(intervals.size()>0);
    }
    @Test
    public void testOlympicModel() throws Exception {
        // Test cases: ref window: 10, 5
        // Drops: 0, 1
        String[] refWindows = new String[]{"10", "5"};
        String[] drops = new String[]{"0", "1"};
        // Load the true expected values from a file.
        String configFile = "src/test/resources/sample_config.ini";
        InputStream is = new FileInputStream(configFile);
        Properties p = new Properties();
        p.load(is);
        ArrayList<TimeSeries> actual_metric = com.yahoo.egads.utilities.FileUtils
                .createTimeSeries("src/test/resources/model_input.csv", p);
        p.setProperty("MAX_ANOMALY_TIME_AGO", "999999999");
        for (int w = 0; w < refWindows.length; w++) {
            for (int d = 0; d < drops.length; d++) {
                 p.setProperty("NUM_WEEKS", refWindows[w]);
                 p.setProperty("NUM_TO_DROP", drops[d]);
                 p.setProperty("THRESHOLD", "mapee#100,mase#10");
                 // Parse the input timeseries.
                 ArrayList<TimeSeries> metrics = com.yahoo.egads.utilities.FileUtils
                            .createTimeSeries("src/test/resources/model_output_" + refWindows[w] + "_" + drops[d] + ".csv", p);
                 OlympicModel model = new OlympicModel(p);
                 model.train(actual_metric.get(0).data);
                 TimeSeries.DataSequence sequence = new TimeSeries.DataSequence(metrics.get(0).startTime(),
                                                                                metrics.get(0).lastTime(),
                                                                                3600);
                 sequence.setLogicalIndices(metrics.get(0).startTime(), 3600);
                 model.predict(sequence);
                 // Initialize the anomaly detector.
                 ExtremeLowDensityModel bcm = new ExtremeLowDensityModel(p);

                 // Initialize the DBScan anomaly detector.
                 DBScanModel dbs = new DBScanModel(p);
                 IntervalSequence anomalies = bcm.detect(actual_metric.get(0).data, sequence);
                 dbs.tune(actual_metric.get(0).data, sequence);
                 IntervalSequence anomaliesdb = dbs.detect(actual_metric.get(0).data, sequence);

                 // Initialize the SimpleThreshold anomaly detector.
                 SimpleThresholdModel stm = new SimpleThresholdModel(p);

                 stm.tune(actual_metric.get(0).data, sequence);
                 IntervalSequence anomaliesstm = stm.detect(actual_metric.get(0).data, sequence);
                 Assert.assertTrue(anomalies.size() > 10);
                 Assert.assertTrue(anomaliesdb.size() > 2);
                 Assert.assertTrue(anomaliesstm.size() > 2);
            }
        }
    }
}
