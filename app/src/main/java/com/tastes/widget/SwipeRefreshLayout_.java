package com.tastes.widget;

import android.content.Context;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Created by marine1079 on 2015-03-12.
 *
 * 이렇게 해도 아직 안된다. 일단 grid empty view에서의 touch를 통한 keyboard close는 다음으로 넘긴다.
 */
public class SwipeRefreshLayout_ extends SwipeRefreshLayout {
    private Runnable onActionDown;

    public SwipeRefreshLayout_(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setOnActionDown(Runnable onActionDown) {
        this.onActionDown = onActionDown;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if(ev.getAction() == MotionEvent.ACTION_DOWN) {// 일단 down 만 해본다.
            if(onActionDown != null) {
                onActionDown.run();
            }
        }

        return super.onTouchEvent(ev);
    }
}
