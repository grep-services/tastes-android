package com.tastes.app;

import android.app.Activity;
import android.app.Fragment;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.devspark.robototextview.widget.RobotoTextView;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.tastes.R;
import com.tastes.content.Image;
import com.tastes.content.Tag;
import com.tastes.util.QueryWrapper;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PictureFragment extends Fragment implements Button.OnClickListener {

    // cursor는 받지 않고 그냥 생성해서 position과 함께 사용한다.
    private static final String ARG_POSITION = "position";
    private static final String ARG_LATITUDE = "latitude";
    private static final String ARG_LONGITUDE = "longitude";
    private static final String ARG_AVAILABLE = "available";

    private double latitude;
    private double longitude;
    private boolean isLocationAvailable;

    private Cursor cursor;
    private int position;

    ImageLoader imageLoader;
    DisplayImageOptions options;

    ViewPager pager;
    Button forwardButton;

    private MainActivity mActivity;
    private PictureFragmentCallbacks mCallbacks;

    public static PictureFragment newInstance(int position, double latitude, double longitude, boolean isLocationAvailable) {
        PictureFragment fragment = new PictureFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_POSITION, position);
        args.putDouble(ARG_LATITUDE, latitude);
        args.putDouble(ARG_LONGITUDE, longitude);
        args.putBoolean(ARG_AVAILABLE, isLocationAvailable);
        fragment.setArguments(args);
        return fragment;
    }
    public PictureFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            position = getArguments().getInt(ARG_POSITION);
            latitude = getArguments().getDouble(ARG_LATITUDE);
            longitude = getArguments().getDouble(ARG_LONGITUDE);
            isLocationAvailable = getArguments().getBoolean(ARG_AVAILABLE);
        }

        cursor = getActivity().getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, null, null, null);

        imageLoader = ImageLoader.getInstance();

        options = new DisplayImageOptions.Builder()
                .showImageOnLoading(R.drawable.stub_large)
                .showImageForEmptyUri(R.drawable.fail_large)
                .showImageOnFail(R.drawable.fail_large)
                        //.resetViewBeforeLoading()// iv null set 하는건데, gc는 한꺼번에 하므로, 이렇게 조금이라도 더 하는게 좋을 것 같다. -> 뭔지 잘 모르겠지만 빼둠.
                .cacheInMemory(false)// -> memory 위해 해제할까 하다가 뜨는 시간 줄이려면 차라리 넣어 두는게 나을 것 같았다.(대신 img 저장할 때 size 자체를 줄인다.)
                .cacheOnDisk(false)//TODO: 용량 많이 들어서 빼둠.
                .imageScaleType(ImageScaleType.EXACTLY) // 속도, 메모리 절약 위해.(not stretched. computed later at center crop)
                .bitmapConfig(Bitmap.Config.RGB_565)// default보다 2배 덜쓴다 한다. -> 너무 누렇게 나온다.
                        //.displayer(new FadeInBitmapDisplayer(500)) 이건 차라리 빼는게 더 빨라 보인다.
                .build();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_picture, container, false);

        // set pager
        pager = (ViewPager) view.findViewById(R.id.fragment_picture_pager);
        pager.setAdapter(new PagerAdapter_());

        pager.setCurrentItem(position);

        forwardButton = (Button) view.findViewById(R.id.fragment_picture_forward);
        forwardButton.setOnClickListener(this);

        return view;
    }

    private class PagerAdapter_ extends PagerAdapter {
        @Override
        public Object instantiateItem(ViewGroup container, final int position) {
            ImageView view = new ImageView(getActivity());
            view.setLayoutParams(new ViewGroup.LayoutParams(ViewPager.LayoutParams.MATCH_PARENT, ViewPager.LayoutParams.MATCH_PARENT));
            view.setScaleType(ImageView.ScaleType.CENTER_CROP);

            cursor.moveToPosition((getCount() - 1) - position);
            String origin = cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA));
            Uri uri = Uri.fromFile(new File(origin));

            imageLoader.displayImage(uri.toString(), view, options);

            container.addView(view);

            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public int getCount() {
            return cursor.getCount();
        }

        @Override
        public boolean isViewFromObject(View view, Object o) {
            return view == o;
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mActivity = (MainActivity) activity;

        try {
            mCallbacks = (PictureFragmentCallbacks) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    public void onForwardClicked(Image image, boolean isLocationAvailable) {
        if (mCallbacks != null) {
            mCallbacks.onPictureForwardClicked(image, isLocationAvailable);
        }
    }


    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.fragment_picture_forward:
                int position = pager.getCurrentItem();

                cursor.moveToPosition((cursor.getCount() - 1) - position);
                String origin = cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA));

                String time = String.valueOf(System.currentTimeMillis());
                // laglng 없을수도 있다.(시작하고 바로 켜면 주로 그럴듯.)
                double latitude_ = latitude;
                double longitude_ = longitude;
                long distance = -2;

                try {
                    ExifInterface exifInterface = new ExifInterface(origin);

                    float[] latlng = new float[2];

                    if(exifInterface.getLatLong(latlng)) {// 현재는 위치 있는것만 표시하는게 아니라 있는건 있는대로 표시하는 방식이다.
                        latitude_ = latlng[0];
                        longitude_ = latlng[1];

                        distance = -1;//TODO: BOOL 만들지 말고, DIST 표시는 안하지만 LATLNG AVAILABLE 하다는 표시로 단다.
                    }

                    String datetime = exifInterface.getAttribute(ExifInterface.TAG_DATETIME);
                    if (datetime != null) {
                        SimpleDateFormat format = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
                        Date date = format.parse(datetime);
                        time = String.valueOf(date.getTime());//TODO: check existence first
                    }
                } catch (IOException e) {// for exif
                    e.printStackTrace();
                } catch (ParseException e) {// for parse date
                    e.printStackTrace();
                } catch (Exception e) {// 끝까지 exception 잡아줘야 crash 안된다.(시작시 바로 종료하면 cursor때문에 npe난다.)
                    e.printStackTrace();
                }

                Image image = new Image(origin, null, time, latitude_, longitude_, distance);

                onForwardClicked(image, isLocationAvailable || (image.distance > -2));// 저것 외에는, not available한 loc이 들어왔을 뿐이다.

                break;
        }
    }

    public interface PictureFragmentCallbacks {
        public void onPictureForwardClicked(Image image, boolean isLocationAvailable);
    }
}