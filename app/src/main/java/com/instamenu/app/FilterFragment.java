package com.instamenu.app;

import android.app.Activity;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.instamenu.R;
import com.instamenu.widget.TagAdapter;

import java.util.ArrayList;
import java.util.List;

public class FilterFragment extends Fragment implements Button.OnClickListener {

    private static final String ARG_TAGS = "tags";
    private static final String ARG_SWITCHES = "switches";

    private DrawerLayout mDrawerLayout;
    private ViewGroup mFrame;
    private View mFragmentContainerView;

    private float lastTranslate = 0.0f;

    private List<String> tags;
    private List<String> switches;

    SharedPreferences preferences;

    ListView list;
    TagAdapter adapter;
    EditText edit;

    private FilterFragmentCallbacks mCallbacks;

    public static FilterFragment newInstance(List<String> tags, List<String> switches) {
        FilterFragment fragment = new FilterFragment();
        Bundle args = new Bundle();
        args.putStringArrayList(ARG_TAGS, tags != null ? (ArrayList<String>) tags : null);
        args.putStringArrayList(ARG_SWITCHES, switches != null ? (ArrayList<String>) switches : null);
        fragment.setArguments(args);
        return fragment;
    }
    public FilterFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            tags = getArguments().getStringArrayList(ARG_TAGS);
            switches = getArguments().getStringArrayList(ARG_SWITCHES);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_filter, container, false);

        list = (ListView) view.findViewById(R.id.fragment_filter_list);

        // setting adapter
        adapter = new TagAdapter(inflater, tags, switches);
        list.setAdapter(adapter);

        edit = (EditText) view.findViewById(R.id.fragment_filter_edit);

        ((Button) view.findViewById(R.id.fragment_filter_add)).setOnClickListener(this);

        return view;
    }

    public void setUp(int fragmentId, DrawerLayout drawerLayout, int frameId) {
        mFragmentContainerView = getActivity().findViewById(fragmentId);
        mDrawerLayout = drawerLayout;
        mFrame = (ViewGroup) getActivity().findViewById(frameId);

        mDrawerLayout.setDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View view, float v) {
                float moveFactor = mFragmentContainerView.getWidth() * v * -1;// rtl

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    mFrame.setTranslationX(moveFactor);
                } else {
                    TranslateAnimation anim = new TranslateAnimation(lastTranslate, moveFactor, 0.0f, 0.0f);
                    anim.setDuration(0);
                    anim.setFillAfter(true);
                    mFrame.startAnimation(anim);

                    lastTranslate = moveFactor;
                }
            }

            @Override
            public void onDrawerOpened(View view) {
                // 사실 fragment가 create, destroy되면 필요하겠지만 현재는 필요없다.
            }

            @Override
            public void onDrawerClosed(View view) {
                closeFilter();

                //setViewing(false);
            }

            @Override
            public void onDrawerStateChanged(int i) {

            }
        });

        // set a custom shadow that overlays the main content when the drawer opens
        //mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.END);
    }

    public boolean isDrawerOpen() {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(mFragmentContainerView);
    }

    public void openDrawer() {
        mDrawerLayout.openDrawer(mFragmentContainerView);
    }

    public void closeDrawer() {
        mDrawerLayout.closeDrawer(mFragmentContainerView);
    }

    public void toggleDrawer() {
        if(isDrawerOpen()) {
            closeDrawer();
        } else {
            openDrawer();
        }
    }

    public void setTags(List<String> tags, List<String> switches) {
        this.tags = tags;
        this.switches = switches;

        adapter.setTags(tags, switches);
    }

    public void setDrawerLocked(boolean locked) {
        if(locked) {
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        } else {
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallbacks = (FilterFragmentCallbacks) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    public void closeFilter() {
        if (mCallbacks != null) {
            mCallbacks.onCloseFilter(adapter.getTags(), adapter.getSwitches());
        }
    }

    public void hideKeyboard() {
        if (mCallbacks != null) {
            mCallbacks.onHideKeyboard(edit);
        }
    }

    // doesn't check duplication, only format.
    public boolean checkTag(String tag) {
        if(tag == null) return false;
        if(tag.trim().equals("")) return false;
        if(tag.trim().contains(" ")) return false;
        if(adapter.getTags() != null && adapter.getTags().contains(tag)) return false;

        return true;
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            /*
            case R.id.fragment_filter_ok:
                actionOKClicked();

                break;*/
            case R.id.fragment_filter_add:
                String tag = edit.getText().toString();
                if(checkTag(tag) == true) {
                    adapter.addTag(tag.trim());

                    hideKeyboard();
                } else {
                    Toast.makeText(getActivity(), "Type right tag format", Toast.LENGTH_SHORT).show();
                }

                break;
        }
    }

    public interface FilterFragmentCallbacks {
        public void onCloseFilter(List<String> tags, List<String> switches);
        public void onHideKeyboard(View view);
    }
}