package com.instamenu.util;

import android.content.Context;
import android.view.GestureDetector;
import android.view.View;

/**
 * Created by marine1079 on 2015-01-05.
 *
 * 그냥 simple- interface 바로 쓸려고 했는데, event 받은 view를 넘길 수가 없어서 직접 만들었다.
 */
public class TapDetector extends GestureDetector.SimpleOnGestureListener {
    private Context context;
    private View view;

    public TapDetector(Context context, View view) {
        this.context = context;
        this.view = view;

        //super(context, this);
    }
}
