package com.example.dark_side;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationHelperActivity;
import android.support.wearable.complications.rendering.ComplicationDrawable;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Digital watch face with seconds. In ambient mode, the seconds aren't displayed. On devices with
 * low-bit ambient mode, the text is drawn without anti-aliasing in ambient mode.
 * <p>
 * Important Note: Because watch face apps do not have a default Activity in
 * their project, you will need to set your Configurations to
 * "Do not launch Activity" for both the Wear and/or Application modules. If you
 * are unsure how to do this, please review the "Run Starter project" section
 * in the Google Watch Face Code Lab:
 * https://codelabs.developers.google.com/codelabs/watchface/index.html#0
 */
public class DarkSideWatchFaceService extends CanvasWatchFaceService {
    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

    private static final String TAG = "DarkSideWatchFace";

    /**
     * Update rate in milliseconds for interactive mode. Defaults to one second
     * because the watch face needs to update seconds in interactive mode.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;


    // TODO: Step 2, intro 1
    private static final int UPPER_LEFT_COMPLICATION_ID = 0;
    private static final int UPPER_RIGHT_COMPLICATION_ID = 1;
    private static final int LOWER_LEFT_COMPLICATION_ID = 2;
    private static final int LOWER_RIGHT_COMPLICATION_ID = 3;

    private static final int[] COMPLICATION_IDS = {UPPER_LEFT_COMPLICATION_ID, UPPER_RIGHT_COMPLICATION_ID, LOWER_LEFT_COMPLICATION_ID, LOWER_RIGHT_COMPLICATION_ID};

    // Left and right dial supported types.
    private static final int[][] COMPLICATION_SUPPORTED_TYPES = {
            {
                    ComplicationData.TYPE_RANGED_VALUE,
                    ComplicationData.TYPE_ICON,
                    ComplicationData.TYPE_SHORT_TEXT,
                    ComplicationData.TYPE_SMALL_IMAGE
            },
            {
                    ComplicationData.TYPE_RANGED_VALUE,
                    ComplicationData.TYPE_ICON,
                    ComplicationData.TYPE_SHORT_TEXT,
                    ComplicationData.TYPE_SMALL_IMAGE
            },
            {
                    ComplicationData.TYPE_RANGED_VALUE,
                    ComplicationData.TYPE_ICON,
                    ComplicationData.TYPE_SHORT_TEXT,
                    ComplicationData.TYPE_SMALL_IMAGE
            },
            {
                    ComplicationData.TYPE_RANGED_VALUE,
                    ComplicationData.TYPE_ICON,
                    ComplicationData.TYPE_SHORT_TEXT,
                    ComplicationData.TYPE_SMALL_IMAGE
            }
    };

    // Used by {@link ComplicationConfigActivity} to retrieve id for complication locations and
    // to check if complication location is supported.
    // TODO: Step 3, expose complication information, part 1
    static int getComplicationId(
            ComplicationConfigActivity.ComplicationLocation complicationLocation) {
        switch (complicationLocation) {
            case UPPER_LEFT:
                return UPPER_LEFT_COMPLICATION_ID;
            case UPPER_RIGHT:
                return UPPER_RIGHT_COMPLICATION_ID;
            case LOWER_LEFT:
                return LOWER_LEFT_COMPLICATION_ID;
            case LOWER_RIGHT:
                return LOWER_RIGHT_COMPLICATION_ID;
            default:
                return -1;
        }
    }

    // Used by {@link ComplicationConfigActivity} to retrieve all complication ids.
    // TODO: Step 3, expose complication information, part 2
    static int[] getComplicationIds() {
        return COMPLICATION_IDS;
    }

    // Used by {@link ComplicationConfigActivity} to retrieve complication types supported by
    // location.
    // TODO: Step 3, expose complication information, part 3
    static int[] getSupportedComplicationTypes(
            ComplicationConfigActivity.ComplicationLocation complicationLocation) {
        switch (complicationLocation) {
            case UPPER_LEFT:
                return COMPLICATION_SUPPORTED_TYPES[0];
            case UPPER_RIGHT:
                return COMPLICATION_SUPPORTED_TYPES[1];
            case LOWER_LEFT:
                return COMPLICATION_SUPPORTED_TYPES[0];
            case LOWER_RIGHT:
                return COMPLICATION_SUPPORTED_TYPES[1];
            default:
                return new int[] {};
        }
    }

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<DarkSideWatchFaceService.Engine> mWeakReference;

        public EngineHandler(DarkSideWatchFaceService.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            DarkSideWatchFaceService.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    private class Engine extends CanvasWatchFaceService.Engine {

        private final Handler mUpdateTimeHandler = new EngineHandler(this);
        private Calendar mCalendar;
        private final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };
        private Bitmap mBackgroundBitmap;
        private boolean mRegisteredTimeZoneReceiver = false;
        private float mXOffset;
        private float mYOffset;
        private float mTSYOffset;
        private float mDayYOffset;
        private float mDateYOffset;
        private Paint mBackgroundPaint;
        private Paint mTextPaint;
        private Paint mTSPaint;
        private Paint mDayPaint;
        private Paint mDatePaint;

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        private boolean mLowBitAmbient;
        private boolean mBurnInProtection;
        private boolean mAmbient;


        private int mWidth;
        private int mHeight;
        private float mCenterX;
        private float mCenterY;
        private float mScale = 1;


        // TODO: Step 2, intro 2
        /* Maps active complication ids to the data for that complication. Note: Data will only be
         * present if the user has chosen a provider via the settings activity for the watch face.
         */
        private SparseArray<ComplicationData> mActiveComplicationDataSparseArray;

        /* Maps complication ids to corresponding ComplicationDrawable that renders the
         * the complication data on the watch face.
         */
        private SparseArray<ComplicationDrawable> mComplicationDrawableSparseArray;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(DarkSideWatchFaceService.this)
                    .setAcceptsTapEvents(true)
                    .build());

            mCalendar = Calendar.getInstance();

            Resources resources = DarkSideWatchFaceService.this.getResources();
            mYOffset = resources.getDimension(R.dimen.digital_time_y_offset);
            mTSYOffset = resources.getDimension(R.dimen.digital_ts_y_offset);
            mDayYOffset = resources.getDimension(R.dimen.digital_day_y_offset);
            mDateYOffset = resources.getDimension(R.dimen.digital_date_y_offset);

            // Initializes background.
            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(
                    ContextCompat.getColor(getApplicationContext(), R.color.background));


            mBackgroundBitmap = BitmapFactory
                    .decodeResource(getResources(), R.drawable.custom_background);

            // Initializes Watch Face.
            mTextPaint = new Paint();
            mTextPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
            mTextPaint.setAntiAlias(true);
            mTextPaint.setColor(
                    ContextCompat.getColor(getApplicationContext(), R.color.digital_text));
            mTextPaint.setTextAlign(Paint.Align.CENTER);

            mTSPaint = new Paint();
            mTSPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
            mTSPaint.setAntiAlias(true);
            mTSPaint.setColor(
                    ContextCompat.getColor(getApplicationContext(), R.color.digital_text));
            mTSPaint.setTextAlign(Paint.Align.CENTER);

            mDayPaint = new Paint();
            mDayPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
            mDayPaint.setAntiAlias(true);
            mDayPaint.setColor(
                    ContextCompat.getColor(getApplicationContext(), R.color.digital_text));
            mDayPaint.setTextAlign(Paint.Align.CENTER);

            mDatePaint = new Paint();
            mDatePaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
            mDatePaint.setAntiAlias(true);
            mDatePaint.setColor(
                    ContextCompat.getColor(getApplicationContext(), R.color.digital_text));
            mDatePaint.setTextAlign(Paint.Align.CENTER);

            // TODO: Step 2, intro 3
            initializeComplications();
        }

        // TODO: Step 2, initializeComplications()
        private void initializeComplications() {
            Log.d(TAG, "initializeComplications()");

            mActiveComplicationDataSparseArray = new SparseArray<>(COMPLICATION_IDS.length);

            ComplicationDrawable upperLeftComplicationDrawable =
                    (ComplicationDrawable) getDrawable(R.drawable.custom_complication_styles);
            upperLeftComplicationDrawable.setContext(getApplicationContext());

            ComplicationDrawable upperRightComplicationDrawable =
                    (ComplicationDrawable) getDrawable(R.drawable.custom_complication_styles);
            upperRightComplicationDrawable.setContext(getApplicationContext());

            ComplicationDrawable lowerLeftComplicationDrawable =
                    (ComplicationDrawable) getDrawable(R.drawable.custom_complication_styles);
            lowerLeftComplicationDrawable.setContext(getApplicationContext());

            ComplicationDrawable lowerRightComplicationDrawable =
                    (ComplicationDrawable) getDrawable(R.drawable.custom_complication_styles);
            lowerRightComplicationDrawable.setContext(getApplicationContext());

            mComplicationDrawableSparseArray = new SparseArray<>(COMPLICATION_IDS.length);
            mComplicationDrawableSparseArray.put(UPPER_LEFT_COMPLICATION_ID, upperLeftComplicationDrawable);
            mComplicationDrawableSparseArray.put(UPPER_RIGHT_COMPLICATION_ID, upperRightComplicationDrawable);
            mComplicationDrawableSparseArray.put(LOWER_LEFT_COMPLICATION_ID, lowerLeftComplicationDrawable);
            mComplicationDrawableSparseArray.put(LOWER_RIGHT_COMPLICATION_ID, lowerRightComplicationDrawable);

            setActiveComplications(COMPLICATION_IDS);
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            DarkSideWatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            DarkSideWatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            // Load resources that have alternate values for round watches.
            Resources resources = DarkSideWatchFaceService.this.getResources();

            mXOffset = resources.getDimension(R.dimen.digital_time_x_offset);
            mTextPaint.setTextSize(resources.getDimension(R.dimen.digital_time_size));
            mTSPaint.setTextSize(resources.getDimension(R.dimen.digital_ts_size));
            mDayPaint.setTextSize(resources.getDimension(R.dimen.digital_day_size));
            mDatePaint.setTextSize(resources.getDimension(R.dimen.digital_date_size));
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            mBurnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);

            mAmbient = inAmbientMode;
            if (mLowBitAmbient) {
                mTextPaint.setAntiAlias(!inAmbientMode);
            }

            // TODO: Step 2, ambient
            ComplicationDrawable complicationDrawable;

            for (int i = 0; i < COMPLICATION_IDS.length; i++) {
                complicationDrawable = mComplicationDrawableSparseArray.get(COMPLICATION_IDS[i]);
                complicationDrawable.setInAmbientMode(mAmbient);
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        // TODO: Step 2, onComplicationDataUpdate()
        @Override
        public void onComplicationDataUpdate(
                int complicationId, ComplicationData complicationData) {
            Log.d(TAG, "onComplicationDataUpdate() id: " + complicationId);

            // Adds/updates active complication data in the array.
            mActiveComplicationDataSparseArray.put(complicationId, complicationData);

            // Updates correct ComplicationDrawable with updated data.
            ComplicationDrawable complicationDrawable =
                    mComplicationDrawableSparseArray.get(complicationId);
            complicationDrawable.setComplicationData(complicationData);

            invalidate();
        }



        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            Log.d(TAG, "OnTapCommand()");
            // TODO: Step 5, OnTapCommand()
            switch (tapType) {
                case TAP_TYPE_TAP:
                    int tappedComplicationId = getTappedComplicationId(x, y);
                    if (tappedComplicationId != -1) {
                        onComplicationTap(tappedComplicationId);
                    }
                    break;
            }
        }

        private int getTappedComplicationId(int x, int y) {

            int complicationId;
            ComplicationData complicationData;
            ComplicationDrawable complicationDrawable;

            long currentTimeMillis = System.currentTimeMillis();

            for (int i = 0; i < COMPLICATION_IDS.length; i++) {
                complicationId = COMPLICATION_IDS[i];
                complicationData = mActiveComplicationDataSparseArray.get(complicationId);

                if ((complicationData != null)
                        && (complicationData.isActive(currentTimeMillis))
                        && (complicationData.getType() != ComplicationData.TYPE_NOT_CONFIGURED)
                        && (complicationData.getType() != ComplicationData.TYPE_EMPTY)) {

                    complicationDrawable = mComplicationDrawableSparseArray.get(complicationId);
                    Rect complicationBoundingRect = complicationDrawable.getBounds();

                    if (complicationBoundingRect.width() > 0) {
                        if (complicationBoundingRect.contains(x, y)) {
                            return complicationId;
                        }
                    } else {
                        Log.e(TAG, "Not a recognized complication id.");
                    }
                }
            }
            return -1;
        }

        // Fires PendingIntent associated with complication (if it has one).
        private void onComplicationTap(int complicationId) {
            // TODO: Step 5, onComplicationTap()
            Log.d(TAG, "onComplicationTap()");

            ComplicationData complicationData =
                    mActiveComplicationDataSparseArray.get(complicationId);

            if (complicationData != null) {

                if (complicationData.getTapAction() != null) {
                    try {
                        complicationData.getTapAction().send();
                    } catch (PendingIntent.CanceledException e) {
                        Log.e(TAG, "onComplicationTap() tap action error: " + e);
                    }

                } else if (complicationData.getType() == ComplicationData.TYPE_NO_PERMISSION) {

                    // Watch face does not have permission to receive complication data, so launch
                    // permission request.
                    ComponentName componentName = new ComponentName(
                            getApplicationContext(),
                            DarkSideWatchFaceService.class);

                    Intent permissionRequestIntent =
                            ComplicationHelperActivity.createPermissionRequestHelperIntent(
                                    getApplicationContext(), componentName);

                    startActivity(permissionRequestIntent);
                }

            } else {
                Log.d(TAG, "No PendingIntent for complication " + complicationId + ".");
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            mWidth = width;
            mHeight = height;
            /*
             * Find the coordinates of the center point on the screen.
             * Ignore the window insets so that, on round watches
             * with a "chin", the watch face is centered on the entire screen,
             * not just the usable portion.
             */
            mCenterX = mWidth / 2f;
            mCenterY = mHeight / 2f;

            mScale = ((float) width) / (float) mBackgroundBitmap.getWidth();

            mBackgroundBitmap = Bitmap.createScaledBitmap
                    (mBackgroundBitmap, (int)(mBackgroundBitmap.getWidth() * mScale),
                            (int)(mBackgroundBitmap.getHeight() * mScale), true);

            // TODO: Step 2, calculating ComplicationDrawable locations
            int sizeOfComplication = (int)(width / 4.4);
            int midpointOfScreen = width / 2;

            int upperRightHorizontalOffset = ((midpointOfScreen - sizeOfComplication) / 2) + (sizeOfComplication / 3);
            int upperLeftHorizontalOffset = ((midpointOfScreen - sizeOfComplication) / 2) - (sizeOfComplication / 3);
            int upperVerticalOffset = (midpointOfScreen - sizeOfComplication) - (sizeOfComplication / 7);

            int lowerRightHorizontalOffset = ((midpointOfScreen - sizeOfComplication) / 2) + (sizeOfComplication / 3);
            int lowerLeftHorizontalOffset = ((midpointOfScreen - sizeOfComplication) / 2) - (sizeOfComplication / 3);
            int lowerVerticalOffset = (midpointOfScreen) + (sizeOfComplication / 3);


            Rect upperLeftBounds =
                    // Left, Top, Right, Bottom
                    new Rect(
                            upperLeftHorizontalOffset,
                            upperVerticalOffset,
                            (upperLeftHorizontalOffset + sizeOfComplication),
                            (upperVerticalOffset + sizeOfComplication));

            ComplicationDrawable upperLeftComplicationDrawable =
                    mComplicationDrawableSparseArray.get(UPPER_LEFT_COMPLICATION_ID);
            upperLeftComplicationDrawable.setBounds(upperLeftBounds);

            Rect upperRightBounds =
                    // Left, Top, Right, Bottom
                    new Rect(
                            (midpointOfScreen + upperRightHorizontalOffset),
                            upperVerticalOffset,
                            (midpointOfScreen + upperRightHorizontalOffset + sizeOfComplication),
                            (upperVerticalOffset + sizeOfComplication));

            ComplicationDrawable upperRightComplicationDrawable =
                    mComplicationDrawableSparseArray.get(UPPER_RIGHT_COMPLICATION_ID);
            upperRightComplicationDrawable.setBounds(upperRightBounds);

            Rect lowerLeftBounds =
                    // Left, Top, Right, Bottom
                    new Rect(
                            lowerLeftHorizontalOffset,
                            lowerVerticalOffset,
                            (lowerLeftHorizontalOffset + sizeOfComplication),
                            (lowerVerticalOffset + sizeOfComplication));

            ComplicationDrawable lowerLeftComplicationDrawable =
                    mComplicationDrawableSparseArray.get(LOWER_LEFT_COMPLICATION_ID);
            lowerLeftComplicationDrawable.setBounds(lowerLeftBounds);

            Rect lowerRightBounds =
                    // Left, Top, Right, Bottom
                    new Rect(
                            (midpointOfScreen + lowerRightHorizontalOffset),
                            lowerVerticalOffset,
                            (midpointOfScreen + lowerRightHorizontalOffset + sizeOfComplication),
                            (lowerVerticalOffset + sizeOfComplication));

            ComplicationDrawable lowerRightComplicationDrawable =
                    mComplicationDrawableSparseArray.get(LOWER_RIGHT_COMPLICATION_ID);
            lowerRightComplicationDrawable.setBounds(lowerRightBounds);
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            canvas.drawBitmap(mBackgroundBitmap, 0, 0, mBackgroundPaint);

            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);

            int hour = mCalendar.get(Calendar.HOUR);
            if (hour == 0) {
                hour = 12;
            }
            String time = String.format("%d:%02d %s", (hour), mCalendar.get(Calendar.MINUTE),
                            ((mCalendar.get(Calendar.AM_PM) == 1) ? "PM": "AM"));
            canvas.drawText(time, mCenterX, mYOffset, mTextPaint);

            String ts = String.format("%d", mCalendar.getTimeInMillis() / 1000);
            canvas.drawText(ts, mCenterX, mTSYOffset, mTSPaint);

            String day = String.format("%s", mCalendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault()));
            canvas.drawText(day, mCenterX, mDayYOffset, mDayPaint);

            String date = String.format("%d/%d/%02d", mCalendar.get(Calendar.MONTH) + 1, mCalendar.get(Calendar.DAY_OF_MONTH), mCalendar.get(Calendar.YEAR) % 100);
            canvas.drawText(date, mCenterX, mDateYOffset, mDatePaint);

            drawComplications(canvas, now);

        }

        private void drawComplications(Canvas canvas, long currentTimeMillis) {
            // TODO: Step 4, drawComplications()
            int complicationId;
            ComplicationDrawable complicationDrawable;

            for (int i = 0; i < COMPLICATION_IDS.length; i++) {
                complicationId = COMPLICATION_IDS[i];
                complicationDrawable = mComplicationDrawableSparseArray.get(complicationId);

                complicationDrawable.draw(canvas, currentTimeMillis);
            }
        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }
    }
}
