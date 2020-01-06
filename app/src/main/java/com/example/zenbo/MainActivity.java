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
    public static final int TURN_AROUND = 5278, TURN_AROUND_BACK = 52778, TURN_BACK = 55278;
    private static final int CENTIMETERS = 52278;
    public static final int HALF_METER = 52788, TURN_FOWARD = 52278;
    public static final int CHECK_SONAR = 527888;
    private int remainAngle = 0;
    private float midSonar = 0.0f;
    private float leftSonar = 0.0f;
    private float rightSonar = 0.0f;
    private static int command = -1;
    private static RobotCmdState cmdState;
    private ArrayList<Triple> sonarList = new ArrayList<Triple>();
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
            leftSonar = event.values[1];
            rightSonar = event.values[0];
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
            robotAPI.cancelCommandAll();
            succeed = true;
            //robotAPI.motion.moveBody(0.5f, 0.5f, 0, MotionControl.SpeedLevel.Body.L1);
            moveHandler.sendEmptyMessage(CHECK_SONAR);
        }
    }

    private class Triple {
        public float left;
        public float middle;
        public float right;

        Triple(float l, float m, float r) {
            left = l;
            middle = m;
            right = r;
        }
    }


    private class MoveHandler extends Handler {
        Message message;
        Bundle bundle;
        int distanceY = 20;


        public MoveHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case TURN_AROUND_BACK:
                    if (!succeed) {
                        moveHandler.sendEmptyMessageDelayed(TURN_AROUND_BACK, 100);
                        break;
                    }
                    move_around(distanceY*-1);
                    moveHandler.sendEmptyMessage(CHECK_SONAR);

                    break;
                case HALF_METER:
                    move_half_meter();
                    moveHandler.sendEmptyMessage(CHECK_SONAR);
                    break;
                case TURN_AROUND:
                   move_around(distanceY);
                    moveHandler.sendEmptyMessage(TURN_AROUND_BACK);
                    break;
                case CENTIMETERS:
                    move_10_centimeters();
                    moveHandler.sendEmptyMessage(CHECK_SONAR);
                    break;
                case CHECK_SONAR:
                    if (!succeed) {
                        moveHandler.sendEmptyMessageDelayed(CHECK_SONAR, 100);
                        break;
                    }

                    sonarList = new ArrayList<Triple>();
//                    if (sonarList.size() > 50)
//                        sonarList = new ArrayList<Triple>();
                    while (sonarList.size() < 8) {
                        sonarList.add(new Triple(leftSonar, midSonar, rightSonar));
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
//                        moveHandler.sendEmptyMessageDelayed(CHECK_SONAR, 100);
                    }
                    int half_meter_count = 0;
                    int centimeters_count = 0;
                    int turnAroundCount = 0;
                    int leftSonarCount = 0;
                    int rightSonarCount = 0;
                    for (int i = sonarList.size() - 7; i < sonarList.size(); i++) {
                        if (sonarList.get(i).middle > 1.1) half_meter_count++;
                        if (sonarList.get(i).middle > 0.55 && sonarList.get(i).middle <= 1.1)
                            centimeters_count++;
                        if (sonarList.get(i).middle <= 0.55) turnAroundCount++;
                        if (sonarList.get(i).left <= 0.6) leftSonarCount++;
                        if (sonarList.get(i).right <= 0.6) rightSonarCount++;
                    }
                    if (half_meter_count > 3) {
                        moveHandler.sendEmptyMessage(HALF_METER);
                    } else if (centimeters_count > 3) {
                        moveHandler.sendEmptyMessage(CENTIMETERS);
                    } else if (turnAroundCount > 3) {
                        if (leftSonarCount > 3 && rightSonarCount <= 3)
                            distanceY = -20;
                        else if (leftSonarCount <= 3 && rightSonarCount > 3)
                            distanceY = 20;
                        else if (leftSonarCount <= 3 && rightSonarCount <= 3)
                            distanceY = (leftSonarCount < rightSonarCount) ? 30 : -30;
                        else {
                            //print.setText("FK");
                            break;
                        }
                        moveHandler.sendEmptyMessage(TURN_AROUND);
                    }
                    break;
            }

            //break;
        }
    }

    private void move_half_meter() {
        robotAPI.cancelCommandAll();
        succeed = false;

        robotAPI.motion.moveBody(0.5f, 0.0f, 0, MotionControl.SpeedLevel.Body.L1);
        return;
    }

    private void move_10_centimeters() {
        robotAPI.cancelCommandAll();
        succeed = false;
        robotAPI.motion.moveBody(0.1f, 0.0f, 0, MotionControl.SpeedLevel.Body.L1);
        return;
    }

    private void move_around(int centimetersY) {
        robotAPI.cancelCommandAll();
        succeed = false;
        robotAPI.motion.moveBody(0.8f, centimetersY/100.0f, 0, MotionControl.SpeedLevel.Body.L1);
        return;
    }

}


