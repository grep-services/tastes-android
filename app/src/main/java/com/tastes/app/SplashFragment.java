package com.tastes.app;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.tastes.R;
import com.tastes.util.LocationUtils;

public class SplashFragment extends Fragment implements Button.OnClickListener {
    private static final String ARG_LOCATION = "location";

    // layout
    View layout_location_agree;//, layout_location_retry;

    // network, location
    private boolean mLocationUpdates = false;// 사실상 동의서다.(pref에 저장된 동의 여부이므로)

    private SplashFragmentCallbakcs mCallbacks;

    public static SplashFragment newInstance(boolean location) {
        SplashFragment fragment = new SplashFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_LOCATION, location);
        fragment.setArguments(args);
        return fragment;
    }
    public SplashFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // get from pref(정확히는 pref(main)->args(frag)->get here.
        if (getArguments() != null) {
            mLocationUpdates = getArguments().getBoolean(ARG_LOCATION);
            //mLocationUpdates = false;
        }
    }

    // 편의를 위해 이렇게 만들고 사용한다.
    public void switchView(View view) {
        // hide all the views.
        layout_location_agree.setVisibility(View.GONE);
        //layout_location_retry.setVisibility(View.GONE);

        // null일 때도 있다.(모두 hide시키고 싶을 때)
        if(view != null) {
            view.setVisibility(View.VISIBLE);
        }
    }

    public void updateUI() {
        if(mLocationUpdates == false) {// 동의 UI
            switchView(layout_location_agree);
        } else {
            switchView(null);

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    onFinished();
                }
            }, 2000);// remove 안해줘도 되는지.
        }
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.fragment_splash_location_agree_btn:
                // set var and save it to pref.
                mLocationUpdates = true;// callback으로 날려야 한다.
                onLocationAgree();
                // update ui
                updateUI();

                break;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_splash, container, false);

        // init layout
        layout_location_agree = view.findViewById(R.id.fragment_splash_location_agree);
        //layout_location_retry = view.findViewById(R.id.fragment_splash_location_retry);

        ((Button) view.findViewById(R.id.fragment_splash_location_agree_btn)).setOnClickListener(this);
        //((Button) view.findViewById(R.id.fragment_splash_location_retry_btn)).setOnClickListener(this);

        updateUI();

        return view;
    }

    public void onFinished() {
        if (mCallbacks != null) {
            mCallbacks.onSplashFinished();
        }
    }

    public void onLocationAgree() {
        if (mCallbacks != null) {
            mCallbacks.onSplashLocationAgreed();
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallbacks = (SplashFragmentCallbakcs) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    public interface SplashFragmentCallbakcs {
        public void onSplashFinished();
        public void onSplashLocationAgreed();
    }
}