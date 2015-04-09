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
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if(mCallbacks != null) {
            //mCallbacks.onHomeItemClicked((Image) adapter.getItem(position));
            //mCallbacks.onGalleryItemClicked(adapter.getImages().get(position));
            Image image = (Image) view.getTag(R.id.image_adapter_tag_object);
            mCallbacks.onGalleryItemClicked(image, isLocationAvailable || (image.distance > -2));// 저것 외에는, not available한 loc이 gallery로 들어왔을 뿐이다.
        }
    }

    public interface GalleryFragmentCallbacks {
        //public void onHomeSearchTag(String tag);
        public void onGalleryItemClicked(Image image, boolean isLocationAvailable);
    }
/*
    // splash가 activity가 아닌 상황에서는 home이 먼저 등록될 수 있다.(웬만하면) 구조를 통째로 바꾸기보다 일단 method하나만 만든다.
    public void setLocation(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;

        updateView();// 단순히 distance가 변경된 img list들이 adapter에 reset되고 notify되는 과정을 담은 method.
    }

    // 아무래도 asynctask로 빼야될 것 같다. refresh는 넣기에 좀 부적절하므로(display에서처럼 그냥 전체화면에 progress 돌린다.)
    public void setView() {
        // 기존 system에 끼워넣기에 db to list가 좋고, 어차피 sort해야 될 가능성 높다는 점에서도 그렇다.
        final List<Image> images = new ArrayList<Image>();// final이어야 해서 ref 줘야 한다.

        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                cursor.moveToFirst();
                while(!cursor.isAfterLast()) {
                    String origin = cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA));

                    try {
                        // 먼저, thumbnail이 표시되지 않을 것은 보여주지 않는다.(필요가 없다. 한계.)(hasThumbnail은 확연히 적다. 쓰지 않는다.)
                        long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID));
                        String thumbnail = null;// 일단 먼저 uri부터 채울 시도를 한다.
                        Cursor cursor_ = MediaStore.Images.Thumbnails.queryMiniThumbnail(getActivity().getContentResolver(), id, MediaStore.Images.Thumbnails.MINI_KIND, null);
                        if(cursor_ != null && cursor_.moveToFirst()) {
                            String path = cursor_.getString(cursor_.getColumnIndex(MediaStore.Images.Thumbnails._ID));
                            Uri uri = Uri.withAppendedPath(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI, path);
                            thumbnail = uri.toString();
                        }
                        Bitmap bitmap = null;
                        if(thumbnail == null) {// null이면 bitmap을 채워본다.
                            bitmap = MediaStore.Images.Thumbnails.getThumbnail(getActivity().getContentResolver(), id, MediaStore.Images.Thumbnails.MICRO_KIND, null);

                            if(bitmap == null) {
                                bitmap = MediaStore.Images.Thumbnails.getThumbnail(getActivity().getContentResolver(), id, MediaStore.Images.Thumbnails.MINI_KIND, null);

                                if(bitmap == null) {
                                    String path = cursor_.getString(cursor_.getColumnIndex(MediaStore.Images.Thumbnails._ID));
                                    bitmap = BitmapFactory.decodeFile(path);
                                }
                            }
                        }

                        if(!(thumbnail == null && bitmap == null)) {// thumbnail이 available한 경우만 표시한다.
                            ExifInterface exifInterface = new ExifInterface(origin);

                            float[] latlng = new float[2];

                            String time = String.valueOf(System.currentTimeMillis());
                            double latitude_ = 0;
                            double longitude_ = 0;
                            long distance = -2;// 이미지에 위치 X, 현위치 X.

                            if(exifInterface.getLatLong(latlng)) {// 현재는 위치 있는것만 표시하는게 아니라 있는건 있는대로 표시하는 방식이다.
                                latitude_ = latlng[0];
                                longitude_ = latlng[1];

                                distance = -1;// 이미지 위치 O, 현위치 X.

                                if (mActivity.isLocationUpdated()) {
                                    Location current = new Location("");
                                    current.setLatitude(latitude);
                                    current.setLongitude(longitude);

                                    Location location = new Location("");
                                    location.setLatitude(latitude_);
                                    location.setLongitude(longitude_);

                                    distance = (long) current.distanceTo(location);
                                }

                                String datetime = exifInterface.getAttribute(ExifInterface.TAG_DATETIME);
                                if (datetime != null) {
                                    SimpleDateFormat format = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
                                    Date date = format.parse(datetime);
                                    time = String.valueOf(date.getTime());//TODO: check existence first
                                }

                                Image image = new Image(origin, thumbnail, bitmap, time, latitude, longitude, distance);

                                images.add(image);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (ParseException e) {// for parse date
                        e.printStackTrace();
                    } catch (Exception e) {// 끝까지 exception 잡아줘야 crash 안된다.(시작시 바로 종료하면 cursor때문에 npe난다.)
                        e.printStackTrace();
                    }

                    cursor.moveToNext();
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void unused) {
                super.onPostExecute(unused);

                if(isAdded()) {// 재현 자주 되지는 않지만 attatched안됐는데 getString 등 한다고 error 난다. onPost에서는 주로 이렇게 해주라는 의견들이 있다.
                    waitView.setVisibility(View.GONE);

                    // images가 distance 있는 image들로 구성되어있다면 sort한다.(아니면 list에 넣을 때 sort하면서 넣거나. => 이게 낫겠다.)

                    // set to adapter.
                    adapter.setImages(images);
                }
            }
        };

        // 11부터는 serial이 default라서.
        if(Build.VERSION.SDK_INT< Build.VERSION_CODES.HONEYCOMB) task.execute();
        else task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void updateView() {
        List<Image> images = adapter.getImages();

        if(images != null) {
            for(Image image : images) {
                if(image.distance == -1) {// 이미지 위치 O, 현위치 X 였던 경우만.
                    Location current = new Location("");
                    current.setLatitude(this.latitude);
                    current.setLongitude(this.longitude);

                    Location location = new Location("");
                    location.setLatitude(image.latitude);
                    location.setLongitude(image.longitude);

                    image.distance = (long) current.distanceTo(location);
                }
            }
        }

        adapter.setImages(images);
    }
*/
}