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
        if(!(dest.toString().equals(mHeader) && source.length() == 0)) { // 일반적인 경우.
            //if(!(patternDefault.matcher(source).matches() || patternKorean.matcher(source).matches())) {
            if(!pattern.matcher(source).matches()) {
                return "";
            }
        } else { // header뿐인데 back 누를 경우
            return dest.subSequence(dstart, dend);
        }
/*
될 것 같지만 안된다. 공간이 계속 같이 남아서 붙어 다닌다. 그냥 두는게 차라리 나을 것 같아서 일단 뺀다.
        // 변경 후 예상되는 문자열
        String expected = new String();
        expected += dest.subSequence(0, dstart);
        expected += source.subSequence(start, end);
        expected += dest.subSequence(dend, dest.length());

        mEdit.setEms(expected.length());
*/
        return null;
    }
}