package com.instamenu.app;

import android.app.Activity;
import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;

import com.instamenu.R;
import com.instamenu.content.Image;
import com.instamenu.util.LogWrapper;
import com.instamenu.util.NetworkProcessor;
import com.instamenu.util.QueryWrapper;
import com.instamenu.widget.ImageAdapter;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment implements GridView.OnItemClickListener {
    private static final String ARG_LATITUDE = "latitude";
    private static final String ARG_LONGITUDE = "longitude";

    private double latitude;
    private double longitude;

    private QueryWrapper queryWrapper;

    ImageLoader imageLoader;

    GridView grid;
    ImageAdapter adapter;

    private MainActivity mActivity;
    private HomeFragmentCallbacks mCallbacks;

    public static HomeFragment newInstance(double latitude, double longitude) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putDouble(ARG_LATITUDE, latitude);
        args.putDouble(ARG_LONGITUDE, longitude);
        fragment.setArguments(args);
        return fragment;
    }
    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        if (getArguments() != null) {
            latitude = getArguments().getDouble(ARG_LATITUDE);
            longitude = getArguments().getDouble(ARG_LONGITUDE);
        }

        queryWrapper = new QueryWrapper();

        imageLoader = ImageLoader.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        /*
        ********************** filter ok 하고서 이쪽으로 온다. 즉, view 설정 등이 또 되는데.....
         */
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        grid = (GridView) view.findViewById(R.id.fragment_home_grid);

        adapter = new ImageAdapter(inflater, imageLoader);

        grid.setAdapter(adapter);
        grid.setOnItemClickListener(this);

        setView();

        return view;
    }

    public void setView() {
        LogWrapper.e("HOME", "set view");
        // get tags from pref.
        //=> pull to refresh 등에서는 pref에서 tags 받아올 필요 없을 거라고 생각할수도 있지만, tag를 1번만 받아오는 구조라면, 나중에 display나 filter 등에서 넘어올 때
        // 다른 정보를 더 받아와야 되는 등, 점점더 골치아파진다.(왜냐하면 이 home frag는 한번 실행되면 계속 남아있으므로 pref도 그대로일 것이기 때문.)
        final List<String> tags = mActivity != null ? mActivity.getFilters() : null;
        // get images from server with tags.
        AsyncTask<Void, Void, List<Image>> task = new AsyncTask<Void, Void, List<Image>>() {
            @Override
            protected List<Image> doInBackground(Void... params) {
                return queryWrapper.getImages(tags, latitude, longitude);
            }

            @Override
            protected void onPostExecute(List<Image> images) {
                super.onPostExecute(images);

                // set to adapter.
                adapter.setImages(images);
            }
        };

        // 11부터는 serial이 default라서.
        if(Build.VERSION.SDK_INT< Build.VERSION_CODES.HONEYCOMB) task.execute();
        else task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mActivity = (MainActivity) activity;

        try {
            mCallbacks = (HomeFragmentCallbacks) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    public void initActionBar() {
        if(mCallbacks != null) {
            mCallbacks.onHomeInitActionBar();
        }
    }

    public void actionHomeClicked() {
        if(mCallbacks != null) {
            mCallbacks.onHomeActionHomeClicked();
        }
    }

    public void actionFilterClicked() {
        if(mCallbacks != null) {
            mCallbacks.onHomeActionFilterClicked();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.home, menu);

        initActionBar();

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                actionHomeClicked();

                break;
            case R.id.action_filter:
                actionFilterClicked();

                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if(mCallbacks != null) {
            mCallbacks.onHomeItemClicked((Image) adapter.getItem(position));
        }
    }

    public interface HomeFragmentCallbacks {
        public void onHomeInitActionBar();
        public void onHomeActionHomeClicked();
        public void onHomeActionFilterClicked();
        public void onHomeItemClicked(Image image);
    }
}
