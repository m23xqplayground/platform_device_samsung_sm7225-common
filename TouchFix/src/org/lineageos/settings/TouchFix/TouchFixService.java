/*
 * Copyright (c) 2015 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lineageos.settings.TouchFix;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.TriggerEvent;
import android.hardware.TriggerEventListener;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class SamsungTouchFixService extends Service {
    private static final String TAG = "TouchFix";
    private static final boolean DEBUG = true;

    private static final String TouchFix_INTENT = "com.android.systemui.TouchFix";

    private Context mContext;
    private ExecutorService mExecutorService;
    private TouchFixService mTouchFixService;
    private AOTService mAOTService;
    private PowerManager mPowerManager;

    private boolean mTouchFixServiceEnabled = true;
    private boolean mAOTServiceEnabled = false;

    class TouchFixService {

        private static final String CMD_FILE_PATH = "/sys/class/sec/tsp/cmd";
        private static final String ENABLED_FILE_PATH = "/sys/class/sec/tsp/enabled";

        protected void enable() {
            writeToFile(CMD_FILE_PATH, "check_connection");
            writeToFile(ENABLED_FILE_PATH, "2,0");
            writeToFile(ENABLED_FILE_PATH, "2,1");
        }

        protected void disable() {
            writeToFile(ENABLED_FILE_PATH, "1,0");
        }

        private void writeToFile(String filePath, String data) {
            File file = new File(filePath);
            try {
                FileWriter writer = new FileWriter(file);
                writer.write(data);
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private TriggerEventListener mPickupListener = new TriggerEventListener() {
            @Override
            public void onTrigger(TriggerEvent event) {
                if (DEBUG) Log.d(TAG, "Touchfixservice Triggered");
                wakeOrLaunchTouchFixPulse();
                enable();
            }
        };
    }

    class AOTService {

        private static final String CMD_FILE_PATH = "/sys/class/sec/tsp/cmd";

        protected void enable() {
            writeToFile(CMD_FILE_PATH, "aot_enable,1");
        }

        protected void disable() {
            writeToFile(CMD_FILE_PATH, "aot_enable,0");
        }

        private void writeToFile(String filePath, String data) {
            File file = new File(filePath);
            try {
                FileWriter writer = new FileWriter(file);
                writer.write(data);
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private TriggerEventListener mPickupListener = new TriggerEventListener() {
            @Override
            public void onTrigger(TriggerEvent event) {
                if (DEBUG) Log.d(TAG, "Touchfixservice Triggered");
                wakeOrLaunchTouchFixPulse();
                enable();
            }
        };
    }

    @Override
    public void onCreate() {
        if (DEBUG) Log.d(TAG, "TouchFixService Started");
        mContext = this;
        mPowerManager = getSystemService(PowerManager.class);
        mAOTService = new AOTService(mContext);
        mTouchFixService = new TouchFixService(mContext);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (DEBUG) Log.d(TAG, "Starting TouchFix service");
        IntentFilter screenStateFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        screenStateFilter.addAction(Intent.ACTION_SCREEN_OFF);
        mContext.registerReceiver(mScreenStateReceiver, screenStateFilter);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void wakeOrLaunchTouchFixPulse() {
        if (Utils.isWakeOnGestureEnabled(mContext)) {
            if (DEBUG) Log.d(TAG, "Wake up display");
            PowerManager powerManager = mContext.getSystemService(PowerManager.class);
            powerManager.wakeUp(SystemClock.uptimeMillis(), PowerManager.WAKE_REASON_GESTURE, TAG);
        } else {
            if (DEBUG) Log.d(TAG, "Launch TouchFix pulse");
            mContext.sendBroadcastAsUser(
                    new Intent(TouchFix_INTENT), new UserHandle(UserHandle.USER_CURRENT));
        }
    }

    private boolean isInteractive() {
        return mPowerManager.isInteractive();
    }

    private void onDisplayOn() {
        if (DEBUG) Log.d(TAG, "Display on");
        mTouchFixService.enable();
    }

    private void onDisplayOff() {
        if (DEBUG) Log.d(TAG, "Display off");
        mTouchFixService.disable();
    }

    private BroadcastReceiver mScreenStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                onDisplayOff();
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                onDisplayOn();
            }
        }
    };
}
