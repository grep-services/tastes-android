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
import android.widget.GridView;

import com.instamenu.R;
import com.instamenu.content.Image;
import com.instamenu.widget.ImageAdapter;

public class HomeFragment extends Fragment {
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        grid = (GridView) view.findViewById(R.id.fragment_home_grid);

        adapter = new ImageAdapter(inflater);

        grid.setAdapter(adapter);

        adapter.addImage(new Image(1, "url", "date", "address", 300, null));

        return view;
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

    public interface HomeFragmentCallbacks {
        public void onHomeInitActionBar();
        public void onHomeActionHomeClicked();
        public void onHomeActionFilterClicked();
    }
}
