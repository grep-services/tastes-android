package com.tastes.app;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.location.Location;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.InputFilter;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;

import com.devspark.robototextview.widget.RobotoEditText;
import com.nostra13.universalimageloader.core.listener.PauseOnScrollListener;
import com.tastes.R;
import com.tastes.content.Image;
import com.tastes.content.Tag;
import com.tastes.util.ByteLengthFilter;
import com.tastes.util.DefaultFilter;
import com.tastes.util.LogWrapper;
import com.tastes.util.QueryWrapper;
import com.tastes.widget.ImageAdapter;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.tastes.widget.SwipeRefreshLayout_;

import org.apache.http.conn.HttpHostConnectException;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/*
사실 현위치 몰라도 올릴 수 있다. 하지만 그렇게 하도록 해버리면, 현위치 받는 도중의 ui는 가능해도, 못받았을 경우 어떡할지(그냥 표시 안할지),
그러다보면 결국 refresh가 어떤 의미로 존재하는지 확실히 알기 힘든 사용자들도 생길 수 있다고 보았다.
따라서, 다른 page들의 컨셉과 달리, 여기서는 위치가 필요없음에도 위치가 있어야만 올릴 수 있도록 한다.

아니면, 이런 방법이 있다.
위치 있으면 거리순 정렬해서 거리 달아서 표시해주고, 위치 없으면 그냥 갤러리 그대로 표시해주는 방식.

가만히 생각해보니 이 방식이 나중을 위해서도 더 나을 것 같다.
위치가 없어도 업로드 가능하다는 점과(이미 사용자가 그것을 인식하고 있을 때), 다른 페이지들의 컨셉처럼 최소 요건(현위치는 아님)만 갖춘다는 점에서 그렇다.

결론 : 위치 있으면 거리순 정렬과 거리 달아 표시, 위치 없으면 그냥 갤러리 그대로 표시.(위치 달려 있는 것을 필터링은 하기). 따라서 refresh 필요 없다.
 */
public class GalleryFragment extends Fragment implements GridView.OnItemClickListener/*, Button.OnClickListener*/ {
    private static final String ARG_LATITUDE = "latitude";
    private static final String ARG_LONGITUDE = "longitude";
    private static final String ARG_AVAILABLE = "available";

    private double latitude;
    private double longitude;
    private boolean isLocationAvailable;

    ImageLoader imageLoader;

    Cursor cursor;

    GridView grid;
    ImageAdapter adapter;

    View emptyView/*, waitView*/;

    private MainActivity mActivity;
    private GalleryFragmentCallbacks mCallbacks;

    public static GalleryFragment newInstance(double latitude, double longitude, boolean isLocationAvailable) {
        GalleryFragment fragment = new GalleryFragment();

        Bundle args = new Bundle();
        args.putDouble(ARG_LATITUDE, latitude);
        args.putDouble(ARG_LONGITUDE, longitude);
        args.putBoolean(ARG_AVAILABLE, isLocationAvailable);
        fragment.setArguments(args);

        return fragment;
    }

    public GalleryFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            latitude = getArguments().getDouble(ARG_LATITUDE);
            longitude = getArguments().getDouble(ARG_LONGITUDE);
            isLocationAvailable = getArguments().getBoolean(ARG_AVAILABLE);
        }

        /*
        thumbnail 없을수도 있고, 있어도 kind가 나눠지는 등, 일단 기본으로 cursor를 잡는다.
        일단 테스트로, thumbnail부터 받고, 실제 img를 보내는 방식 해본다.
        그렇게 되면, thumbnail 없는건 안나오고, size도 일단 안받게 된다.(land 구별 못함)
        앞으로 실제로는, 실제img에서 thumbnail을 참조하는 것이고, thumbnail 없으면 만드는데 mini로 만든다.
         */
        cursor = getActivity().getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, null, null, null);
        //cursor = getActivity().getContentResolver().query(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI, null, null, null, null);

        imageLoader = ImageLoader.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_gallery, container, false);

        emptyView = view.findViewById(R.id.fragment_gallery_empty);

        grid = (GridView) view.findViewById(R.id.fragment_gallery_grid);

        //adapter = new ImageAdapter(getActivity(), inflater, imageLoader, true);
        adapter = new ImageAdapter(getActivity(), inflater, imageLoader, cursor, latitude, longitude);

        grid.setAdapter(adapter);
        grid.setOnItemClickListener(this);
        grid.setEmptyView(emptyView);
        //TODO: scroll false로 하면, touch시에 결국 update를 하긴 하지만, 필요할 것 같아서 넣는다.(그래도 fling자체에는 안된다는 점에서 괜찮긴 하다.)
        //TODO: 하지만, scroll false로 해봤더니, 더 많은 사진들 있는 곳에서는 너무 느리다. 일단 true.
        grid.setOnScrollListener(new PauseOnScrollListener(adapter.getImageLoader(), true, true));
/*
        waitView = view.findViewById(R.id.fragment_gallery_wait);
        waitView.setVisibility(View.VISIBLE);
*/
        //setView();

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mActivity = (MainActivity) activity;

        try {
            mCallbacks = (GalleryFragmentCallbacks) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement GalleryFragmentCallbacks");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cursor.close();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if(mCallbacks != null) {
            Image image = (Image) view.getTag(R.id.image_adapter_tag_object);
            mCallbacks.onGalleryItemClicked(image, isLocationAvailable || (image.distance > -2));// 저것 외에는, not available한 loc이 gallery로 들어왔을 뿐이다.
        }
    }

    public interface GalleryFragmentCallbacks {
        public void onGalleryItemClicked(Image image, boolean isLocationAvailable);
    }
}