package com.tastes.widget;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Created by marine1079 on 2015-02-26.
 */
public class ViewPager_ extends ViewPager {

    private boolean isPagingEnabled = true;

    public ViewPager_(Context context) {
        super(context);
    }

    public ViewPager_(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return isPagingEnabled && super.onTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return isPagingEnabled && super.onInterceptTouchEvent(ev);
    }

    public void setPagingEnabled(boolean enabled) {
        isPagingEnabled = enabled;
    }
}
