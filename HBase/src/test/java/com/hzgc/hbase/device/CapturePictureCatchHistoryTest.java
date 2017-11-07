package com.hzgc.hbase.device;

import com.hzgc.dubbo.Attribute.*;
import com.hzgc.dubbo.dynamicrepo.SearchOption;
import com.hzgc.dubbo.dynamicrepo.SearchResult;
import com.hzgc.hbase.dynamicrepo.CapturePictureSearchServiceImpl;
import com.hzgc.hbase.dynamicrepo.DynamicTable;
import com.hzgc.hbase.staticrepo.ElasticSearchHelper;
import org.elasticsearch.action.index.IndexResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2017-11-4.
 */
public class CapturePictureCatchHistoryTest {
    public static void main(String[] args) {
        String ftpurl = "ftp://192.168.1.28:2121/3B0383FPAG00883/2017/05/23/16/00/2017_05_23_16_00_15_5704_0.jpg";
        int haircolor = 1;
        int eleglasses = 1;
        int gender = 1;
        int hairstyle = 1;
        int hat = 1;
        int huzi = 1;
        int tie = 1;
        String date = "2017-1-10";
        String pictype = "PERSON";
        String timestamp = "2017-11-04 11:17:56";
        String ipcid = "001";
        String timeslot = "1117";
        Map<String, Object> map = new HashMap<>();
        map.put("haircolor", haircolor);
        map.put("eleglasses", eleglasses);
        map.put("gender", gender);
        map.put("hairstyle", hairstyle);
        map.put("hat", hat);
        map.put("huzi", huzi);
        map.put("tie", tie);
        map.put("date", date);
        map.put("pictype", pictype);
        map.put("timestamp", timestamp);
        map.put("ipcid", ipcid);
        map.put("timeslot", timeslot);
        IndexResponse indexResponse = ElasticSearchHelper.getEsClient()
                .prepareIndex(DynamicTable.DYNAMIC_INDEX, DynamicTable.PERSON_INDEX_TYPE, ftpurl)
                .setSource(map).get();
        System.out.println(indexResponse.getVersion());
        SearchOption searchOption = new SearchOption();
        searchOption.setCount(1);
        List<Attribute> attributes = new ArrayList<>();
        Attribute attribute = new Attribute();
        attribute.setIdentify("haircolor");
        attribute.setLogistic(Logistic.AND);
        List<AttributeValue> attributeValues = new ArrayList<>();
        AttributeValue attributeValue = new AttributeValue();
        attributeValue.setValue(1);
        attributeValue.setDesc("黄色");
        AttributeValue attributeValue1 = new AttributeValue();
        attributeValue1.setValue(2);
        attributeValue1.setDesc("红色");
        attributeValues.add(attributeValue1);
        attributeValues.add(attributeValue);
        attribute.setValues(attributeValues);
        attributes.add(attribute);
        searchOption.setAttributes(attributes);
        CapturePictureSearchServiceImpl capturePictureSearchService = new CapturePictureSearchServiceImpl();
        SearchResult searchResult = capturePictureSearchService.getCaptureHistory(searchOption);
        System.out.println(searchResult);
    }
}
