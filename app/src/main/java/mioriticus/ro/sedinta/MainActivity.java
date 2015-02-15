package mioriticus.ro.sedinta;

import android.annotation.TargetApi;
import android.app.Activity;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.Chronometer;

import mioriticus.ro.sedinta.util.SystemUiHider;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class MainActivity extends Activity {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 2000;

    /**
     * If set, will toggle the system UI visibility upon interaction. Otherwise,
     * will show the system UI visibility upon interaction.
     */
    private static final boolean TOGGLE_ON_CLICK = true;

    /**
     * The flags to pass to {@link SystemUiHider#getInstance}.
     */
    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

    /**
     * The instance of the {@link SystemUiHider} for this activity.
     */
    private SystemUiHider mSystemUiHider;

    private Chronometer mChronometer;
    private View mMainFrame;
    private Button mButtonStart;
    private Button mButtonStop;
    private int mStep1Background;
    private int mStep2Background;
    private int mStep3Background;
    private int mEndBackground;
    private int mStep1Text;
    private int mStep2Text;
    private int mStep3Text;
    private int mEndText;
    private MediaPlayer mPlayerWarning;
    private MediaPlayer mPlayerEnd;
    private boolean mNotifiedStep1;
    private boolean mNotifiedStep2;
    private boolean mNotifiedStep3;
    private boolean mNotifiedEnd;

    private boolean mChronoRunning;
    private long mPausedMoment;

    private final Handler mHideHandler = new Handler();
    private Animation mAnim;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        final View controlsView = findViewById(R.id.fullscreen_content_controls);
        final View contentView = findViewById(R.id.fullscreen_content);

        mMainFrame = findViewById(R.id.main_frame);

        // Set up chronometer
        mChronometer = (Chronometer) findViewById(R.id.chronometer);
        mChronometer.setOnChronometerTickListener(mChronoTickListener);

        // Set up an instance of SystemUiHider to control the system UI for
        // this activity.
        mSystemUiHider = SystemUiHider.getInstance(this, contentView, HIDER_FLAGS);
        mSystemUiHider.setup();
        mSystemUiHider
                .setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
                    // Cached values.
                    int mControlsHeight;
                    int mShortAnimTime;

                    @Override
                    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
                    public void onVisibilityChange(boolean visible) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
                            // If the ViewPropertyAnimator API is available
                            // (Honeycomb MR2 and later), use it to animate the
                            // in-layout UI controls at the bottom of the
                            // screen.
                            if (mControlsHeight == 0) {
                                mControlsHeight = controlsView.getHeight();
                            }
                            if (mShortAnimTime == 0) {
                                mShortAnimTime = getResources().getInteger(
                                        android.R.integer.config_shortAnimTime);
                            }
                            controlsView.animate()
                                    .translationY(visible ? 0 : mControlsHeight)
                                    .setDuration(mShortAnimTime);
                        } else {
                            // If the ViewPropertyAnimator APIs aren't
                            // available, simply show or hide the in-layout UI
                            // controls.
                            controlsView.setVisibility(visible ? View.VISIBLE : View.GONE);
                        }

                        // if (visible && AUTO_HIDE) {
                        if (visible) {
                            // Schedule a hide().
                            delayedHide(AUTO_HIDE_DELAY_MILLIS);
                        }
                    }
                });

        // Set up the user interaction to manually show or hide the system UI.
        contentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TOGGLE_ON_CLICK) {
                    mSystemUiHider.toggle();
                } else {
                    mSystemUiHider.show();
                }
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        mButtonStart = (Button) findViewById(R.id.button_start);
        mButtonStart.setOnTouchListener(mDelayHideTouchListener);
        mButtonStart.setOnClickListener(mButtonStartClickListener);
        mButtonStop = (Button) findViewById(R.id.button_stop);
        mButtonStop.setOnTouchListener(mDelayHideTouchListener);
        mButtonStop.setOnClickListener(mButtonStopClickListener);

        mStep1Background = getResources().getColor(R.color.step_1_background);
        mStep2Background = getResources().getColor(R.color.step_2_background);
        mStep3Background = getResources().getColor(R.color.step_3_background);
        mEndBackground = getResources().getColor(R.color.end_background);
        mStep1Text = getResources().getColor(R.color.step_1_text);
        mStep2Text = getResources().getColor(R.color.step_2_text);
        mStep3Text = getResources().getColor(R.color.step_3_text);
        mEndText = getResources().getColor(R.color.end_text);

        mAnim = new AlphaAnimation(0.0f, 1.0f);
        mAnim.setDuration(700); //You can manage the blinking time with this parameter
        mAnim.setStartOffset(0);
        mAnim.setRepeatMode(Animation.REVERSE);
        mAnim.setRepeatCount(Animation.INFINITE);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }


    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            mSystemUiHider.hide();
        }
    };

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    private final Chronometer.OnChronometerTickListener mChronoTickListener = new Chronometer.OnChronometerTickListener() {
        @Override
        public void onChronometerTick(Chronometer chronometer) {
            long time = SystemClock.elapsedRealtime() - chronometer.getBase();

            // Check elapsed time
            long timeStep1 = 1000 * 60 * 45; // 45 min
            long timeStep2 = 1000 * 60 * 75; // 1h 15min
            long timeStep3 = 1000 * 60 * 85; // 1h 25min
            long timeEnd = 1000 * 60 * 90; // 1h 30min

            if (time >= timeEnd && !mNotifiedEnd) {
                mMainFrame.setBackgroundColor(mEndBackground);
                mChronometer.setTextColor(mEndText);
                mPlayerEnd.start();
                mNotifiedEnd = true;
            } else if (time >= timeStep3 && !mNotifiedStep3) {
                mMainFrame.setBackgroundColor(mStep3Background);
                mChronometer.setTextColor(mStep3Text);
                mPlayerWarning.start();
                mNotifiedStep3 = true;
            } else if (time >= timeStep2 && !mNotifiedStep2) {
                mMainFrame.setBackgroundColor(mStep2Background);
                mChronometer.setTextColor(mStep2Text);
                mPlayerWarning.start();
                mNotifiedStep2 = true;
            } else if (time >= timeStep1 && !mNotifiedStep1) {
                mMainFrame.setBackgroundColor(mStep1Background);
                mChronometer.setTextColor(mStep1Text);
                mPlayerWarning.start();
                mNotifiedStep1 = true;
            }
        }
    };

    private final View.OnClickListener mButtonStartClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!mChronoRunning) {
                // animation
                mChronometer.setAnimation(null);

                // chrono
                mChronometer.setBase(SystemClock.elapsedRealtime() + mPausedMoment);
                mChronometer.start();

                // ui
                mButtonStart.setText(R.string.lbl_pause);
                mButtonStop.setVisibility(View.VISIBLE);
            } else {
                // chrono
                mPausedMoment = mChronometer.getBase() - SystemClock.elapsedRealtime();
                mChronometer.stop();

                // ui
                mButtonStart.setText(R.string.lbl_start);
                mButtonStop.setVisibility(View.VISIBLE);

                // animation
                mChronometer.startAnimation(mAnim);
            }

            // toggle flag
            mChronoRunning = !mChronoRunning;
        }
    };

    private final View.OnClickListener mButtonStopClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // chrono
            mChronometer.stop();
            mPausedMoment = 0;
            mChronometer.setText("00:00");

            // ui
            mButtonStart.setText(R.string.lbl_start);
            mButtonStop.setVisibility(View.GONE);
            mMainFrame.setBackgroundColor(getResources().getColor(R.color.start_background));
            mChronometer.setTextColor(getResources().getColor(R.color.start_text));

            // toggle flag
            mChronoRunning = false;
        }
    };

    @Override
    protected void onResume() {
        super.onResume();

        mPlayerWarning = MediaPlayer.create(this, R.raw.alarm_warn);
        mPlayerEnd = MediaPlayer.create(this, R.raw.alarm_end);
    }

    @Override
    protected void onPause() {
        super.onPause();

        mPlayerWarning.release();
        mPlayerEnd.release();
        mPlayerWarning = null;
        mPlayerEnd = null;
    }
}
