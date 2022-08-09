package com.yahoo.egads.models.tsmm;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.yahoo.egads.data.TimeSeries;
import com.yahoo.egads.utilities.HttpsClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.awt.image.AreaAveragingScaleFilter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

public class SmartTrainModel extends TimeSeriesAbstractModel{
    private Properties p;
    private String predict_num;
    private String model;

    // The actual model that stores the expectations.
    protected ArrayList<Float> model_predict;

    //往后再多预测的条数
    protected ArrayList<Float> model_further;
    public SmartTrainModel(Properties config){
        super(config);
        modelName = "SmartTrainModel";
        predict_num = config.getProperty("PREDICT_NUM");
        model = config.getProperty("SMART_MODEL");
        model_further = new ArrayList<Float>();
        model_predict = new ArrayList<Float>();
        this.p = config;
    }

    @Override
    public void reset() {

    }


    public static String doPostJson(String url, String json) {
        // 创建Httpclient对象
        CloseableHttpClient httpClient = HttpClients.createDefault();
        CloseableHttpResponse response = null;
        String resultString = "";
        try {
            // 创建Http Post请求
            HttpPost httpPost = new HttpPost(url);
            // 创建请求内容
            StringEntity entity = new StringEntity(json, ContentType.APPLICATION_JSON);
            httpPost.setEntity(entity);
            // 执行http请求
            response = httpClient.execute(httpPost);
            resultString = EntityUtils.toString(response.getEntity(), "utf-8");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                response.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return resultString;
    }

    @Override
    public void train(TimeSeries.DataSequence data) throws Exception {
        int n = data.size();
        Map<Long, Float> param = new HashMap<Long, Float>();
        for(int i = 0; i < n; i++){
            param.put(data.get(i).time, data.get(i).value);
        }
        TreeMap<Long, Float> sortedMap = new TreeMap<>(param);
        String model = p.getProperty("SMART_MODEL");
        String predict_num = p.getProperty("PREDICT_NUM");
        JSONObject ob = new JSONObject();
        ob.put("data", sortedMap);
        ob.put("model", model);
        ob.put("predict_num", predict_num);
        //传输数据，指定模型，指定向后预测条数的post请求
        String paramString = JSON.toJSONString(ob);
        String port = doPostJson(p.getProperty("FLASK_URL"), paramString);
        JSONObject jj = JSON.parseObject(port);

        String predict = jj.getString("predict_data");


        Map<String, Object> predict_data = JSON.parseObject(predict, LinkedHashMap.class, Feature.OrderedField);

        for(String key : predict_data.keySet()) {
            float value = ((BigDecimal) predict_data.get(key)).floatValue();
            model_predict.add(value);
        }


        initForecastErrors(model_predict, data);
        logger.debug(getBias() + "\t" + getMAD() + "\t" + getMAPE() + "\t" + getMSE() + "\t" + getSAE() + "\t" + 0 + "\t" + 0);
    }

    @Override
    public void update(TimeSeries.DataSequence data) throws Exception {

    }

    @Override
    public void predict(TimeSeries.DataSequence sequence) throws Exception {

    }
}
