package com.tastes.app;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;

import com.tastes.R;

public class PasscodeFragment extends Fragment implements Button.OnClickListener {
    private static final String ARG_INITIAL = "initial";// FOR DISPLAY'S FIRST
    private static final String ARG_PASSCODE = "passcode";// FOR ITEM'S MORE
    private static final String ARG_ID = "id";// FOR ITEM'S MORE - IMAGE ID
    private static final String ARG_LATITUDE = "latitude";
    private static final String ARG_LONGITUDE = "longitude";

    private boolean initial;
    private String passcode;
    private int id;
    private double latitude;
    private double longitude;

    private String input = "";

    View line;
    View[] circle;
    Button button;// cancel/delete

    private PasscodeFragmentCallbakcs mCallbacks;

    public static PasscodeFragment newInstance(boolean initial, String passcode, int id, double latitude, double longitude) {
        PasscodeFragment fragment = new PasscodeFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_INITIAL, initial);
        args.putString(ARG_PASSCODE, passcode);
        args.putInt(ARG_ID, id);
        args.putDouble(ARG_LATITUDE, latitude);
        args.putDouble(ARG_LONGITUDE, longitude);
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
            id = getArguments().getInt(ARG_ID);
            latitude = getArguments().getDouble(ARG_LATITUDE);
            longitude = getArguments().getDouble(ARG_LONGITUDE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_passcode, container, false);

        line = view.findViewById(R.id.fragment_passcode_line);

        circle = new View[4];
        circle[0] = view.findViewById(R.id.fragment_passcode_circle_1);
        circle[1] = view.findViewById(R.id.fragment_passcode_circle_2);
        circle[2] = view.findViewById(R.id.fragment_passcode_circle_3);
        circle[3] = view.findViewById(R.id.fragment_passcode_circle_4);

        view.findViewById(R.id.fragment_passcode_number_1).setOnClickListener(this);
        view.findViewById(R.id.fragment_passcode_number_2).setOnClickListener(this);
        view.findViewById(R.id.fragment_passcode_number_3).setOnClickListener(this);
        view.findViewById(R.id.fragment_passcode_number_4).setOnClickListener(this);
        view.findViewById(R.id.fragment_passcode_number_5).setOnClickListener(this);
        view.findViewById(R.id.fragment_passcode_number_6).setOnClickListener(this);
        view.findViewById(R.id.fragment_passcode_number_7).setOnClickListener(this);
        view.findViewById(R.id.fragment_passcode_number_8).setOnClickListener(this);
        view.findViewById(R.id.fragment_passcode_number_9).setOnClickListener(this);
        view.findViewById(R.id.fragment_passcode_number_0).setOnClickListener(this);

        button = (Button) view.findViewById(R.id.fragment_passcode_button);
        button.setOnClickListener(this);

        return view;
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.fragment_passcode_button:
                clickButton();

                break;
            default:// numbers
                clickNumber((String) v.getTag());

                break;
        }
    }

    public void clickButton() {
        if(input.isEmpty()) {
            ((MainActivity) getActivity()).onBackPressed();
        } else {
            circle[input.length() - 1].setEnabled(true);
            input = input.substring(0, input.length() - 1);// remove last char.
            if(input.isEmpty()) {
                button.setText(getString(R.string.passcode_cancel));
            }
        }
    }

    public void clickNumber(String number) {
        if(input.isEmpty()) {
            button.setText(getString(R.string.passcode_delete));
        }
        input += number;
        circle[input.length() - 1].setEnabled(false);

        if(input.length() == 4) {
            if(initial) {// FROM DISPLAY
                onPasscodeInit(input, latitude, longitude);
            } else {// FROM ITEM
                if(input.equals(passcode)) {// TODO: passcode can be null yet
                    onPasscodeConfirmed(id);
                } else {
                    // shake
                    Animation shake = AnimationUtils.loadAnimation(getActivity(), R.anim.shake);
                    line.startAnimation(shake);
                    // reset
                    button.setText(getString(R.string.passcode_cancel));
                    input = "";
                    for(View view : circle) {
                        view.setEnabled(true);
                    }
                }
            }
        }
    }

    public void onPasscodeInit(String passcode, double latitude, double longitude) {
        if (mCallbacks != null) {
            mCallbacks.onPasscodeInit(passcode, latitude, longitude);
        }
    }

    public void onPasscodeConfirmed(int id) {
        if (mCallbacks != null) {
            mCallbacks.onPasscodeConfirmed(id);
        }
    }

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
        public void onPasscodeInit(String passcode, double latitude, double longitude);
        public void onPasscodeConfirmed(int id);
    }
}