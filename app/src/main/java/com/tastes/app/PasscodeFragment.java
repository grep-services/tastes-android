package com.tastes.app;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.tastes.R;

public class PasscodeFragment extends Fragment implements Button.OnClickListener {
    private static final String ARG_INITIAL = "initial";// FOR DISPLAY'S FIRST
    private static final String ARG_PASSCODE = "passcode";// FOR ITEM'S MORE

    private boolean initial;
    private String passcode;

    private PasscodeFragmentCallbakcs mCallbacks;

    public static PasscodeFragment newInstance(boolean initial, String passcode) {
        PasscodeFragment fragment = new PasscodeFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_INITIAL, initial);
        args.putString(ARG_PASSCODE, passcode);
        fragment.setArguments(args);
        return fragment;
    }
    public PasscodeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            initial = getArguments().getBoolean(ARG_INITIAL);
            passcode = getArguments().getString(ARG_PASSCODE);
        }
    }

    @Override
    public void onClick(View v) {
        /*switch(v.getId()) {
            case R.id.fragment_splash_location_agree_btn:
                // set var and save it to pref.
                mLocationUpdates = true;// callback으로 날려야 한다.
                onLocationAgree();
                // update ui
                updateUI();

                break;
        }*/
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_passcode, container, false);

        return view;
    }
/*
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
*/
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallbacks = (PasscodeFragmentCallbakcs) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    public interface PasscodeFragmentCallbakcs {
        //public void onSplashFinished();
        //public void onSplashLocationAgreed();
    }
}