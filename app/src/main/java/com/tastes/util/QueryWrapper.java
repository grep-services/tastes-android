package com.tastes.util;

import com.tastes.content.Image;

import org.apache.http.NameValuePair;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
        if(string != null && string.equals("null") == false) {
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

    public void testUtf(byte[] file, String string) throws HttpHostConnectException {
        // set params
        List<NameValuePair> parameters = new ArrayList<NameValuePair>();
        // 일단 보낼 필요도 없다. 괜히 method에서 null때문에 exception이나 나고(network processor에서) 일단 보내지 않고 놔둔다.
        //parameters.add(new BasicNameValuePair("address", address));
        parameters.add(new BasicNameValuePair("string", string));
        // get result
        String response = networkProcessor.getResponse("/test/utf/", parameters, file);
        // parse
    }

    // add image.(create)
    public void addImage(byte[] file, long time, double latitude, double longitude, List<String> tags, List<String> positions) throws HttpHostConnectException {
        // set params
        List<NameValuePair> parameters = new ArrayList<NameValuePair>();
        // 일단 보낼 필요도 없다. 괜히 method에서 null때문에 exception이나 나고(network processor에서) 일단 보내지 않고 놔둔다.
        parameters.add(new BasicNameValuePair("time", String.valueOf(time)));
        LogWrapper.e("TIME", ": "+time);
        //parameters.add(new BasicNameValuePair("address", address));
        parameters.add(new BasicNameValuePair("latitude", String.valueOf(latitude)));
        parameters.add(new BasicNameValuePair("longitude", String.valueOf(longitude)));
        parameters.add(new BasicNameValuePair("tag", getString(tags)));
        parameters.add(new BasicNameValuePair("positions", getString(positions)));
        // get result
        String response = networkProcessor.getResponse(PATH_ADD_IMAGE, parameters, file);

        if(response.contains("502 Bad Gateway")) {
            // 여기도 어차피 가만히 놔두면 json exception으로 넘어간다.
            throw new HttpHostConnectException(null, null);// null 괜찮은지 모르겠다.(일단 괜찮은 것 같긴 하다.)
        }
        // parse
    }
    // tag to image.(edit)
    public void tagImage(int id, List<String> tags) throws HttpHostConnectException {
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
    public Image getImage(int id, long distance) throws HttpHostConnectException {
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

            List<String> positions = getList(imageObject.getString("positions"));

            String datetime = null;
            if(imageObject.getString("time").equals("null") == false) {
                long time = Long.valueOf(imageObject.getString("time"));// null인 것들은 exception 날 준비 한다.
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());// default locale이 어떤 영향을 미칠 지 확인해보기.
                datetime = format.format(new Date(time));
            }

            image = new Image(imageObject.getInt("id"), imageObject.getString("origin"), imageObject.getString("thumbnail"), datetime, imageObject.getString("address"), distance, tags, positions);
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
    public List<Image> getImages(List<String> tags, double latitude, double longitude) throws HttpHostConnectException {

        if(tags == null) return null;// 다 묶을 필요까지는 없고, 아무튼 filter null일 경우까지도 생각해야 된다는 말이다. 실제로 filter 다 지우면 exception 떴다.

        List<Image> images = null;

        // set params
        List<NameValuePair> parameters = new ArrayList<NameValuePair>();
        parameters.add(new BasicNameValuePair("tag", getString(tags)));
        parameters.add(new BasicNameValuePair("latitude", String.valueOf(latitude)));
        parameters.add(new BasicNameValuePair("longitude", String.valueOf(longitude)));
        // get result
        String response = networkProcessor.getResponse(PATH_GET_IMAGES, parameters, null);

        if(response == null) {
            return null;// 보통 network 끊기면 여기서 걸릴 것이다.(splash든 refresh든)
        } else if(response.contains("502 Bad Gateway")) {
            // 직접 하는것도 사실 좀 그렇긴 하지만, 가만 놔둬서 json exception으로 넘어가고, 그럼에도 그냥 null 취급 당해서 empty와 구분도 안가게 놔두는 것 보다는 낫다.
            throw new HttpHostConnectException(null, null);// null 괜찮은지 모르겠다.(일단 괜찮은 것 같긴 하다.)
        }
        // parse
        try {
            JSONArray imageArray = new JSONArray(response);
            for(int i = 0; i < imageArray.length(); i++) {
                JSONObject imageObject = imageArray.getJSONObject(i);
                List<String> tags_ = getList(imageObject.getString("tag_str"));
                if(tags_ == null) { // 일단 이렇게 중첩으로 간다. 이유는 기존의 것들은 tag_str 없고, 이 밑의 if 해두는 이유는 나중에라도 tag parameter가 필요할 수 있기 때문에.(edit 등.)
                    JSONArray tagArray = imageObject.getJSONArray("tag");
                    for(int j = 0; j < tagArray.length(); j++) {
                        JSONObject tagObject = tagArray.getJSONObject(j);

                        if(tags_ == null) {
                            tags_ = new ArrayList<String>();
                        }

                        tags_.add(tagObject.getString("name"));
                    }
                }

                String distString = imageObject.getString("dist");
                long dist = Math.round(Double.valueOf(distString.replace("m", "").trim()));

                List<String> positions = getList(imageObject.getString("positions"));

                Image image = new Image(imageObject.getInt("id"), imageObject.getString("origin"), imageObject.getString("thumbnail"), imageObject.getString("time"), imageObject.getString("address"), dist, tags_, positions);

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
    public List<String> getTags(String tag) throws HttpHostConnectException {
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