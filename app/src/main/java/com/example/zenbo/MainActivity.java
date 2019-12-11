package com.example.zenbo;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.asus.robotframework.API.MotionControl;
import com.asus.robotframework.API.RobotCallback;
import com.asus.robotframework.API.RobotCmdState;
import com.asus.robotframework.API.RobotCommand;
import com.asus.robotframework.API.RobotErrorCode;
import com.asus.robotframework.API.Utility;
import com.asus.robotframework.API.results.DetectFaceResult;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends RobotActivity {
    private Button button;
    public static int count = 0;
    public static final int TURN_AROUND = 5278, CLEAR_LIST = 52778, TURN_BACK = 55278;
    private static final int MOVE_AROUND = 52278;
    public static final int FOWARD = 52788, TURN_FOWARD = 52278;
    public static final int CHECK_SONAR = 527888;
    private int remainDistance = 0;
    private float midSonar = 0.0f;
    private static int command = -1;
    private static RobotCmdState cmdState;
    private ArrayList<Float> sonarList = new ArrayList<Float>();
    private ArrayList<Float> sonarList2 = new ArrayList<Float>();
    public static boolean succeed = false;
    public static TextView[] textView = new TextView[3];
    public static SensorManager sensorManager;
    public static MainActivity mainActivity;
    private HandlerThread mainThread;
    private HandlerThread moveThread;
    private MoveHandler moveHandler;
    private static TextView print;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView[0] = (TextView) findViewById(R.id.sonar_left);
        textView[1] = (TextView) findViewById(R.id.sonar_right);
        textView[2] = (TextView) findViewById(R.id.sonar_mid);
        button = (Button) findViewById(R.id.moveButton);
        print = (TextView) findViewById(R.id.print);

        mainActivity = this;

        ButtonHandler buttonHandler = new ButtonHandler();
        button.setOnClickListener(buttonHandler);
        mySensorListener sensorListener = new mySensorListener();
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor sensor = sensorManager.getDefaultSensor(Utility.SensorType.SONAR);
        sensorManager.registerListener(sensorListener, sensor, SensorManager.SENSOR_DELAY_FASTEST);
        moveThread = new HandlerThread("Move_thread");
        moveThread.start();
        moveHandler = new MoveHandler(moveThread.getLooper());

    }

    public MainActivity() {
        super(robotCallback, robotListenCallback);
    }

    public static RobotCallback robotCallback = new RobotCallback() {
        @Override
        public void onResult(int cmd, int serial, RobotErrorCode err_code, Bundle result) {
            super.onResult(cmd, serial, err_code, result);
        }

        @Override
        public void onStateChange(int cmd, int serial, RobotErrorCode err_code, RobotCmdState state) {
            super.onStateChange(cmd, serial, err_code, state);
            command = cmd;
            cmdState = state;
            switch (state) {
                case ACTIVE:
                    print.setText("ACTIVE");
                    break;
                case FAILED:
                    print.setText("failed");
                    break;
                case SUCCEED:
                    print.setText("SUCCEED");
                    if (cmd == RobotCommand.MOTION_MOVE_BODY.getValue())
                        succeed = true;
                    break;
                case PENDING:
                    print.setText("pending");
                    break;
                case REJECTED:
                    print.setText("reject");
                    break;
                case PREEMPTED:
                    print.setText("preempted");
                    break;
            }
        }

        @Override
        public void onDetectFaceResult(List<DetectFaceResult> resultList) {
            super.onDetectFaceResult(resultList);
        }

        @Override
        public void initComplete() {
            super.initComplete();
        }
    };

    public static RobotCallback.Listen robotListenCallback = new RobotCallback.Listen() {
        @Override
        public void onFinishRegister() {
        }

        @Override
        public void onVoiceDetect(JSONObject jsonObject) {
//            Intent intent = new Intent();
//            intent.setAction("android.media.action.STILL_IMAGE_CAMERA");
//            mainActivity.startActivity(intent);
            // robotAPI.motion.remoteControlBody(MotionControl.Direction.Body.TURN_LEFT);

        }

        @Override
        public void onSpeakComplete(String s, String s1) {
        }

        @Override
        public void onEventUserUtterance(JSONObject jsonObject) {
        }

        @Override
        public void onResult(JSONObject jsonObject) {

        }

        @Override
        public void onRetry(JSONObject jsonObject) {
        }
    };

    private class mySensorListener implements SensorEventListener {
        @Override
        public void onSensorChanged(SensorEvent event) {
            textView[0].setText("Sonar " + "left" + " : " + String.valueOf(event.values[0]));
            textView[1].setText("Sonar " + "right" + " : " + String.valueOf(event.values[1]));
            textView[2].setText("Sonar " + "mid" + " : " + String.valueOf(event.values[2]));
            midSonar = event.values[2];
            //sonarList.add(new Float(event.values[2]));
            //sonarList2.add(new Float(event.values[1]));

        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    }

    private class ButtonHandler implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            robotAPI.motion.stopMoving();
            succeed = true;
            moveHandler.sendEmptyMessage(FOWARD);
        }
    }

    private class MoveHandler extends Handler {
        Message message;
        Bundle bundle;

        public MoveHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case CLEAR_LIST:
                    sonarList = new ArrayList<Float>();
                    moveHandler.sendEmptyMessage(CHECK_SONAR);

                    break;
                case FOWARD:
                    robotAPI.motion.moveBody(20f, 0.0f, 0, MotionControl.SpeedLevel.Body.L1);
                    moveHandler.sendEmptyMessage(CLEAR_LIST);
                    break;
                case TURN_AROUND:
                    if (cmdState == RobotCmdState.FAILED || cmdState == RobotCmdState.SUCCEED) {
                        robotAPI.motion.moveBody(0.0f, 0.0f, 45, MotionControl.SpeedLevel.Body.L1);
                        sonarList = new ArrayList<Float>();
                        succeed = false;
                        //sonarList2.clear();
                        moveHandler.sendEmptyMessageDelayed(CHECK_SONAR, 500);
                    } else {
                        moveHandler.sendEmptyMessageDelayed(TURN_AROUND, 100);
                    }
                    break;
                case MOVE_AROUND:
                    break;
                case CHECK_SONAR:
                    if (!succeed) {
                        moveHandler.sendEmptyMessageDelayed(CHECK_SONAR, 100);
                        break;
                    }
                    if (sonarList.size() >50)
                        sonarList = new ArrayList<Float>();
                    sonarList.add(new Float(midSonar));
                    if (sonarList.size() < 8) {
                        moveHandler.sendEmptyMessageDelayed(CHECK_SONAR, 100);
                        break;
                    } else {
                        int count = 0;
                        for (int i = sonarList.size() - 6; i < sonarList.size(); i++)
                            if (sonarList.get(i) > 0.25) count++;
                        if (count > 2) {
                            if (command == RobotCommand.MOTION_MOVE_BODY.getValue() && cmdState == RobotCmdState.ACTIVE)
                                moveHandler.sendEmptyMessageDelayed(CHECK_SONAR, 100);
                            moveHandler.sendEmptyMessage(FOWARD);
                        } else {
                            robotAPI.cancelCommandAll();
                            moveHandler.sendEmptyMessage(TURN_AROUND);
                        }
                    }

                    break;
            }
        }
    }

}
