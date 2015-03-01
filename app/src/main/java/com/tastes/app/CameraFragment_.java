package com.tastes.app;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.commonsware.cwac.camera.CameraFragment;
import com.commonsware.cwac.camera.CameraView;
import com.commonsware.cwac.camera.PictureTransaction;
import com.commonsware.cwac.camera.SimpleCameraHost;
import com.tastes.R;

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

    private CameraView cameraView;
    private View layerView;
    private View tools;// for splash

    private boolean use_ffc = false;// splash ffc로 하고 main에서 flip하려니 타이밍 에러 가능성 있다. 버벅거리기도 하므로 이렇게 간다.
    private boolean mirror_ffc = true;
    // 일단, continuous가 대세다. 이걸 더 발전시킨다. take전에 autofocus는 답답함 늘릴 수 있다. 차라리 tap to autofocus나 추가한다.
    private boolean use_autofocus = false;
    private String mode_focus;// = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE;// 이건 거의 바뀔 일 없을듯.
    private boolean use_singleshot = true;
    private boolean use_fullbleed = true;
    private String mode_flash = Camera.Parameters.FLASH_MODE_OFF;

    private boolean isAutoFocusing;

    private CameraFragmentCallbacks mCallbacks = null;// callback. 변경 ui는 fragment에 있고, 변경 대상 vars는 activity에 있다. activity가 implement하게 하고, 그 method를 call한다.
    // 근데 이거 그런방법은 없는가? callback 그대로 쓰는 등...
    private MainActivity mActivity = null;// 쓰일 곳이 한군데이긴 하지만, attatch에 붙여서 사용하는게 더 정리되어 보인다.
    private Button shotButton, homeButton, flipButton;
    private CheckBox flashCheck;

    private ImageView focus;

    private GestureDetector gestureDetector;

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

        gestureDetector = new GestureDetector(getActivity(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) { // double tap과 tap 구별 가능한 method.
                autoFocus(e.getX(), e.getY());

                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                flip();

                return true;
            }
        });
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

        layerView = view.findViewById(R.id.fragment_camera_layer);
        tools = view.findViewById(R.id.fragment_camera_toolbars);

        if(mActivity.isSplashViewing()) setToolsVisible(false);// 해제는 splash에서 한다.

        //setCameraView(cameraView);

        shotButton = (Button) view.findViewById(R.id.fragment_camera_shot);
        homeButton = (Button) view.findViewById(R.id.fragment_camera_home);
        flipButton = (Button) view.findViewById(R.id.fragment_camera_flip);
        flashCheck = (CheckBox) view.findViewById(R.id.fragment_camera_flash);

        //view.setOnTouchListener(this);
        layerView.setOnTouchListener(this);

        //shotButton.setOnTouchListener(this);
        //homeButton.setOnTouchListener(this);
        flashCheck.setOnTouchListener(this);

        shotButton.setOnClickListener(this);
        flipButton.setOnClickListener(this);

        flashCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mode_flash = isChecked ? Camera.Parameters.FLASH_MODE_ON : Camera.Parameters.FLASH_MODE_OFF;
            }
        });

        focus = (ImageView) view.findViewById(R.id.fragment_camera_focus);

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
            mCallbacks.onSaveImage(use_ffc, image);
        }
    }

    public void setShotButtonEnabled(boolean enabled) {
        shotButton.setEnabled(enabled);

        // false일 때가 티가 나도 되지만, 기능만 disable되어도 상관없다. 순식간인데 괜히 button back 바뀌면 혼란스럽기만 할 수도 있다.
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if(v instanceof ViewGroup) {
            gestureDetector.onTouchEvent(event);

            return true;
        } else { // 나중에 버튼 등.
            switch(event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    //YoYo.with(Techniques.Pulse)/*.duration(1000)*/.playOn(v);

                    break;
                case MotionEvent.ACTION_UP:
            }
        }

        return false;// 필요한 곳에서만 consume하면 된다.(그래야 not consume해서 checkbox 등도 눌리고 한다.)
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.fragment_camera_shot:
                if(use_ffc == true) {
                    // 이렇게 기존 변수는 변경시키지 말아야 다시 flip했을 때 그대로 사용하게 할 수 있다.
                    takePicture(Camera.Parameters.FLASH_MODE_OFF);
                } else {
                    takePicture(mode_flash);
                }

                break;
            case R.id.fragment_camera_flip:
                flip();

                break;
        }
    }

    public void autoFocus(float x, float y) {
        if(use_autofocus == true) { // host에서 알아서 바뀌는데, 이것부터 check해야 한다. 그래야 mode도 정해진다.
            if(use_ffc == false) { // ffc를 쓸 때는 하면 kill된다. 어쨌든 이렇게 처리하면 된다.
                if(isAutoFocusing == false) {
                    // 이걸 빨리 해놔야 재터치 안된다.
                    isAutoFocusing = true;
                    // shot할 때 focusing check 할거 까지는 없고 여기서 이렇게 해준다.
                    setShotButtonEnabled(false);
                    // 준비 됐으면 area 띄워준다.
                    showFocusArea(x, y, true);
                    // v size check해보고, xy는 그냥 저렇게 하면 되고, focus size는 dp가 아니라 camera axis(fixed)에 맞춰져야 하므로 고정된 100 정도로 한다.
                    cameraView.autoFocus(layerView.getWidth(), layerView.getHeight(), x, y, 50);
                }
            }
        }
    }

    public void flip() {
        use_ffc = !use_ffc;

        flashCheck.setVisibility(use_ffc ? View.GONE : View.VISIBLE);

        if(cameraView != null) {
            cameraView.onPause();
        }

        View view = getView();
        ((ViewGroup) view).removeView(cameraView);
        cameraView = new CameraView(getActivity());// author 말을 봐도 view나 frag 새로 생성해야 한다고 한다.
        cameraView.setHost(new CameraHost_(getActivity()));
        setCameraView(cameraView);// init때는 어차피 super에서 받아오므로 안 해도 된다고 볼 수 있다.(demo에도 그렇다.)
        ((ViewGroup) view).addView(cameraView, 0);

        cameraView.onResume();
    }

    public void showFocusArea(float x, float y, boolean show) {
        if(show) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {// xy set으로 해본다.(화면 넘어가도 괜찮도록)
                focus.setX(x - focus.getWidth() / 2);
                focus.setY(y - focus.getHeight() / 2);
            } else {// 그냥 autofocus이므로 가운데 정렬이면 된다.
                FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) focus.getLayoutParams();
                layoutParams.gravity = Gravity.CENTER;
                focus.setLayoutParams(layoutParams);
            }

            focus.setVisibility(View.VISIBLE);

            // 그리고 animation -> 일단 필요없을듯.
        } else {
            focus.setVisibility(View.INVISIBLE);

            // 그리고 animation -> 일단 필요없을듯.
        }
    }

    class CameraHost_ extends SimpleCameraHost {
        public CameraHost_(Context context) {
            super(context);
        }

        @Override
        public boolean useFrontFacingCamera() {
            return use_ffc;
        }

        @Override
        public boolean mirrorFFC() {
            return mirror_ffc;
        }

        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            super.onAutoFocus(success, camera);

            // 일단 빠른 시점에 없애본다.(늦게 없어지면 답답해 보일 것 같아서)
            showFocusArea(-1, -1, false);

            // 내 생각에는 success든 fail이든 할 처리들은 다 해야 된다.
            camera.cancelAutoFocus();

            // 다시 mode_focus로 복귀시킨다.
            Camera.Parameters parameters = camera.getParameters();
            // api 보면 continuous 같은 경우 resume시 재설정 해야 된다 하고, 다른 것들도 확실히 잘 모르므로, 괜히 중복 체크 하지 말고 reset해준다.
            if(parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                mode_focus = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE;
            } else { // focus callback이라는거 자체가 최소 auto는 된다는 것이다.
                mode_focus = Camera.Parameters.FOCUS_MODE_AUTO;
            }

            if(parameters.getFocusMode() != mode_focus) {// 4.0이하에서는 그냥 autofocus하게 되고 그럼 cont 인채로 돌아올 수도 있다.
                parameters.setFocusMode(mode_focus);
                camera.setParameters(parameters);
            }

            // 다시 touchable.
            isAutoFocusing = false;

            // shot enabled.
            setShotButtonEnabled(true);
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
        /* 여기는 일단 picture 이후에 되는거 같진 않지만 picture과 비율이 같아야 되는 등의 제약은 없어 보이는 것 같아서 빼뒀다.(default는 제일 가까운 비율인듯)
        @Override
        public Camera.Size getPreviewSize(int displayOrientation, int width, int height, Camera.Parameters parameters) {
            Camera.Size op = CameraUtils.getOptimalPreviewSize(displayOrientation, width, height, parameters);
            Camera.Size be = CameraUtils.getBestAspectPreviewSize(displayOrientation, width, height, parameters);

            //return super.getPreviewSize(displayOrientation, width, height, parameters);
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
            //Camera.Size max = CameraUtils.getLargestPictureSize(this, parameters);
            //Camera.Size min = CameraUtils.getSmallestPictureSize(parameters);
            //return super.getPictureSize(xact, parameters);
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

            // 혹시라도 불켜놓는거 필요하면 여기서 찾아써보든가 한다. 안되면 light라도...
            //mode_flash = CameraUtils.findBestFlashModeMatch(parameters,
            //        Camera.Parameters.FLASH_MODE_RED_EYE,
            //        Camera.Parameters.FLASH_MODE_AUTO,
            //        Camera.Parameters.FLASH_MODE_ON);

            if(use_autofocus) { // 일단 available해야 한다.
                if(use_ffc == false) { // 이게 ffc에서는 안되는 것 같다.(focus 자체가 안되는 걸지도) => area focus 해보면 그쪽에서는 touch하면 아예 kill된다.
                    if(parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                        mode_focus = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE;
                    } else { // 마찬가지로 focus available하면 최소 auto는 있다.
                        mode_focus = Camera.Parameters.FOCUS_MODE_AUTO;
                    }

                    parameters.setFocusMode(mode_focus);
                }
            }

            return super.adjustPreviewParameters(parameters);
        }
    }
    /*
    사실 현재는 button의 listener 자체가 activity에 구현되어 있기 때문에 이런 callback 방식이 필요없으나
    구조상 이런게 쓰일 곳이 있을거고 activity reference를 받아두기 위해서(host set할 때) attatch당시를 catch하려 했던 용도도 있다.
     */
    public interface CameraFragmentCallbacks {
        public void onTakePicture(String mode_flash);
        public void onSaveImage(boolean mirror, byte[] image);
    }
}