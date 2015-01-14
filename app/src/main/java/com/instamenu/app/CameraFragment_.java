package com.instamenu.app;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.commonsware.cwac.camera.CameraFragment;
import com.commonsware.cwac.camera.CameraHost;
import com.commonsware.cwac.camera.CameraHostProvider;
import com.commonsware.cwac.camera.CameraUtils;
import com.commonsware.cwac.camera.CameraView;
import com.commonsware.cwac.camera.PictureTransaction;
import com.commonsware.cwac.camera.SimpleCameraHost;
import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.instamenu.R;

import java.util.List;

/*
당장 필요한건, 찍을 때 autofocus 되고, flash나 ffc는 나중이면 된다.
추가로, singleshot이고, fullbleed(아마 비율 조정하는것인듯)이며, zoom은 나중에 고려해본다.

autofocus : true로 가지만, take picture과 연결되어야 한다. ==> true
singleshot ==> true
fullbleed : 뭔지 잘은 모르겠지만 ==> true
ffc : 일단 camera number는 activiy에서 세어서 넘겨줄텐데, 당장은 1개로 간다. ==> false
flash : 나중에. ==> false.
zoom ==> 일단 배제. 이건 boolean으로 다룰 만한 것도 아니다.

toggle되는 설정들 없으므로 아직 host customize하진 않고 simple에서의 builder 설정으로 바로 가도록 한다.
*/
public class CameraFragment_ extends CameraFragment implements View.OnTouchListener, Button.OnClickListener {

    private View tools;// for splash

    private CameraView cameraView;

    private boolean use_ffc = false;
    // 일단, continuous가 대세다. 이걸 더 발전시킨다. take전에 autofocus는 답답함 늘릴 수 있다. 차라리 tap to autofocus나 추가한다.
    private boolean use_autofocus = false;
    private String mode_focus = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE;// 이건 거의 바뀔 일 없을듯.
    private boolean use_singleshot = true;
    private boolean use_fullbleed = true;
    private String mode_flash = null;

    private CameraFragmentCallbacks mCallbacks = null;// callback. 변경 ui는 fragment에 있고, 변경 대상 vars는 activity에 있다. activity가 implement하게 하고, 그 method를 call한다.
    // 근데 이거 그런방법은 없는가? callback 그대로 쓰는 등...
    private MainActivity mActivity = null;// 쓰일 곳이 한군데이긴 하지만, attatch에 붙여서 사용하는게 더 정리되어 보인다.
    private Button shotButton, homeButton;
    private CheckBox flashCheck;

    public static CameraFragment_ newInstance() {
        CameraFragment_ fragment = new CameraFragment_();

        return fragment;
    }

    public CameraFragment_() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHost(new CameraHost_(getActivity()));
    }

    public void setToolsVisible(boolean visible) {
        if(tools != null) {
            tools.setVisibility(visible == true ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_camera_, container, false);

        cameraView = (CameraView) super.onCreateView(inflater, container, savedInstanceState);
        ((ViewGroup) view).addView(cameraView, 0);

        tools = view.findViewById(R.id.fragment_camera_toolbars);

        if(mActivity.isSplashViewing()) setToolsVisible(false);// 해제는 splash에서 한다.

        //setCameraView(cameraView);

        shotButton = (Button) view.findViewById(R.id.fragment_camera_shot);
        homeButton = (Button) view.findViewById(R.id.fragment_camera_home);
        flashCheck = (CheckBox) view.findViewById(R.id.fragment_camera_flash);

        //shotButton.setOnTouchListener(this);
        //homeButton.setOnTouchListener(this);
        flashCheck.setOnTouchListener(this);

        shotButton.setOnClickListener(this);

        flashCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mode_flash = isChecked ? Camera.Parameters.FLASH_MODE_ON : Camera.Parameters.FLASH_MODE_OFF;
            }
        });

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mActivity = (MainActivity) activity;

        try {
            mCallbacks = (CameraFragmentCallbacks) activity; // 이거 사용할 때는(button에 의해서든 뭐든, null check부터.)
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement CameraConfigurationListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        mCallbacks = null;
    }

    public void takePicture(String mode_flash) {
        if (mCallbacks != null) {
            mCallbacks.onTakePicture(mode_flash);
        }
    }

    public void saveImage_(byte[] image) {
        if (mCallbacks != null) {
            mCallbacks.onSaveImage(image);
        }
    }

    public void setShotButtonEnabled(boolean enabled) {
        shotButton.setEnabled(enabled);

        // false일 때가 티가 나도 되지만, 기능만 disable되어도 상관없다. 순식간인데 괜히 button back 바뀌면 혼란스럽기만 할 수도 있다.
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch(event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                YoYo.with(Techniques.Pulse)/*.duration(1000)*/.playOn(v);

                break;
            case MotionEvent.ACTION_UP:
                break;
        }

        return false;
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.fragment_camera_shot:
                takePicture(mode_flash);

                break;
        }
    }

    class CameraHost_ extends SimpleCameraHost {
        public CameraHost_(Context context) {
            super(context);
        }

        @Override
        public boolean useFrontFacingCamera() {
            return use_ffc;// 나중엔 toggle 될수도.
        }

        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            super.onAutoFocus(success, camera);

            // 나중에 쓸 일이 있을듯. autofocus square 만들 때 등.
        }

        @Override
        public void autoFocusAvailable() {
            super.autoFocusAvailable();

            use_autofocus = true;
        }

        @Override
        public void autoFocusUnavailable() {
            super.autoFocusUnavailable();

            use_autofocus = false;
        }

        @Override
        public boolean useSingleShotMode() {
            return use_singleshot;// default false.
        }

        @Override
        public RecordingHint getRecordingHint() {
            return RecordingHint.STILL_ONLY;
            //return super.getRecordingHint();
        }
/*
        @Override
        public Camera.Size getPreviewSize(int displayOrientation, int width, int height, Camera.Parameters parameters) {
            int a=3;
            Camera.Size op = CameraUtils.getOptimalPreviewSize(displayOrientation, width, height, parameters);
            Camera.Size be = CameraUtils.getBestAspectPreviewSize(displayOrientation, width, height, parameters);

            //return super.getPreviewSize(displayOrientation, width, height, parameters);
            //return op;

            List<Camera.Size> list = parameters.getSupportedPreviewSizes();

            int diff = 0;
            Camera.Size result = null;

            for(Camera.Size size : list) {
                if(result == null) {
                    diff = Math.abs(size.width - 720);
                    result = size;
                } else {
                    if(Math.abs(size.width - 720) < diff) {
                        diff = Math.abs(size.width - 720);
                        result = size;
                    }
                }
            }

            return result;
        }
*/
        @Override
        public Camera.Size getPictureSize(PictureTransaction xact, Camera.Parameters parameters) {
            List<Camera.Size> list = parameters.getSupportedPictureSizes();

            int diff = 0;
            Camera.Size result = null;

            for(Camera.Size size : list) {
                int width = Math.min(size.width, size.height);// 기본적으로 land로 하는거 같지만(그러면 height가 min이다.) 확실하게 하기 위해 이렇게 해준다.

                if(result == null) {
                    diff = Math.abs(width - 720);
                    result = size;
                } else {
                    if(Math.abs(width - 720) < diff) {
                        diff = Math.abs(width - 720);
                        result = size;
                    }
                }
            }
            /*
            int index = list.size() / 2;
            Camera.Size max = CameraUtils.getLargestPictureSize(this, parameters);
            Camera.Size min = CameraUtils.getSmallestPictureSize(parameters);
            //return min;
            return list.get(list.size() * 2 / 3);
            //return super.getPictureSize(xact, parameters);
            */
            //return list.get(list.size() / 2);
            return result;
        }

        @Override
        public void saveImage(PictureTransaction pictureTransaction, byte[] image) {
            saveImage_(image);
        }

        @Override
        public void onCameraFail(FailureReason reason) {
            super.onCameraFail(reason);

            Toast.makeText(getActivity(), "Cannot use the camera", Toast.LENGTH_SHORT).show();
        }

        @Override
        public boolean useFullBleedPreview() {
            return use_fullbleed;// test는 해보기. 되는지.
        }

        @Override
        public Camera.Parameters adjustPreviewParameters(Camera.Parameters parameters) {
            // deprecated되었다고 나오는데, 이건 camera2 쓰라는 말이다(v21),
            // 직접 정해줄수도 있지만(위에서) 나중에는 이렇게 가야 될 것 같아서 여기서 해준다.

            //mode_flash = CameraUtils.findBestFlashModeMatch(parameters,
            //        Camera.Parameters.FLASH_MODE_RED_EYE,
            //        Camera.Parameters.FLASH_MODE_AUTO,
            //        Camera.Parameters.FLASH_MODE_ON);

            // 나중에 flash 기능 사용할 때면, 여기 때문에 항상 refresh될건데 fragment의 flash ui 제대로 refresh되는지 확인한다.
            mode_flash = CameraUtils.findBestFlashModeMatch(parameters, Camera.Parameters.FLASH_MODE_OFF);

            parameters.setFocusMode(mode_focus);

            return super.adjustPreviewParameters(parameters);
        }
    }

    /*
    사실 현재는 button의 listener 자체가 activity에 구현되어 있기 때문에 이런 callback 방식이 필요없으나
    구조상 이런게 쓰일 곳이 있을거고 activity reference를 받아두기 위해서(host set할 때) attatch당시를 catch하려 했던 용도도 있다.
     */
    public interface CameraFragmentCallbacks {
        public void onTakePicture(String mode_flash);
        public void onSaveImage(byte[] image);
    }
}