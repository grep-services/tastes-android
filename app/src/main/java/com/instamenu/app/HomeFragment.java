package com.instamenu.app;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import com.instamenu.R;
import com.instamenu.content.Image;
import com.instamenu.util.LogWrapper;
import com.instamenu.widget.ImageAdapter;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment implements GridView.OnItemClickListener {
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    ImageLoader imageLoader;

    GridView grid;
    ImageAdapter adapter;

    private HomeFragmentCallbacks mCallbacks;

    public static HomeFragment newInstance(String param1, String param2) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
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
            //mParam1 = getArguments().getString(ARG_PARAM1);
            //mParam2 = getArguments().getString(ARG_PARAM2);
        }

        imageLoader = ImageLoader.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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
        // get images from server with tags.
        List<String> list = new ArrayList<String>();
        list.add("tag0");
        Image image = new Image(19, "http://54.65.1.56:3639/media/origin/img1_jTUpfOP.jpg", "http://54.65.1.56:3639/media/thumbnail/img1_FjdDYrJ.jpg", "2014-12-12T19:31:41.533835Z", "address10", 300, list);
        List<Image> images = new ArrayList<Image>();
        images.add(image);
        // set to adapter.
        adapter.setImages(images);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
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
