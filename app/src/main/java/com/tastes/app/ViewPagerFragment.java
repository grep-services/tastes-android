package com.tastes.app;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tastes.R;
import com.tastes.widget.ViewPager_;

import java.util.ArrayList;
import java.util.List;

public class ViewPagerFragment extends Fragment {

    private CameraFragment_ cameraFragment;
    //private GalleryFragment galleryFragment;
    private HomeFragment homeFragment;
    //private FilterFragment filterFragment;

    private SectionsPagerAdapter mSectionsPagerAdapter;

    private ViewPager_ mViewPager;

    private ViewPagerFragmentCallbacks mCallbacks;

    public static ViewPagerFragment newInstance() {
        ViewPagerFragment fragment = new ViewPagerFragment();

        return fragment;
    }

    public ViewPagerFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        cameraFragment = ((MainActivity) getActivity()).getCameraFragment();
        //galleryFragment = ((MainActivity) getActivity()).getGalleryFragment();
        homeFragment = ((MainActivity) getActivity()).getHomeFragment();
        //filterFragment = ((MainActivity) getActivity()).getFilterFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        /*
        view pager가 root면 좋겠지만, tab 구조상 viewgroup에 wrapped되어야 하는듯 하다.
         */
        View view = inflater.inflate(R.layout.fragment_view_pager, container, false);

        mSectionsPagerAdapter = new SectionsPagerAdapter(getActivity().getFragmentManager());

        mViewPager = (ViewPager_) view.findViewById(R.id.pager);

        mViewPager.setAdapter(mSectionsPagerAdapter);

        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                mOnPageSelected(position);
            }
        });

        mViewPager.setOffscreenPageLimit(2);

        // for init flag of activity.
        //mViewPager.setCurrentItem(0);
        //mOnPageSelected(0);
        setCurrentPage(0);// camera

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

    public void setEnabled(boolean enabled) {
        mViewPager.setPagingEnabled(enabled);
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
                /*case 0:
                    fragment = galleryFragment;

                    break;*/
                case 0:
                    fragment = cameraFragment;

                    break;
                case 1:
                    fragment = homeFragment;

                    break;
                /*case 2:
                    fragment = filterFragment;*/
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
    }
}