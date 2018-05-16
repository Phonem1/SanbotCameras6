package com.anewtech.phone.sanbotcameras6;

import android.content.Intent;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import android.view.WindowManager;
import android.widget.RadioButton;
import android.widget.Toast;

import com.qihancloud.opensdk.base.BindBaseActivity;
import com.qihancloud.opensdk.beans.FuncConstant;
import com.qihancloud.opensdk.function.beans.headmotion.RelativeAngleHeadMotion;
import com.qihancloud.opensdk.function.unit.HardWareManager;
import com.qihancloud.opensdk.function.unit.HeadMotionManager;

import java.io.IOException;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class BodyCamActivity extends BindBaseActivity implements SurfaceHolder.Callback{
    private final static String TAG = BodyCamActivity.class.getSimpleName();
    @Bind(R.id.sfv_video)
    SurfaceView surfaceView;

    //Radio Buttons
    @Bind(R.id.faceCam)
    RadioButton fcrButton;
    @Bind(R.id.bodyCam)
    RadioButton bcrButton;
    @Bind(R.id.faceDetect)
    RadioButton fdrButton;

    CountDownTimer timer;
    HardWareManager hardWareManager;
    HeadMotionManager headMotionManager;
    RelativeAngleHeadMotion relativeAngleHeadMotionUp = new RelativeAngleHeadMotion(RelativeAngleHeadMotion.ACTION_UP, 30);
    RelativeAngleHeadMotion relativeAngleHeadMotionDown = new RelativeAngleHeadMotion(RelativeAngleHeadMotion.ACTION_DOWN, 0);

    Camera camera;
    boolean mIsCapturing;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        register(MainActivity.class);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_body);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        ButterKnife.bind(this);
        fcrButton.setChecked(false);
        bcrButton.setChecked(true);
        fdrButton.setChecked(false);

        hardWareManager = (HardWareManager) getUnitManager(FuncConstant.HARDWARE_MANAGER);
        headMotionManager = (HeadMotionManager) getUnitManager(FuncConstant.HEADMOTION_MANAGER);

        surfaceView.getHolder().addCallback(this);
        mIsCapturing = true;

    }
    @OnClick({R.id.bodyCam,R.id.faceCam,R.id.faceDetect})
    public void onRadioButtonClicked(RadioButton rButton) {
        boolean checked = rButton.isChecked();
        switch(rButton.getId()) {
            case R.id.faceCam:
                if (checked) {
                    Toast.makeText(this, "Right now at Face Camera", Toast.LENGTH_SHORT).show();
                    timer.cancel();
                    Intent intent = new Intent(BodyCamActivity.this,MainActivity.class);
                    startActivity(intent);
                }
                break;
            case R.id.bodyCam:
                if (checked) {
                    Toast.makeText(this, "Right now at Body Camera", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.faceDetect:
                if (checked) {
                    Toast.makeText(this, "Right now at Face Detection", Toast.LENGTH_SHORT).show();
                    timer.cancel();
                    Intent intent = new Intent(BodyCamActivity.this,FaceDetectionActivity.class);
                    startActivity(intent);
                }
                break;
        }

    }
    @Override
    protected void onMainServiceConnected() {
        timer = new CountDownTimer(10000, 5000) {
            @Override
            public void onTick(long l) {
                hardWareManager.switchWhiteLight(false);
                // headMotionManager.doAbsoluteLocateMotion(locateAbsoluteAngleHeadMotionDown);
                headMotionManager.doRelativeAngleMotion(relativeAngleHeadMotionUp);

            }

            @Override
            public void onFinish() {
                hardWareManager.switchWhiteLight(true);
                // headMotionManager.doAbsoluteLocateMotion(locateAbsoluteAngleHeadMotionUp);
                headMotionManager.doRelativeAngleMotion(relativeAngleHeadMotionDown);
                try{
                    timer.start();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }.start();
    }
    private void start_camera(SurfaceHolder holder)    {
        try{
            camera = Camera.open();
        }catch(RuntimeException e){
            Log.e(TAG, "init_camera: " + e);
            return;
        }

        try {
            camera.setPreviewDisplay(holder);
            camera.startPreview();
            //camera.takePicture(shutter, raw, jpeg)
        } catch (Exception e) {
            Log.e(TAG, "init_camera: " + e);
            return;
        }
    }

    private void stop_camera()
    {
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }
    public void surfaceChanged(SurfaceHolder holder, int arg1, int arg2, int arg3) {
        if (camera != null) {
            try {
                camera.setPreviewDisplay(holder);
                if (mIsCapturing) {
                    camera.startPreview();
                }
            } catch (IOException e) {
                Toast.makeText(BodyCamActivity.this, "Unable to start camera preview.", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        start_camera(holder);
    }

    public void surfaceDestroyed(SurfaceHolder holder) {

    }
    @Override
    protected void onPause() {
        super.onPause();
        stop_camera();

    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        stop_camera();

    }
}
