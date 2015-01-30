package com.instamenu.util;

import android.text.InputFilter;
import android.text.Spanned;
import android.widget.EditText;

import java.util.regex.Pattern;

/**
 * Created by marine1079 on 2015-01-03.
 */
public class DefaultFilter implements InputFilter{
    //private EditText mEdit;
    private String mHeader;

    //Pattern patternDefault = Pattern.compile("^[a-zA-Z0-9_]*$"); // english, number, underline
    //Pattern patternKorean = Pattern.compile("^[가-힣ㄱ-ㅎㅏ-ㅣ\\u318D\\u119E\\u11A2\\u2022\\u2025a\\u00B7\\uFE55]*$");
    // 따로 하고 싶지만 따로 하면 123가나다 이런게 입력이 안된다. 일단 원인 찾을때까지는 이렇게 간다.
    Pattern pattern = Pattern.compile("^[a-zA-Z0-9_가-힣ㄱ-ㅎㅏ-ㅣ\\u318D\\u119E\\u11A2\\u2022\\u2025a\\u00B7\\uFE55]*$");

    public DefaultFilter(/*EditText mEdit, */String mHeader) {
        //this.mEdit = mEdit;
        this.mHeader = mHeader;
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        String expected = new String();
        expected += dest.subSequence(0, dstart);
        expected += source.subSequence(start, end);
        expected += dest.subSequence(dend, dest.length());
        //if(!expected.contains(mHeader)) { // header를 지울려고 하면 안되게 한다.
        if(!expected.startsWith(mHeader)) { // header 지우는 것 뿐만 아니라 header 앞뒤에서 텍스트 생성되는 것도 다 막는다.
            return dest.subSequence(dstart, dend);
        } else {
            if(!pattern.matcher(source).matches()) { // 그건 아니라 하더라도 pattern 안맞으면 그것도 안되게 한다.
                return "";
            }
        }

        return null;
    }
}