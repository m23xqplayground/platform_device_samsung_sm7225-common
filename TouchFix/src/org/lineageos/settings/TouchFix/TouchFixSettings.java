/*
 * Copyright (C) 2015 The CyanogenMod Project
 *               2017-2019 The LineageOS Project
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

import android.os.Bundle;
import android.os.Handler;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceFragment;
import androidx.preference.SwitchPreference;

import com.android.settingslib.widget.MainSwitchPreference;

import org.lineageos.internal.util.ScreenType;

public class TouchFixSettings extends PreferenceFragment
        implements OnPreferenceChangeListener, OnCheckedChangeListener {

    private SwitchPreference mAOTpreference;

    private Handler mHandler = new Handler();

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        mAOTpreference = (SwitchPreference) findPreference(Utils.AOT_ENABLE_KEY);
        mAOTpreference.setTitle("Enable Always On Touch");
        mAOTpreference.setSummary("Keeps Touch always on, but breaks Double Tap 2 Wake");
        mAOTpreference.setEnabled(AOTEnabled);
        mAOTpreference.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        // If running on a phone, remove padding around the listview
        if (!ScreenType.isTablet(getContext())) {
            getListView().setPadding(0, 0, 0, 0);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        mHandler.post(() -> Utils.checkTouchFixService(getActivity()));

        return true;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
	mAOTpreference.setChecked(isChecked);
    }
}
