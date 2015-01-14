package com.instamenu.content;

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
    public String time;
    public String address;
    public long distance;
    public List<String> tags;
    public List<String> positions;

    // 자세한 setter, getter들은 나중에 datetime, location들을 str에서 제대로 된 object들로 바꾸고 나서(tag도 tag custom object 만들게 될 수도 있다.) 만든다.

    public Image(int id, String origin, String thumbnail, String time, String address, long distance, List<String> tags, List<String> positions) {
        this.id = id;
        this.origin = origin;
        this.thumbnail = thumbnail;
        this.time = time;
        this.address = address;
        this.distance = distance;
        this.tags = tags;
        this.positions = positions;
    }
}