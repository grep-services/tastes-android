package com.instamenu.util;

import android.text.InputFilter;
import android.text.Spanned;

import java.util.regex.Pattern;

/**
 * Created by marine1079 on 2015-01-03.
 */
public class DefaultFilter implements InputFilter{
    private String mHeader;

    Pattern patternDefault = Pattern.compile("^[a-zA-Z0-9_]*$"); // english, number, underline
    Pattern patternKorean = Pattern.compile("^[가-힣ㄱ-ㅎㅏ-ㅣ\\u318D\\u119E\\u11A2\\u2022\\u2025a\\u00B7\\uFE55]*$");

    public DefaultFilter(String mHeader) {
        this.mHeader = mHeader;
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        if(!(dest.toString().equals(mHeader) && source.length() == 0)) { // 일반적인 경우.
            if (!(patternDefault.matcher(source).matches() || patternKorean.matcher(source).matches())) {
                return "";
            }
        } else { // header뿐인데 back 누를 경우
            return dest.subSequence(dstart, dend);
        }

        return null;
    }
}
