package com.instamenu.util;

import android.util.Log;

import com.instamenu.content.Image;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class QueryWrapper {
    // query process까지 3단계로 하면, 그게 나중에 필요할진 몰라도, 구조는 좋아도, 헷까린다. 일단 간단하게 간다.
    private static final String PATH_ADD_IMAGE = "/image/add/";
    private static final String PATH_TAG_IMAGE = "/image/tag/";
    private static final String PATH_GET_IMAGE = "/image/get/";
    private static final String PATH_GET_IMAGES = "/image/list/";
    private static final String PATH_GET_TAGS = "/tag/list/";

    private NetworkProcessor networkProcessor;

    public QueryWrapper() {
        networkProcessor = new NetworkProcessor();
    }

    public ArrayList<String> getList(String string) {
        if(string != null) {
            return new ArrayList<String>(Arrays.asList(string.split("\\,")));
        } else {
            return null;
        }
    }

    public String getString(List<String> list) {
        String string = null;

        for(String item : list) {
            if(string == null) {
                string = new String(item);
            } else {
                string += ("," + item);
            }
        }

        return string;
    }

    // add image.(create)
    public void addImage(byte[] file, String address, double latitude, double longitude, List<String> tags) {
        // set params
        List<NameValuePair> parameters = new ArrayList<NameValuePair>();
        parameters.add(new BasicNameValuePair("address", address));
        parameters.add(new BasicNameValuePair("latitude", String.valueOf(latitude)));
        parameters.add(new BasicNameValuePair("longitude", String.valueOf(longitude)));
        parameters.add(new BasicNameValuePair("tag", getString(tags)));
        // get result
        String response = networkProcessor.getResponse(PATH_ADD_IMAGE, parameters, file);
        // parse
    }
    // tag to image.(edit)
    public void tagImage(int id, List<String> tags) {
        // set params
        List<NameValuePair> parameters = new ArrayList<NameValuePair>();
        parameters.add(new BasicNameValuePair("id", String.valueOf(id)));
        parameters.add(new BasicNameValuePair("tag", getString(tags)));
        // get result
        String response = networkProcessor.getResponse(PATH_TAG_IMAGE, parameters, null);
        // parse
    }
    // get image.(get one)
    // distance는 이렇게 개개의 item을 받을 때는 넘어오지 않는다.(현재는). 따라서 id와 같이, 기존의 것을 다시 넘기고 받는다.(사실 받는 쪽에서 해도 되지만, 초기화 측면에서 여기서 했다.)
    public Image getImage(int id, long distance) {
        Image image = null;

        // set params
        List<NameValuePair> parameters = new ArrayList<NameValuePair>();
        parameters.add(new BasicNameValuePair("id", String.valueOf(id)));
        // get result
        String response = networkProcessor.getResponse(PATH_GET_IMAGE, parameters, null);
        // parse
        try {
            JSONObject imageObject = new JSONObject(response);
            JSONArray tagArray = imageObject.getJSONArray("tag");
            List<String> tags = null;
            for(int i = 0; i < tagArray.length(); i++) {
                JSONObject tagObject = tagArray.getJSONObject(i);

                if(tags == null) {
                    tags = new ArrayList<String>();
                }

                tags.add(tagObject.getString("name"));
            }

            image = new Image(imageObject.getInt("id"), imageObject.getString("origin"), imageObject.getString("thumbnail"), imageObject.getString("date"), imageObject.getString("address"), distance, tags);
        } catch (JSONException e) {
            LogWrapper.e("JSON", e.getMessage());
        }

        return image;
    }
    // get image list.(get multiple)
    /*
    사실 tags가 먼저 오고 좌표가 나중에 오는 이유는, 좌표가 없을 수도 있다는 가정을 한 것인데
    아직까지는 서버에서 좌표로 filtering해서 넘겨주기 때문에 좌표가 없으면 아무것도 받아오지 못한다.
     */
    public List<Image> getImages(List<String> tags, double latitude, double longitude) {
        List<Image> images = null;

        // set params
        List<NameValuePair> parameters = new ArrayList<NameValuePair>();
        parameters.add(new BasicNameValuePair("tag", getString(tags)));
        parameters.add(new BasicNameValuePair("latitude", String.valueOf(latitude)));
        parameters.add(new BasicNameValuePair("longitude", String.valueOf(longitude)));
        // get result
        String response = networkProcessor.getResponse(PATH_GET_IMAGES, parameters, null);
        // parse
        try {
            JSONArray imageArray = new JSONArray(response);
            for(int i = 0; i < imageArray.length(); i++) {
                JSONObject imageObject = imageArray.getJSONObject(i);
                JSONArray tagArray = imageObject.getJSONArray("tag");
                List<String> tags_ = null;
                for(int j = 0; j < tagArray.length(); j++) {
                    JSONObject tagObject = tagArray.getJSONObject(j);

                    if(tags_ == null) {
                        tags_ = new ArrayList<String>();
                    }

                    tags_.add(tagObject.getString("name"));
                }

                String distString = imageObject.getString("dist");
                long dist = Math.round(Double.valueOf(distString.replace("m", "").trim()));

                Image image = new Image(imageObject.getInt("id"), imageObject.getString("origin"), imageObject.getString("thumbnail"), imageObject.getString("date"), imageObject.getString("address"), dist, tags_);

                if(images == null) {
                    images = new ArrayList<Image>();
                }

                images.add(image);
            }
        } catch (JSONException e) {
            LogWrapper.e("JSON", e.getMessage());
        }

        return images;
    }
    // get tag.(get multiple)
    public List<String> getTags(String tag) {
        List<String> tags = null;

        // set params
        List<NameValuePair> parameters = new ArrayList<NameValuePair>();
        parameters.add(new BasicNameValuePair("tag", tag));
        // get result
        String response = networkProcessor.getResponse(PATH_GET_TAGS, parameters, null);
        // parse
        try {
            JSONArray tagArray = new JSONArray(response);
            for(int i = 0; i < tagArray.length(); i++) {
                JSONObject tagObject = tagArray.getJSONObject(i);

                if (tags == null) {
                    tags = new ArrayList<String>();
                }

                tags.add(tagObject.getString("name"));
            }
        } catch (JSONException e) {
            LogWrapper.e("JSON", e.getMessage());
        }

        return tags;
    }
}