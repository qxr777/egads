// main entry point for egads processing node

package com.yahoo.egads;

import java.util.Properties;
import java.io.FileInputStream;
import java.io.InputStream;
import com.yahoo.egads.utilities.*;
import java.io.File;

/*
 * Call stack.
 *
 * Anomaly Dtection Use Case (Assuming a trained model).
 * Egads.Main()
 *   ProcessableObjectFactory.create
 *     Create AnomalyDetector
 *       BuildADModel()
 *     Create ModelAdapter
 *       BuildTSModel()
 *   po.process
 *     TODO: write to anomaly DB.
 *
 */

public class Egads {
    public static void main(String[] args) throws Exception {
        long startTime=System.currentTimeMillis(); //获取开始时间

        if (args.length == 0) {
            System.err.println("Usage: java Egads config.ini (input [STDIN,CSV])");
            System.exit(1);
        }

        // TODO: This config will be retrieved from ConfigDB later,
        // for now it is assumed it's a static file.
        Properties p = new Properties();
        String config = args[0];
        File f = new File(config);
        boolean isRegularFile = f.exists();
        String reg = "http://\\S+";
        if (isRegularFile) {
            InputStream is = new FileInputStream(config);
            //p是配置文件的实现化
            p.load(is);
        } else {
            FileUtils.initProperties(config, p);
        }

        // Set the input type.
        InputProcessor ip = null;
        //获取输入文件实例化为ip
        if (p.getProperty("INPUT") == null || p.getProperty("INPUT").equals("CSV")) {
            ip = new FileInputProcessor(args[1]);
        } else if (p.getProperty("INPUT").equals("API")) {
            ip = new APIInputProcessor(args[1]);
        } else {
            ip = new StdinProcessor();
        }

        // Process the input the we received (either STDIN or as a file).
        ip.processInput(p);
        long endTime=System.currentTimeMillis(); //获取结束时间
        System.out.println("程序运行时间： "+(endTime-startTime)+"ms");
    }
}
