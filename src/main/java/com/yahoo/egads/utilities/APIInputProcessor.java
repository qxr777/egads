package com.yahoo.egads.utilities;



import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.yahoo.egads.control.ProcessableObject;
import com.yahoo.egads.control.ProcessableObjectFactory;
import com.yahoo.egads.data.TimeSeries;
import org.json.JSONArray;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;


public class APIInputProcessor implements InputProcessor{

    private String API = null;

    public APIInputProcessor(String API) {
        this .API=API;
    }


    @Override
    public void processInput(Properties p) throws Exception {
        //通过url拿到json数组对象
        HttpsClient getMassage = new HttpsClient();
        String massage = getMassage.sendGet(API);
        JSONObject object = JSONObject.parseObject(massage);
        com.alibaba.fastjson.JSONArray data1 = com.alibaba.fastjson.JSONArray.parseArray(object.get("data").toString());


        //处理数据
        ArrayList<TimeSeries> metrics = com.yahoo.egads.utilities.APIUtils
                .createTimeSeries(data1, p);
        int i = 0;
        for (TimeSeries ts : metrics) {
            i = 1;
            ProcessableObject po = ProcessableObjectFactory.create(ts, p);
            po.process();
            if(i==1){
                break;
            }
        }
    }
}
