package com.anewtech.phone.sanbotcameras6;

import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.RadioButton;
import android.widget.Toast;

import com.qihancloud.opensdk.base.BindBaseActivity;
import com.qihancloud.opensdk.beans.FuncConstant;
import com.qihancloud.opensdk.function.beans.StreamOption;
import com.qihancloud.opensdk.function.beans.headmotion.LocateAbsoluteAngleHeadMotion;
import com.qihancloud.opensdk.function.beans.headmotion.RelativeAngleHeadMotion;
import com.qihancloud.opensdk.function.unit.HardWareManager;
import com.qihancloud.opensdk.function.unit.HeadMotionManager;
import com.qihancloud.opensdk.function.unit.MediaManager;
import com.qihancloud.opensdk.function.unit.interfaces.media.MediaStreamListener;
import java.io.IOException;
import java.nio.ByteBuffer;;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class MainActivity extends BindBaseActivity implements SurfaceHolder.Callback {

    private final static String TAG = MainActivity.class.getSimpleName();

    @Bind(R.id.sfv_video)
    SurfaceView surfaceView;
    // Radio Buttons
    @Bind(R.id.faceCam)
    RadioButton fcrButton;
    @Bind(R.id.bodyCam)
    RadioButton bcrButton;
    @Bind(R.id.faceDetect)
    RadioButton fdrButton;

    CountDownTimer timer;

    MediaCodec videoDecoder;
    ByteBuffer[] videoInputBuffers;
    final static String videoMimeType = "video/avc";
    AudioTrack audioTrack;
    Surface surface;
    MediaCodec.BufferInfo videoBufferInfo = new MediaCodec.BufferInfo();
    long decodeTimeout = 16000;
    int i=0;
    MediaManager mediaManager;
    HardWareManager hardWareManager;
    HeadMotionManager headMotionManager;
    LocateAbsoluteAngleHeadMotion locateAbsoluteAngleHeadMotionUp;
    LocateAbsoluteAngleHeadMotion locateAbsoluteAngleHeadMotionDown;

    RelativeAngleHeadMotion relativeAngleHeadMotionUp = new RelativeAngleHeadMotion(RelativeAngleHeadMotion.ACTION_UP, 30);
    RelativeAngleHeadMotion relativeAngleHeadMotionDown = new RelativeAngleHeadMotion(RelativeAngleHeadMotion.ACTION_DOWN, 0);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        register(MainActivity.class);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        ButterKnife.bind(this);
        fcrButton.setChecked(true);
        bcrButton.setChecked(false);
        fdrButton.setChecked(false);

        hardWareManager = (HardWareManager) getUnitManager(FuncConstant.HARDWARE_MANAGER);
        headMotionManager = (HeadMotionManager) getUnitManager(FuncConstant.HEADMOTION_MANAGER);
        locateAbsoluteAngleHeadMotionUp = new LocateAbsoluteAngleHeadMotion(LocateAbsoluteAngleHeadMotion.ACTION_BOTH_LOCK, 90,30);
        locateAbsoluteAngleHeadMotionDown = new LocateAbsoluteAngleHeadMotion(LocateAbsoluteAngleHeadMotion.ACTION_BOTH_LOCK, 90,0);

        mediaManager = (MediaManager) getUnitManager(FuncConstant.MEDIA_MANAGER);
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.adjustStreamVolume(AudioManager.STREAM_VOICE_CALL,AudioManager.ADJUST_LOWER,
                AudioManager.FLAG_SHOW_UI);
        int sampleRate = 8000;
        int minBufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, minBufferSize+100, AudioTrack.MODE_STREAM);
        audioTrack.play();
        mediaManager.setMediaListener(new MediaStreamListener() {
            @Override
            public void getVideoStream(byte[] data) {
                Log.i("info", "start streaming video");
                drawVideoSample(ByteBuffer.wrap(data));
            }

            @Override
            public void getAudioStream(byte[] data) {
                Log.i("info", "start streaming audio" + i++);
                audioTrack.write(data,0,data.length);
            }
        });
        surfaceView.getHolder().addCallback(this);

    }



    public void drawVideoSample(ByteBuffer sampleData) {
        try {
            // put sample data
            int inIndex = videoDecoder.dequeueInputBuffer(decodeTimeout);
            if (inIndex >= 0) {
                ByteBuffer buffer = videoInputBuffers[inIndex];
                int sampleSize = sampleData.limit();
                buffer.clear();
                buffer.put(sampleData);
                buffer.flip();
                // Log.i("DecodeActivity", "" + buffer.toString());
                videoDecoder.queueInputBuffer(inIndex, 0, sampleSize, 0, 0);
            }
            // output, 1 microseconds = 100,0000 / 1 second
            int ret = videoDecoder.dequeueOutputBuffer(videoBufferInfo, decodeTimeout);
            if (ret < 0) {
                onDecodingError(ret);
                return;
            }
            videoDecoder.releaseOutputBuffer(ret, true);
        } catch (Exception e) {
            Log.e(TAG, "发生错误", e);
        }
    }

    @OnClick({R.id.bodyCam,R.id.faceCam,R.id.faceDetect})
    public void onRadioButtonClicked(RadioButton rButton) {
        boolean checked = rButton.isChecked();
        switch(rButton.getId()) {
            case R.id.faceCam:
                if (checked) {
                Toast.makeText(this, "Right now at Face Camera", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.bodyCam:
                if (checked) {
                Toast.makeText(this, "Right now at Body Camera", Toast.LENGTH_SHORT).show();
                timer.cancel();
                Intent intent = new Intent(MainActivity.this,BodyCamActivity.class);
                startActivity(intent);
                }
                break;
            case R.id.faceDetect:
                if (checked) {
                    Toast.makeText(this, "Right now at Face Detection", Toast.LENGTH_SHORT).show();
                    timer.cancel();
                    // stopDecoding();
                    Intent intent = new Intent(MainActivity.this,FaceDetectionActivity.class);
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

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        surface = holder.getSurface();
        startDecoding(1280, 720);
        StreamOption streamOption = new StreamOption();
        streamOption.setChannel(StreamOption.SUB_STREAM);
        String result = mediaManager.openStream(streamOption).getResult();
        Log.e("result", result);
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        mediaManager.closeStream();
        stopDecoding();
        audioTrack.stop();
        audioTrack.release();
        Log.e("result", "关闭surface");
    }
    private void onDecodingError(int index) {
        switch (index) {
            case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                Log.e(TAG, "onDecodingError: The output buffers have changed");
                // The output buffers have changed, the client must refer to the
                // new
                // set of output buffers returned by getOutputBuffers() from
                // this
                // point on.
                // outputBuffers = decoder.getOutputBuffers();
                break;

            case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                Log.d(TAG, "New format: " + videoDecoder.getOutputFormat());
                // The output format has changed, subsequent data will follow
                // the
                // new format. getOutputFormat() returns the new format.
                break;

            case MediaCodec.INFO_TRY_AGAIN_LATER:
                Log.d(TAG, "dequeueOutputBuffer timed out!");
                // If a non-negative timeout had been specified in the call to
                // dequeueOutputBuffer(MediaCodec.BufferInfo, long), indicates
                // that
                // the call timed out.
                break;

            default:
                break;
        }
    }
    private boolean startDecoding(int width, int height) {
        try {
            if (videoInputBuffers != null) {
                Log.w(TAG,
                        "startDecoding: videoInputBuffers already created!");
                return false;

            } else if (videoDecoder != null) {
                Log.w(TAG, "startDecoding: videoDecoder already created!");
                return false;

            }
            // format
            MediaFormat format = MediaFormat.createVideoFormat(
                    videoMimeType, width, height);
            Log.i(TAG, "" + format);

            videoDecoder = MediaCodec.createDecoderByType(videoMimeType);
            videoDecoder.configure(format, surface, null, 0);
            videoDecoder.start();

            videoInputBuffers = videoDecoder.getInputBuffers();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            Log.e("CODEC", "onCreateCodec");
        }
        return true;
    }


    public void stopDecoding() {
        if (videoDecoder != null) {
            videoDecoder.stop();
            videoDecoder.release();
            videoDecoder = null;
            Log.i(TAG, "stopDecoding");
        }
        videoInputBuffers = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
