package com.tastes.app;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.devspark.robototextview.util.RobotoTextViewUtils;
import com.devspark.robototextview.util.RobotoTypefaceManager;
import com.devspark.robototextview.widget.RobotoTextView;
import com.google.android.gms.maps.model.LatLng;
import com.nostra13.universalimageloader.core.imageaware.ImageAware;
import com.nostra13.universalimageloader.core.imageaware.ImageViewAware;
import com.tastes.R;
import com.tastes.content.Image;
import com.tastes.content.Tag;
import com.tastes.util.LogWrapper;
import com.tastes.util.QueryWrapper;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import org.apache.http.conn.HttpHostConnectException;

import java.util.ArrayList;
import java.util.List;

public class ItemFragment extends Fragment implements Button.OnClickListener {

    /*
    id를 받아와도 되고, obj를 받아와도 되지만,
    짧은 순간인데 새로 받을 필요까진 없다.
    obj를 intent로 serialization해서 받아온다.
    다만 새로고침을 할 수는 있게 해둔다.
     */
    private static final String ARG_IMAGES = "images";
    private static final String ARG_POSITION = "position";

    //private Image image;
    // 이제는 image list와 position을 받는다.
    private List<Image> images;
    private int position;

    private QueryWrapper queryWrapper;

    ImageLoader imageLoader;
    DisplayImageOptions options;

    ViewPager pager;

    TextView distance;//TODO: pointer for address. 이 방법 말고 더 좋은 방법(다른 페이지들에도 주소 저장 될 수 있는) 있을지 생각해보기.

    //private final String HEADER = "";

    private MainActivity mActivity;
    private ItemFragmentCallbacks mCallbacks;

    public static ItemFragment newInstance(List<Image> images, int position) {
        ItemFragment fragment = new ItemFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_IMAGES, (ArrayList<Image>) images);
        args.putInt(ARG_POSITION, position);
        fragment.setArguments(args);
        return fragment;
    }
    public ItemFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            images = (ArrayList<Image>) getArguments().getSerializable(ARG_IMAGES);
            position = getArguments().getInt(ARG_POSITION);
        }

        queryWrapper = new QueryWrapper();

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

    public int getPixel(int dp) {
        DisplayMetrics dm = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
        float density = dm.density;

        return (int)(dp * density);
    }

    public TextView getText(String tag) {
        //TextView text = new TextView(getActivity());
        TextView text = new RobotoTextView(getActivity());
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        text.setLayoutParams(layoutParams);
        int p = getPixel(16);
        text.setPadding(p, p, p, p);
        text.setIncludeFontPadding(false);// noto top bottom padding 없애기 위해.
        text.setText(Tag.HEADER + tag);
        text.setTextSize(18);
        text.setTextColor(getResources().getColor(R.color.text_inverse));
        text.setSingleLine(true);
        //text.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
        text.setId(R.id.fragment_item_tag);
        text.setTag(tag);// getText도 되지만 cast하고 parsing하는것보다 이게 더 편하다.
        text.setOnClickListener(this);

        return text;
    }

    public void fillContainer(ViewGroup container, Image image) {
        // TODO: 일단 image not null일 것이기 때문에 그냥 간다. 하지만 확실히 query wrapper까지 확인해서 null 가능성 확인해보도록 한다.
        for(int i = 0; i < image.tags.size(); i++) {
            float ratioX = image.positions != null ? Float.valueOf(image.positions.get(i).split("\\|")[0]) : 0;
            float ratioY = image.positions != null ? Float.valueOf(image.positions.get(i).split("\\|")[1]) : 0;

            TextView text = getText(image.tags.get(i));

            container.addView(text);

            // 저장시 최초 touch시의 padding 효과를 제거한 실제 xy를 저장했으므로 출력도 그대로 해주면 된다.
            text.setX((int)(container.getMeasuredWidth() * ratioX));
            text.setY((int)(container.getMeasuredHeight() * ratioY));

            if(image.orientations != null) {// 기존 image에 tag가 추가되는 기능이 activate되지 않는 한 list 내에 빈 값이 있을 일은 없다. 그래서 현재는 null check만 하면 된다.
                // pivot역시 padding을 고려할 필요 없이 그대로 xy를 이용해주면 된다는 계산.
                //container.setPivotX(text.getX());
                //container.setPivotY(text.getY());
                //text.getRootView().setPivotX(text.getX());
                //text.getRootView().setPivotY(text.getY());
                //TODO: should change pivot

                text.setRotation(Float.valueOf(image.orientations.get(i)));
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_item, container, false);

        // set pager
        pager = (ViewPager) view.findViewById(R.id.fragment_item_pager);
        pager.setAdapter(new PagerAdapter_());

        pager.setCurrentItem(position);

        return view;
    }
/*
    //TODO: npe 가능성 있음.
    public void setAddress(Address address) {
        if(address != null) {
            String result = null;

            List<String> stringList = new ArrayList<String>();

            //TODO: addressLine join한 것과 대략 비슷한지 다른 나라들도 일일히 체크해보기.
            stringList.add(address.getSubLocality());// null 확률 있음.(중국으로 치면 현 정도)
            stringList.add(address.getThoroughfare());// 동 정도.
            stringList.add(address.getSubThoroughfare());// null 확률 있음.(동 이하. 번지수 정도)

            for(String string : stringList) {
                if(string != null) {
                    if(result == null) {
                        result = string;
                    } else {
                        result += " " + string;
                    }
                }
            }

            if(result != null) {
                distance.setText(result);

                return;
            }// address 있어도 결국 null일 경우는 address null인 경우와 같이 처리된다.(즉, address 있으면서 실제 주소도 있는 경우만 주소가 표시되고 나머지는 좌표가 표시된다.)
        }

        int position = (Integer) distance.getTag();

        distance.setText(images.get(position).latitude + ", " + images.get(position).longitude);
    }

    public void requestAddress(double latitude, double longitude, TextView text) {
        distance = text;// args 순서는 저렇지만 여기서는 이걸 먼저 해야 안전할 것 같다.

        Location location  = new Location("");
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        mActivity.requestAddress(location);
    }
*/
    private class PagerAdapter_ extends PagerAdapter {
        @Override
        public Object instantiateItem(ViewGroup container, final int position) {
            View view = View.inflate(getActivity().getApplicationContext(), R.layout.pager_item, null);

            ImageView image = (ImageView) view.findViewById(R.id.pager_item_image);
            final ViewGroup container_ = (ViewGroup) view.findViewById(R.id.pager_item_container);
            final View properties = view.findViewById(R.id.pager_item_properties);

            imageLoader.displayImage("http://54.65.1.56:3639"+images.get(position).origin, image, options, new SimpleImageLoadingListener() {
                @Override
                public void onLoadingStarted(String imageUri, View view) { // 나중에 refresh를 할 때면 필요할 수도 있다.
                    container_.setVisibility(View.GONE);
                    properties.setVisibility(View.GONE);
                }

                @Override
                public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                    container_.setVisibility(View.VISIBLE);
                    properties.setVisibility(View.VISIBLE);
                }
            });

            container_.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    if(container_.getWidth() > 0 && container_.getHeight() > 0) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            container_.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        } else {
                            container_.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                        }

                        fillContainer(container_, images.get(position));
                    }
                }
            });

            Button button = ((Button) view.findViewById(R.id.pager_item_more));
            button.setTag(position);
            button.setOnClickListener(ItemFragment.this);

            TextView text = ((TextView) view.findViewById(R.id.pager_item_distance));
            text.setText(images.get(position).distance + getActivity().getResources().getString(R.string.distance_unit));
            text.setTag(position);
            text.setOnClickListener(ItemFragment.this);// activity mOnClick method 안됨. 아마 custom view context 관련 문제인듯.

            //requestAddress(images.get(position).latitude, images.get(position).longitude, text);

            String datetime;
            String strTime = images.get(position).time;
            long time = strTime != null ? (strTime.equals("null") == false ? Long.valueOf(images.get(position).time) : -1) : -1;
            long sec = time != -1 ? (System.currentTimeMillis() - time) / 1000 : 0;// 물론 time이 0인지로 비교해도 되지만 아예 없는 걸 하기 위해 -1로 했다.

            if(sec < 60) { // 초 단위(1분 미만)
                datetime = sec + getString(R.string.date_sec) + (sec > 1 ? getString(R.string.date_etc) : "");
            } else if(sec < 60 * 60) { // 분 단위(1시간 미만)
                datetime = sec / 60 + getString(R.string.date_min) + ((sec / 60) > 1 ? getString(R.string.date_etc) : "");
            } else if(sec < 60 * 60 * 24) { // 시 단위(24시간 미만)
                datetime = sec / (60 * 60) + getString(R.string.date_hour) + ((sec / (60 * 60)) > 1 ? getString(R.string.date_etc) : "");
            } else { // 일 단위(나머지 전부)
                datetime = sec / (60 * 60 * 24) + getString(R.string.date_day) + ((sec / (60 * 60 * 24)) > 1 ? getString(R.string.date_etc) : "");
            }

            //SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());// default locale이 어떤 영향을 미칠 지 확인해보기.
            //datetime = format.format(new Date(time));

            ((TextView) view.findViewById(R.id.pager_item_datetime)).setText(datetime);// null이면 그냥 안보인다.

            container.addView(view);

            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public int getCount() {
            return images != null ? images.size() : 0;
        }

        @Override
        public boolean isViewFromObject(View view, Object o) {
            return view == o;
        }
    }

    public void showToast(int resId) {
        if(getActivity() != null) {
            ((MainActivity) getActivity()).showToast(resId);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mActivity = (MainActivity) activity;

        try {
            mCallbacks = (ItemFragmentCallbacks) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    public void actionShareClicked() {
        if (mCallbacks != null) {
            mCallbacks.onItemActionShareClicked();
        }
    }

    public void actionTagClicked(String tag) {
        if (mCallbacks != null) {
            mCallbacks.onItemActionTagClicked(tag);
        }
    }

    public void moreClicked(int id) {
        if (mCallbacks != null) {
            mCallbacks.onItemMoreClicked(id);
        }
    }

    public void actionDistanceClicked(double latitude, double longitude) {
        if (mCallbacks != null) {
            mCallbacks.onItemActionDistanceClicked(latitude, longitude);
        }
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            /*
            case R.id.fragment_item_share:
                actionShareClicked();

                break;
                */
            case R.id.pager_item_more:
                //showDialog((Integer) v.getTag());
                int id = images.get((Integer) v.getTag()).id;

                moreClicked(id);

                break;
            case R.id.fragment_item_tag:
                String tag = (String) v.getTag();

                actionTagClicked(tag);

                break;
            case R.id.pager_item_distance:
                int index = (Integer) v.getTag();

                actionDistanceClicked(images.get(index).latitude, images.get(index).longitude);

                break;
        }
    }

    public interface ItemFragmentCallbacks {
        public void onItemActionShareClicked();
        public void onItemActionTagClicked(String tag);
        public void onItemMoreClicked(int id);
        public void onItemActionDistanceClicked(double latitude, double longitude);
    }
}