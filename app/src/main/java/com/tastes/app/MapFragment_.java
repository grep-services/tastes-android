package com.tastes.app;

import android.app.Activity;
import android.location.Location;
import android.os.Bundle;
import android.app.Fragment;
import android.view.Gravity;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.tastes.R;

/**
 * address를 사용하기는 하지만 외부로부터 받지도 않고 전달하지도 않는다.
 * 여기서 사용하는 address는 여기서 사용하고, 외부(home 등)에서 사용하는건 거기서 알아서 사용한다.
 * 그렇게 하면 간혹 연결 도중 network 문제 등으로 address 못 받을 수 있겠지만
 * 전달을 위해 넘기다 보면 구조가 복잡해질 수 있다.
 *
 * -> 그런데 그 구조 복잡해지는 것 보다 address 받는 속도 문제가 더 클 수 있다고 본다.(당장 map에서 back후 home을 봐도 그럴 것이다.)
 * 따라서 address도 local에 있더라도 main에서 global하게 관리되는 느낌으로 간다.(각 frag에서도 최대한 주고받을 수 있으면 주고받는다.)
 *
 * 또한, 애초에 main->home->map 등으로 coords가 전달되는 등 그 updated flag의 sync가 잘 안맞을 수 있다는 생각에 arg로 둘까 했으나
 * 일단 그냥 가본다.
 *
 * 그리고, MapFragment를 상속받지 않으면 결국 nested구조로 가거나 view recycle문제 생기는 방식으로 가야 되어서
 * 상속받는 구조로 가고 onCreateView에서 좀 제어해주도록 했다.
 */
public class MapFragment_ extends MapFragment implements OnMapReadyCallback, View.OnClickListener {
    private static final String ARG_LATITUDE = "latitude";
    private static final String ARG_LONGITUDE = "longitude";
    private static final String ARG_AVAILABLE = "available";

    private double latitude;
    private double longitude;

    private boolean isLocationAvailable;// main updated 안됐어도 map manually set 된 것 넘어올 수 있다.

    private String address;// 이건 받는 arg는 아닐 것이므로 이렇게 따로 둔다.

    private GoogleMap map;
    private Marker current;

    private Button button, btn;

    private MainActivity mActivity;
    private MapFragmentCallbacks mCallbacks;

    public static MapFragment_ newInstance(double latitude, double longitude, boolean isLocationAvailable) {
        MapFragment_ fragment = new MapFragment_();
        Bundle args = new Bundle();
        args.putDouble(ARG_LATITUDE, latitude);
        args.putDouble(ARG_LONGITUDE, longitude);
        args.putBoolean(ARG_AVAILABLE, isLocationAvailable);
        fragment.setArguments(args);
        return fragment;
    }
    public MapFragment_() {
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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_map_, container, false);

        View mapView = super.onCreateView(inflater, container, savedInstanceState);

        root.addView(mapView, 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        button = (Button) root.findViewById(R.id.fragment_map_location);
        button.setOnClickListener(this);

        btn = (Button) root.findViewById(R.id.fragment_map_go);
        btn.setOnClickListener(this);
        btn.setEnabled(false);

        getMapAsync(this);

        return root;
    }

    public void setLocation(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;

        button.setEnabled(true);
        // current location만의 marker 선택 효과는 없애기로 했다.
        setMarker(latitude, longitude);
    }

    public void notifyLocationFailure() {
        button.setEnabled(true);

        Toast toast = Toast.makeText(mActivity, getString(R.string.location_retry), Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;

        googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                setMarker(latLng.latitude, latLng.longitude);
            }
        });

        // 생각해보면 setLocation과 여기서 setMarker가 겹칠 일이(물론 겹친다고 문제 될 것도 없긴 하지만) 논리적으로는 없으며 현실적으로는 아주 작은 확률로 존재할 수 있다.
        //if(mActivity.isLocationUpdated()) {// 들어온 coords가 available한 값이면
        if(isLocationAvailable) {// location off 상태의 home에서 set된 loc 올 수도 있다.
            setMarker(latitude, longitude);
        }
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.fragment_map_location:
                /*
                TODO: 읽어보기.
                새로운 getMyLocation은 없었다. 오히려 deprecated된 것이었다. 기존 method 쓰면 된다.
                그리고, 이제는 현위치를 받았어도, 다시 받아올 수 있다는 개념을 도입한다.
                즉, location updated되어 있는지 check하지 않는다.
                또한, wait message도 필요없게 만든다.(button disable 및 progress로의 대체)
                */
                button.setEnabled(false);
                //TODO: progress 추가하기.
                mActivity.startLocationUpdates();

                break;
            case R.id.fragment_map_go:
                LatLng location = current.getPosition();

                locationClicked(location.latitude, location.longitude);

                break;
        }
    }

    public void setMarker(double latitude, double longitude) {
        if(current == null) {
            btn.setEnabled(true);// init 필요 at least once

            current = map.addMarker(new MarkerOptions()
                    .position(new LatLng(latitude, longitude))
                    .draggable(true));// title은 나중에 추가한다 치고, draggable 넣어두면 편할듯. 하지만 알아차리기 힘들듯.
        } else {
            current.setPosition(new LatLng(latitude, longitude));
        }

        animateCamera(latitude, longitude);
    }

    public void animateCamera(double latitude, double longitude) {
        CameraPosition position = new CameraPosition.Builder().target(new LatLng(latitude, longitude)).zoom(17f).build();

        //map.moveCamera(CameraUpdateFactory.newCameraPosition(position));
        map.animateCamera(CameraUpdateFactory.newCameraPosition(position));
    }

    public void locationClicked(double latitude, double longitude) {
        if (mCallbacks != null) {
            mCallbacks.onMapLocationClicked(latitude, longitude);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mActivity = (MainActivity) activity;

        try {
            mCallbacks = (MapFragmentCallbacks) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    public interface MapFragmentCallbacks {
        public void onMapLocationClicked(double latitude, double longitude);
    }
}
