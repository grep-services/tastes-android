package com.tastes.content;

import android.graphics.Bitmap;

import java.io.Serializable;
import java.util.List;

/**
 * Created by marine1079 on 2014-12-01.
 *
 * 오래 쓰기 위해 잘 갖춰진 구조(fk 등 이용한)가 아니다.
 * instant distance도 받아와서 갖고 있고, tag도 바로 str로 갖고 있는 등
 * 어차피 server에서 filtering해서 가져오는 소량 자료인 만큼
 * 필요한 내용들만 갖고 있으면서 편하게 쓸 수 있는 컨셉이다.
 */
public class Image implements Serializable {
    public int id;
    public String origin;
    public String thumbnail;
    //public Bitmap bitmap;// thumbnail 최대한 안놓치는게 좋다. 이렇게 해도 놓칠건 놓칠수도 있겠지만 할만큼 하면 된다.
    public String time;
    //public String address; 어차피 주소가 있어도 sub역할이지 실제 상호는 tag에 달리므로 address는 image가 갖고 있을 만한 성질의 것이 아니다. 확실히 하기 위해 뺀다.
    // 좌표는 현재 실제 query get시에는 쓰이지 않지만 gallery에서 img 받아와서 저장하기 위한 용도로 쓴다.
    public double latitude;
    public double longitude;
    public long distance;
    //public boolean available;// latlng avilable(for gallery)
    public List<String> tags;
    public List<String> positions;
    //public List<String> orientations;

    // 자세한 setter, getter들은 나중에 datetime, location들을 str에서 제대로 된 object들로 바꾸고 나서(tag도 tag custom object 만들게 될 수도 있다.) 만든다.

    // for gallery(img obj는 어차피 display에서 parse되어 쓰일 용도지만 grid, display에서 최대한 편하게 쓰일 수 있게 하기 위해 이렇게 obj를 사용한다.
    public Image(String origin, String thumbnail/*, Bitmap bitmap*/, String time, double latitude, double longitude, long distance) {
        this(0, origin, thumbnail/*, bitmap*/, time, /*null, */latitude, longitude, distance, null, null/*, null*/);
    }

    // default(for query)
    public Image(int id, String origin, String thumbnail, String time, /*String address,*/ double latitude, double longitude, long distance, List<String> tags, List<String> positions/*, List<String> orientations*/) {
        this.id = id;
        this.origin = origin;
        this.thumbnail = thumbnail;
        //this.bitmap = bitmap;
        this.time = time;
        //this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.distance = distance;
        this.tags = tags;
        this.positions = positions;
        //this.orientations = orientations;
    }
}