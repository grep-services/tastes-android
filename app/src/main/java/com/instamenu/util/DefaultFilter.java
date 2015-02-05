package com.instamenu.util;

import android.text.InputFilter;
import android.text.Spanned;
import android.widget.EditText;

import com.instamenu.content.Tag;

import java.util.regex.Pattern;

/**
 * Created by marine1079 on 2015-01-03.
 */
public class DefaultFilter implements InputFilter{
    //private EditText mEdit;
    //private String mHeader;

    //Pattern patternDefault = Pattern.compile("^[a-zA-Z0-9_]*$"); // english, number, underline
    //Pattern patternKorean = Pattern.compile("^[가-힣ㄱ-ㅎㅏ-ㅣ\\u318D\\u119E\\u11A2\\u2022\\u2025a\\u00B7\\uFE55]*$");
    // 따로 하고 싶지만 따로 하면 123가나다 이런게 입력이 안된다. 일단 원인 찾을때까지는 이렇게 간다.
    // header가 source에 있다 없다 해서 힘들어서 결국 expected를 검사하기로 했고 그래서 pattern 자체에 이렇게 header를 넣었다.
    Pattern pattern = Pattern.compile("^"+Tag.HEADER+"[a-zA-Z0-9_가-힣ㄱ-ㅎㅏ-ㅣ\\u318D\\u119E\\u11A2\\u2022\\u2025a\\u00B7\\uFE55]*$");

    public DefaultFilter() {
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        String expected = new String();
        expected += dest.subSequence(0, dstart);
        expected += source.subSequence(start, end);
        expected += dest.subSequence(dend, dest.length());
        if(!expected.startsWith(Tag.HEADER)) { // header 지우는 것 뿐만 아니라 header 앞뒤에서 텍스트 생성되는 것도 다 막는다.
            return dest.subSequence(dstart, dend);
        } else {
            if(!pattern.matcher(expected).matches()) { // 그건 아니라 하더라도 pattern 안맞으면 그것도 안되게 한다.
                if(dend - dstart == dest.length()) { // 골때리지만 자동완성 같은건 header(destination)까지 통째로 바꿔버려서 이렇게 empty로 초기화될 판에는 다시 return시켜줬다.
                    return Tag.HEADER;
                } else { // 그런것들 다 제외한 나머지 일반적인 mismatch시에는 그냥 이렇게 무반응 하도록 했다.
                    return "";
                }
            }
        }

        return null;
    }
}