package com.example.dark_side;

/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.wearable.complications.ComplicationHelperActivity;
import android.support.wearable.complications.ComplicationProviderInfo;
import android.support.wearable.complications.ProviderChooserIntent;
import android.support.wearable.complications.ProviderInfoRetriever;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.util.concurrent.Executors;

/**
 * The watch-side config activity for {@link DarkSideWatchFaceService}, which allows for setting
 * the left and right complications of watch face.
 */
public class ComplicationConfigActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "ConfigActivity";

    static final int COMPLICATION_CONFIG_REQUEST_CODE = 1001;

    /**
     * Used by associated watch face ({@link DarkSideWatchFaceService}) to let this
     * configuration Activity know which complication locations are supported, their ids, and
     * supported complication data types.
     */
    // TODO: Step 3, intro 1
    public enum ComplicationLocation {
        UPPER_LEFT,
        UPPER_RIGHT,
        LOWER_LEFT,
        LOWER_RIGHT
    }

    private int mUpperLeftComplicationId;
    private int mUpperRightComplicationId;

    private int mLowerLeftComplicationId;
    private int mLowerRightComplicationId;
    
    // Selected complication id by user.
    private int mSelectedComplicationId;

    // ComponentName used to identify a specific service that renders the watch face.
    private ComponentName mWatchFaceComponentName;

    // Required to retrieve complication data from watch face for preview.
    private ProviderInfoRetriever mProviderInfoRetriever;

    private ImageView mUpperLeftComplicationBackground;
    private ImageView mUpperRightComplicationBackground;
    private ImageView mLowerLeftComplicationBackground;
    private ImageView mLowerRightComplicationBackground;
    
    
    private ImageButton mUpperLeftComplication;
    private ImageButton mUpperRightComplication;
    private ImageButton mLowerLeftComplication;
    private ImageButton mLowerRightComplication;
    
    
    private Drawable mDefaultAddComplicationDrawable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_config);

        mDefaultAddComplicationDrawable = getDrawable(R.drawable.add_complication);

        // TODO: Step 3, initialize 1

        mSelectedComplicationId = -1;

        mUpperLeftComplicationId =
                DarkSideWatchFaceService.getComplicationId(ComplicationLocation.UPPER_LEFT);
        mUpperRightComplicationId =
                DarkSideWatchFaceService.getComplicationId(ComplicationLocation.UPPER_RIGHT);
        mLowerLeftComplicationId =
                DarkSideWatchFaceService.getComplicationId(ComplicationLocation.LOWER_LEFT);
        mLowerRightComplicationId =
                DarkSideWatchFaceService.getComplicationId(ComplicationLocation.LOWER_RIGHT);

        mWatchFaceComponentName =
                new ComponentName(getApplicationContext(), DarkSideWatchFaceService.class);


        // Sets up upper left complication preview.
        mUpperLeftComplicationBackground = (ImageView) findViewById(R.id.upper_left_complication_background);
        mUpperLeftComplication = (ImageButton) findViewById(R.id.upper_left_complication);
        mUpperLeftComplication.setOnClickListener(this);

        // Sets default as "Add Complication" icon.
        mUpperLeftComplication.setImageDrawable(mDefaultAddComplicationDrawable);
        mUpperLeftComplicationBackground.setVisibility(View.INVISIBLE);

        // Sets up upper right complication preview.
        mUpperRightComplicationBackground = (ImageView) findViewById(R.id.upper_right_complication_background);
        mUpperRightComplication = (ImageButton) findViewById(R.id.upper_right_complication);
        mUpperRightComplication.setOnClickListener(this);

        // Sets default as "Add Complication" icon.
        mUpperRightComplication.setImageDrawable(mDefaultAddComplicationDrawable);
        mUpperRightComplicationBackground.setVisibility(View.INVISIBLE);

        // Sets up lower left complication preview.
        mLowerLeftComplicationBackground = (ImageView) findViewById(R.id.lower_left_complication_background);
        mLowerLeftComplication = (ImageButton) findViewById(R.id.lower_left_complication);
        mLowerLeftComplication.setOnClickListener(this);

        // Sets default as "Add Complication" icon.
        mLowerLeftComplication.setImageDrawable(mDefaultAddComplicationDrawable);
        mLowerLeftComplicationBackground.setVisibility(View.INVISIBLE);

        // Sets up lower right complication preview.
        mLowerRightComplicationBackground = (ImageView) findViewById(R.id.lower_right_complication_background);
        mLowerRightComplication = (ImageButton) findViewById(R.id.lower_right_complication);
        mLowerRightComplication.setOnClickListener(this);

        // Sets default as "Add Complication" icon.
        mLowerRightComplication.setImageDrawable(mDefaultAddComplicationDrawable);
        mLowerRightComplicationBackground.setVisibility(View.INVISIBLE);

        // TODO: Step 3, initialize 2
        mProviderInfoRetriever =
                new ProviderInfoRetriever(getApplicationContext(),Executors.newCachedThreadPool());
        mProviderInfoRetriever.init();

        retrieveInitialComplicationsData();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // TODO: Step 3, release
        mProviderInfoRetriever.release();
    }

    // TODO: Step 3, retrieve complication data
    public void retrieveInitialComplicationsData() {

        final int[] complicationIds = DarkSideWatchFaceService.getComplicationIds();

        mProviderInfoRetriever.retrieveProviderInfo(
                new ProviderInfoRetriever.OnProviderInfoReceivedCallback() {
                    @Override
                    public void onProviderInfoReceived(
                            int watchFaceComplicationId,
                            @Nullable ComplicationProviderInfo complicationProviderInfo) {

                        Log.d(TAG, "\n\nonProviderInfoReceived: " + complicationProviderInfo);

                        updateComplicationViews(watchFaceComplicationId, complicationProviderInfo);
                    }
                },
                mWatchFaceComponentName,
                complicationIds);
    }

    @Override
    public void onClick(View view) {
        if (view.equals(mUpperLeftComplication)) {
            Log.d(TAG, "Upper Left Complication click()");
            launchComplicationHelperActivity(ComplicationLocation.UPPER_LEFT);

        } else if (view.equals(mUpperRightComplication)) {
            Log.d(TAG, "Upper Right Complication click()");
            launchComplicationHelperActivity(ComplicationLocation.UPPER_RIGHT);
        } else if (view.equals(mLowerLeftComplication)) {
            Log.d(TAG, "Lower Left Complication click()");
            launchComplicationHelperActivity(ComplicationLocation.LOWER_LEFT);
        } else if (view.equals(mLowerRightComplication)) {
            Log.d(TAG, "Lower Right Complication click()");
            launchComplicationHelperActivity(ComplicationLocation.LOWER_RIGHT);
        }
    }

    // Verifies the watch face supports the complication location, then launches the helper
    // class, so user can choose their complication data provider.
    // TODO: Step 3, launch data selector
    private void launchComplicationHelperActivity(ComplicationLocation complicationLocation) {

        mSelectedComplicationId =
                DarkSideWatchFaceService.getComplicationId(complicationLocation);

        if (mSelectedComplicationId >= 0) {

            int[] supportedTypes =
                    DarkSideWatchFaceService.getSupportedComplicationTypes(
                            complicationLocation);

            startActivityForResult(
                    ComplicationHelperActivity.createProviderChooserHelperIntent(
                            getApplicationContext(),
                            mWatchFaceComponentName,
                            mSelectedComplicationId,
                            supportedTypes),
                    ComplicationConfigActivity.COMPLICATION_CONFIG_REQUEST_CODE);

        } else {
            Log.d(TAG, "Complication not supported by watch face.");
        }
    }

    public void updateComplicationViews(
            int watchFaceComplicationId, ComplicationProviderInfo complicationProviderInfo) {
        Log.d(TAG, "updateComplicationViews(): id: " + watchFaceComplicationId);
        Log.d(TAG, "\tinfo: " + complicationProviderInfo);

        if (watchFaceComplicationId == mUpperLeftComplicationId) {
            if (complicationProviderInfo != null) {
                mUpperLeftComplication.setImageIcon(complicationProviderInfo.providerIcon);
                mUpperLeftComplicationBackground.setVisibility(View.VISIBLE);

            } else {
                mUpperLeftComplication.setImageDrawable(mDefaultAddComplicationDrawable);
                mUpperLeftComplicationBackground.setVisibility(View.INVISIBLE);
            }

        } else if (watchFaceComplicationId == mUpperRightComplicationId) {
            if (complicationProviderInfo != null) {
                mUpperRightComplication.setImageIcon(complicationProviderInfo.providerIcon);
                mUpperRightComplicationBackground.setVisibility(View.VISIBLE);

            } else {
                mUpperRightComplication.setImageDrawable(mDefaultAddComplicationDrawable);
                mUpperRightComplicationBackground.setVisibility(View.INVISIBLE);
            }
        } else if (watchFaceComplicationId == mLowerLeftComplicationId) {
            if (complicationProviderInfo != null) {
                mLowerLeftComplication.setImageIcon(complicationProviderInfo.providerIcon);
                mLowerLeftComplicationBackground.setVisibility(View.VISIBLE);

            } else {
                mLowerLeftComplication.setImageDrawable(mDefaultAddComplicationDrawable);
                mLowerLeftComplicationBackground.setVisibility(View.INVISIBLE);
            }
        } else if (watchFaceComplicationId == mLowerRightComplicationId) {
            if (complicationProviderInfo != null) {
                mLowerRightComplication.setImageIcon(complicationProviderInfo.providerIcon);
                mLowerRightComplicationBackground.setVisibility(View.VISIBLE);

            } else {
                mLowerRightComplication.setImageDrawable(mDefaultAddComplicationDrawable);
                mLowerRightComplicationBackground.setVisibility(View.INVISIBLE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        // TODO: Step 3, update views
        if (requestCode == COMPLICATION_CONFIG_REQUEST_CODE && resultCode == RESULT_OK) {

            // Retrieves information for selected Complication provider.
            ComplicationProviderInfo complicationProviderInfo =
                    data.getParcelableExtra(ProviderChooserIntent.EXTRA_PROVIDER_INFO);
            Log.d(TAG, "Provider: " + complicationProviderInfo);

            if (mSelectedComplicationId >= 0) {
                updateComplicationViews(mSelectedComplicationId, complicationProviderInfo);
            }
        }
    }
}