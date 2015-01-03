package com.instamenu.app;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.graphics.Camera;
import android.os.Bundle;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.instamenu.R;

import java.util.List;

public class ViewPagerFragment extends Fragment {

    private static final String ARG_LATITUDE = "latitude";
    private static final String ARG_LONGITUDE = "longitude";

    private double latitude;
    private double longitude;

    private CameraFragment_ cameraFragment;
    private HomeFragment homeFragment;

    private SectionsPagerAdapter mSectionsPagerAdapter;

    private ViewPager mViewPager;

    private ViewPagerFragmentCallbacks mCallbacks;

    public static ViewPagerFragment newInstance(double latitude, double longitude) {
        ViewPagerFragment fragment = new ViewPagerFragment();

        Bundle args = new Bundle();
        args.putDouble(ARG_LATITUDE, latitude);
        args.putDouble(ARG_LONGITUDE, longitude);
        fragment.setArguments(args);

        return fragment;
    }

    public ViewPagerFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            latitude = getArguments().getDouble(ARG_LATITUDE);
            longitude = getArguments().getDouble(ARG_LONGITUDE);
        }

        cameraFragment = CameraFragment_.newInstance();
        homeFragment = HomeFragment.newInstance(latitude, longitude);

        setFragments();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        /*
        view pager가 root면 좋겠지만, tab 구조상 viewgroup에 wrapped되어야 하는듯 하다.
         */
        View view = inflater.inflate(R.layout.fragment_view_pager, container, false);

        mSectionsPagerAdapter = new SectionsPagerAdapter(getActivity().getFragmentManager());

        mViewPager = (ViewPager) view.findViewById(R.id.pager);

        mViewPager.setAdapter(mSectionsPagerAdapter);

        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                mOnPageSelected(position);
            }
        });

        // for init flag of activity.
        //mViewPager.setCurrentItem(0);
        //mOnPageSelected(0);
        setCurrentPage(0);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    public void mOnPageSelected(int position) {
        if (mCallbacks != null) {
            mCallbacks.onViewPagerPageSelected(position);
        }
    }

    public void setFragments() {
        if (mCallbacks != null) {
            mCallbacks.onSetFragments();
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mCallbacks = (ViewPagerFragmentCallbacks) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement TabContentCallbacks");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    public void setCurrentPage(int index) {
        mViewPager.setCurrentItem(index);
        mOnPageSelected(index);
    }

    public CameraFragment_ getCameraFragment() {
        return cameraFragment;
    }

    public HomeFragment getHomeFragment() {
        return homeFragment;
    }

    /*
     * 일단 tab content 중간중간에 사라지는 문제가 state로 바꾸면서 해결은 되었지만 원인 확실히 알아봐야 한다.
     * 어쨌든 메모리 생각해서 state object 쓰는게 맞다고 생각된다.
     */
    public class SectionsPagerAdapter extends FragmentStatePagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {

            Fragment fragment = null;

            switch(position) {
                case 0:
                    fragment = cameraFragment;

                    break;
                case 1:
                    fragment = homeFragment;

                    break;
            }

            return fragment;
        }

        @Override
        public int getCount() {
            return 2;
        }
    }

    public interface ViewPagerFragmentCallbacks {
        public void onViewPagerPageSelected(int position);
        public void onSetFragments();
    }
}