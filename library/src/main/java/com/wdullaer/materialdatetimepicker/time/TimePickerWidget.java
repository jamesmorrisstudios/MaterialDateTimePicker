package com.wdullaer.materialdatetimepicker.time;

import android.animation.ObjectAnimator;
import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wdullaer.materialdatetimepicker.HapticFeedbackController;
import com.wdullaer.materialdatetimepicker.R;
import com.wdullaer.materialdatetimepicker.TypefaceHelper;
import com.wdullaer.materialdatetimepicker.Utils;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Created by James on 8/1/2016.
 */

public class TimePickerWidget implements
    RadialPickerLayout.OnValueSelectedListener, TimePickerController {
    private static final String TAG = "TimePickerWidget";

    public static final int HOUR_INDEX = 0;
    public static final int MINUTE_INDEX = 1;
    public static final int SECOND_INDEX = 2;
    public static final int AM = 0;
    public static final int PM = 1;

    // Delay before starting the pulse animation, in ms.
    private static final int PULSE_ANIMATOR_DELAY = 300;

    private OnTimeChangedListener mCallback;

    private HapticFeedbackController mHapticFeedbackController;

    private TextView mHourView;
    private TextView mHourSpaceView;
    private TextView mMinuteView;
    private TextView mMinuteSpaceView;
    private TextView mSecondView;
    private TextView mSecondSpaceView;
    private TextView mAmPmTextView;
    private View mAmPmHitspace;
    private RadialPickerLayout mTimePicker;

    private int mSelectedColor;
    private int mUnselectedColor;
    private String mAmText;
    private String mPmText;

    private boolean mAllowAutoAdvance;
    private Timepoint mInitialTime;
    private boolean mIs24HourMode;
    private Utils.DateTimeTheme mTheme;
    private boolean mVibrate;
    private int mAccentColor = -1;
    private Timepoint[] mSelectableTimes;
    private Timepoint mMinTime;
    private Timepoint mMaxTime;
    private boolean mEnableSeconds;
    private boolean mEnableMinutes;

    // Accessibility strings.
    private String mHourPickerDescription;
    private String mSelectHours;
    private String mMinutePickerDescription;
    private String mSelectMinutes;
    private String mSecondPickerDescription;
    private String mSelectSeconds;

    /**
     * The callback interface used to indicate the user is done filling in
     * the time (they clicked on the 'Set' button).
     */
    public interface OnTimeChangedListener {

        void onHourChanged(int hourOfDay);

        void onMinutechanged(int minute);

        void onSecondChanged(int second);
    }

    private TimePickerWidget(@Nullable OnTimeChangedListener listener, int hourOfDay, int minute, int second, boolean is24HourMode, Utils.DateTimeTheme theme,
                             @ColorInt int accentColor, boolean enableVibrate, boolean enableSeconds, boolean enableMinutes) {
        mCallback = listener;
        mInitialTime = new Timepoint(hourOfDay, minute, second);
        mIs24HourMode = is24HourMode;
        mTheme = theme;
        mAccentColor = accentColor;
        mVibrate = enableVibrate;
        mEnableSeconds = enableSeconds;
        mEnableMinutes = enableMinutes;
        if (enableSeconds) mEnableMinutes = true;
    }

    public static Builder Builder() {
        return new Builder();
    }


    public static class Builder {
        private OnTimeChangedListener listener;
        private int hourOfDay = 0;
        private int minute = 0;
        private int second = 0;
        private boolean is24HourMode = false;
        private Utils.DateTimeTheme theme = Utils.DateTimeTheme.LIGHT;
        @ColorInt private int accentColor = Color.BLUE;
        private boolean enableVibrate = false;
        private boolean enableSeconds = false;
        private boolean enableMinutes = true;


        public Builder() {}

        @NonNull
        public Builder setListener(OnTimeChangedListener listener) {
            this.listener = listener;
            return this;
        }

        @NonNull
        public Builder setHourOfDay(int hourOfDay) {
            this.hourOfDay = hourOfDay;
            return this;
        }

        @NonNull
        public Builder setMinute(int minute) {
            this.minute = minute;
            return this;
        }

        @NonNull
        public Builder setSecond(int second) {
            this.second = second;
            return this;
        }

        @NonNull
        public Builder setIs24HourMode(boolean is24HourMode) {
            this.is24HourMode = is24HourMode;
            return this;
        }

        @NonNull
        public Builder setTheme(@NonNull final Utils.DateTimeTheme theme) {
            this.theme = theme;
            return this;
        }

        @NonNull
        public Builder setAccentColor(int accentColor) {
            this.accentColor = accentColor;
            return this;
        }

        @NonNull
        public Builder setEnableVibrate(boolean enableVibrate) {
            this.enableVibrate = enableVibrate;
            return this;
        }

        @NonNull
        public Builder setEnableSeconds(boolean enableSeconds) {
            this.enableSeconds = enableSeconds;
            return this;
        }

        @NonNull
        public Builder setEnableMinutes(boolean enableMinutes) {
            this.enableMinutes = enableMinutes;
            return this;
        }

        public final TimePickerWidget build() {
            return new TimePickerWidget(listener, hourOfDay, minute, second, is24HourMode, theme, accentColor, enableVibrate, enableSeconds, enableMinutes);
        }

    }

    @NonNull
    @Override
    public Utils.DateTimeTheme getDialogTheme() {
        return mTheme;
    }

    @Override
    public boolean is24HourMode() {
        return mIs24HourMode;
    }

    @Override
    public int getAccentColor() {
        return mAccentColor;
    }

    @SuppressWarnings("unused")
    public void setMinTime(int hour, int minute, int second) {
        setMinTime(new Timepoint(hour, minute, second));
    }

    public void setMinTime(Timepoint minTime) {
        if(mMaxTime != null && minTime.compareTo(mMaxTime) > 0)
            throw new IllegalArgumentException("Minimum time must be smaller than the maximum time");
        mMinTime = minTime;
    }

    @SuppressWarnings("unused")
    public void setMaxTime(int hour, int minute, int second) {
        setMaxTime(new Timepoint(hour, minute, second));
    }

    public void setMaxTime(Timepoint maxTime) {
        if(mMinTime != null && maxTime.compareTo(mMinTime) < 0)
            throw new IllegalArgumentException("Maximum time must be greater than the minimum time");
        mMaxTime = maxTime;
    }

    @SuppressWarnings("unused")
    public void setSelectableTimes(Timepoint[] selectableTimes) {
        mSelectableTimes = selectableTimes;
        Arrays.sort(mSelectableTimes);
    }

    /**
     * Set the interval for selectable times in the TimePickerDialog
     * This is a convenience wrapper around setSelectableTimes
     * The interval for all three time components can be set independently
     * @param hourInterval The interval between 2 selectable hours ([1,24])
     * @param minuteInterval The interval between 2 selectable minutes ([1,60])
     * @param secondInterval The interval between 2 selectable seconds ([1,60])
     */
    public void setTimeInterval(@IntRange(from=1, to=24) int hourInterval,
                                @IntRange(from=1, to=60) int minuteInterval,
                                @IntRange(from=1, to=60) int secondInterval) {
        List<Timepoint> timepoints = new ArrayList<>();

        int hour = 0;
        while (hour < 24) {
            int minute = 0;
            while (minute < 60) {
                int second = 0;
                while (second < 60) {
                    timepoints.add(new Timepoint(hour, minute, second));
                    second += secondInterval;
                }
                minute += minuteInterval;
            }
            hour += hourInterval;
        }
        setSelectableTimes(timepoints.toArray(new Timepoint[timepoints.size()]));
    }

    /**
     * Set the interval for selectable times in the TimePickerDialog
     * This is a convenience wrapper around setSelectableTimes
     * The interval for all three time components can be set independently
     * @param hourInterval The interval between 2 selectable hours ([1,24])
     * @param minuteInterval The interval between 2 selectable minutes ([1,60])
     */
    public void setTimeInterval(@IntRange(from=1, to=24) int hourInterval,
                                @IntRange(from=1, to=60) int minuteInterval) {
        setTimeInterval(hourInterval, minuteInterval, 1);
    }

    /**
     * Set the interval for selectable times in the TimePickerDialog
     * This is a convenience wrapper around setSelectableTimes
     * The interval for all three time components can be set independently
     * @param hourInterval The interval between 2 selectable hours ([1,24])
     */
    @SuppressWarnings("unused")
    public void setTimeInterval(@IntRange(from=1, to=24) int hourInterval) {
        setTimeInterval(hourInterval, 1);
    }

    public void setStartTime(int hourOfDay, int minute, int second) {
        mInitialTime = roundToNearest(new Timepoint(hourOfDay, minute, second));
    }

    @SuppressWarnings("unused")
    public void setStartTime(int hourOfDay, int minute) {
        setStartTime(hourOfDay, minute, 0);
    }

    public View build(Activity activity, ViewGroup container) {

        View view = LayoutInflater.from(activity).inflate(R.layout.mdtp_time_picker_dialog, container,false);

        // If an accent color has not been set manually, get it from the context
        if (mAccentColor == -1) {
            mAccentColor = Utils.getAccentColorFromThemeIfAvailable(activity);
        }

        Resources res = activity.getResources();
        Context context = activity;
        mHourPickerDescription = res.getString(R.string.mdtp_hour_picker_description);
        mSelectHours = res.getString(R.string.mdtp_select_hours);
        mMinutePickerDescription = res.getString(R.string.mdtp_minute_picker_description);
        mSelectMinutes = res.getString(R.string.mdtp_select_minutes);
        mSecondPickerDescription = res.getString(R.string.mdtp_second_picker_description);
        mSelectSeconds = res.getString(R.string.mdtp_select_seconds);
        mSelectedColor = ContextCompat.getColor(context, R.color.mdtp_white);
        mUnselectedColor = ContextCompat.getColor(context, R.color.mdtp_accent_color_focused);

        mHourView = (TextView) view.findViewById(R.id.hours);
        mHourSpaceView = (TextView) view.findViewById(R.id.hour_space);
        mMinuteSpaceView = (TextView) view.findViewById(R.id.minutes_space);
        mMinuteView = (TextView) view.findViewById(R.id.minutes);
        mSecondSpaceView = (TextView) view.findViewById(R.id.seconds_space);
        mSecondView = (TextView) view.findViewById(R.id.seconds);
        mAmPmTextView = (TextView) view.findViewById(R.id.ampm_label);
        String[] amPmTexts = new DateFormatSymbols().getAmPmStrings();
        mAmText = amPmTexts[0];
        mPmText = amPmTexts[1];

        mHapticFeedbackController = new HapticFeedbackController(activity);
        mInitialTime = roundToNearest(mInitialTime);

        mTimePicker = (RadialPickerLayout) view.findViewById(R.id.time_picker);
        mTimePicker.setOnValueSelectedListener(this);
        mTimePicker.initialize(activity, this, mInitialTime, mIs24HourMode);

        int currentItemShowing = HOUR_INDEX;
        setCurrentItemShowing(currentItemShowing, false, true, true);
        mTimePicker.invalidate();
        mHourView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setCurrentItemShowing(HOUR_INDEX, true, false, true);
                tryVibrate();
            }
        });
        mMinuteView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setCurrentItemShowing(MINUTE_INDEX, true, false, true);
                tryVibrate();
            }
        });
        mSecondView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setCurrentItemShowing(SECOND_INDEX, true, false, true);
                tryVibrate();
            }
        });

        /*
        mOkButton = (Button) view.findViewById(R.id.ok);
        mOkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mInKbMode && isTypedTimeFullyLegal()) {
                    finishKbMode(false);
                } else {
                    tryVibrate();
                }
                notifyOnDateListener();
                dismiss();
            }
        });
        mOkButton.setOnKeyListener(keyboardListener);
        mOkButton.setTypeface(TypefaceHelper.get(context, "Roboto-Medium"));
        if(mOkString != null) mOkButton.setText(mOkString);
        else mOkButton.setText(mOkResid);

        mCancelButton = (Button) view.findViewById(R.id.cancel);
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tryVibrate();
                if (getDialog() != null) getDialog().cancel();
            }
        });
        mCancelButton.setTypeface(TypefaceHelper.get(context, "Roboto-Medium"));
        if(mCancelString != null) mCancelButton.setText(mCancelString);
        else mCancelButton.setText(mCancelResid);
        mCancelButton.setVisibility(isCancelable() ? View.VISIBLE : View.GONE);
*/
        // Enable or disable the AM/PM view.
        mAmPmHitspace = view.findViewById(R.id.ampm_hitspace);
        if (mIs24HourMode) {
            mAmPmTextView.setVisibility(View.GONE);
        } else {
            mAmPmTextView.setVisibility(View.VISIBLE);
            updateAmPmDisplay(mInitialTime.isAM() ? AM : PM);
            mAmPmHitspace.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Don't do anything if either AM or PM are disabled
                    if (isAmDisabled() || isPmDisabled()) return;

                    tryVibrate();
                    int amOrPm = mTimePicker.getIsCurrentlyAmOrPm();
                    if (amOrPm == AM) {
                        amOrPm = PM;
                    } else if (amOrPm == PM) {
                        amOrPm = AM;
                    }
                    mTimePicker.setAmOrPm(amOrPm);
                }
            });
        }

        // Disable seconds picker
        if (!mEnableSeconds) {
            mSecondView.setVisibility(View.GONE);
            view.findViewById(R.id.separator_seconds).setVisibility(View.GONE);
        }

        // Disable minutes picker
        if (!mEnableMinutes) {
            mMinuteSpaceView.setVisibility(View.GONE);
            view.findViewById(R.id.separator).setVisibility(View.GONE);
        }

        // Center stuff depending on what's visible
        if (mIs24HourMode && !mEnableSeconds && mEnableMinutes) {
            // center first separator
            RelativeLayout.LayoutParams paramsSeparator = new RelativeLayout.LayoutParams(
                    ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT
            );
            paramsSeparator.addRule(RelativeLayout.CENTER_IN_PARENT);
            TextView separatorView = (TextView) view.findViewById(R.id.separator);
            separatorView.setLayoutParams(paramsSeparator);
        } else if (!mEnableMinutes && !mEnableSeconds) {
            // center the hour
            RelativeLayout.LayoutParams paramsHour = new RelativeLayout.LayoutParams(
                    ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT
            );
            paramsHour.addRule(RelativeLayout.CENTER_IN_PARENT);
            mHourSpaceView.setLayoutParams(paramsHour);

            if (!mIs24HourMode) {
                RelativeLayout.LayoutParams paramsAmPm = new RelativeLayout.LayoutParams(
                        ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT
                );
                paramsAmPm.addRule(RelativeLayout.RIGHT_OF, R.id.hour_space);
                paramsAmPm.addRule(RelativeLayout.ALIGN_BASELINE, R.id.hour_space);
                mAmPmTextView.setLayoutParams(paramsAmPm);
            }
        } else if (mEnableSeconds) {
            // link separator to minutes
            final View separator = view.findViewById(R.id.separator);
            RelativeLayout.LayoutParams paramsSeparator = new RelativeLayout.LayoutParams(
                    ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT
            );
            paramsSeparator.addRule(RelativeLayout.LEFT_OF, R.id.minutes_space);
            paramsSeparator.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
            separator.setLayoutParams(paramsSeparator);

            if (!mIs24HourMode) {
                // center minutes
                RelativeLayout.LayoutParams paramsMinutes = new RelativeLayout.LayoutParams(
                        ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT
                );
                paramsMinutes.addRule(RelativeLayout.CENTER_IN_PARENT);
                mMinuteSpaceView.setLayoutParams(paramsMinutes);
            } else {
                // move minutes to right of center
                RelativeLayout.LayoutParams paramsMinutes = new RelativeLayout.LayoutParams(
                        ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT
                );
                paramsMinutes.addRule(RelativeLayout.RIGHT_OF, R.id.center_view);
                mMinuteSpaceView.setLayoutParams(paramsMinutes);
            }
        }

        mAllowAutoAdvance = true;
        setHour(mInitialTime.getHour(), true);
        setMinute(mInitialTime.getMinute());
        setSecond(mInitialTime.getSecond());

        // Set the theme at the end so that the initialize()s above don't counteract the theme.
        view.findViewById(R.id.time_display_background).setBackgroundColor(mAccentColor);
        view.findViewById(R.id.time_display).setBackgroundColor(mAccentColor);

        int circleBackground = ContextCompat.getColor(context, R.color.mdtp_circle_background);
        int backgroundColor = ContextCompat.getColor(context, R.color.mdtp_background_color);

        int lightGray = ContextCompat.getColor(context, R.color.mdtp_light_gray);
        int darkBackgroundColor = ContextCompat.getColor(context, R.color.mdtp_light_gray);

        if(getDialogTheme() == Utils.DateTimeTheme.LIGHT) {
            mTimePicker.setBackgroundColor(circleBackground);
            view.findViewById(R.id.time_picker_dialog).setBackgroundColor(backgroundColor);
        } else if(getDialogTheme() == Utils.DateTimeTheme.DARK) {
            mTimePicker.setBackgroundColor(lightGray);
            view.findViewById(R.id.time_picker_dialog).setBackgroundColor(darkBackgroundColor);
        } else {
            mTimePicker.setBackgroundColor(Color.BLACK);
            view.findViewById(R.id.time_picker_dialog).setBackgroundColor(Color.BLACK);
        }
        return view;
    }

/*
    @Override
    public void onResume() {
        super.onResume();
        mHapticFeedbackController.start();
    }

    @Override
    public void onPause() {
        super.onPause();
        mHapticFeedbackController.stop();
        if(mDismissOnPause) dismiss();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        if(mOnCancelListener != null) mOnCancelListener.onCancel(dialog);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if(mOnDismissListener != null) mOnDismissListener.onDismiss(dialog);
    }
    */

    @Override
    public void tryVibrate() {
        if(mVibrate) mHapticFeedbackController.tryVibrate();
    }

    private void updateAmPmDisplay(int amOrPm) {
        if (amOrPm == AM) {
            mAmPmTextView.setText(mAmText);
            Utils.tryAccessibilityAnnounce(mTimePicker, mAmText);
            mAmPmHitspace.setContentDescription(mAmText);
        } else if (amOrPm == PM){
            mAmPmTextView.setText(mPmText);
            Utils.tryAccessibilityAnnounce(mTimePicker, mPmText);
            mAmPmHitspace.setContentDescription(mPmText);
        } else {
            mAmPmTextView.setText("");
        }
    }

    /**
     * Called by the picker for updating the header display.
     */
    @Override
    public void onValueSelected(Timepoint newValue) {
        setHour(newValue.getHour(), false);
        mTimePicker.setContentDescription(mHourPickerDescription + ": " + newValue.getHour());
        setMinute(newValue.getMinute());
        mTimePicker.setContentDescription(mMinutePickerDescription + ": " + newValue.getMinute());
        setSecond(newValue.getSecond());
        mTimePicker.setContentDescription(mSecondPickerDescription + ": " + newValue.getSecond());
        if(!mIs24HourMode) updateAmPmDisplay(newValue.isAM() ? AM : PM);
    }

    @Override
    public void advancePicker(int index) {
        if(!mAllowAutoAdvance) return;
        if(index == HOUR_INDEX && mEnableMinutes) {
            setCurrentItemShowing(MINUTE_INDEX, true, true, false);

            String announcement = mSelectHours + ". " + mTimePicker.getMinutes();
            Utils.tryAccessibilityAnnounce(mTimePicker, announcement);
        } else if(index == MINUTE_INDEX && mEnableSeconds) {
            setCurrentItemShowing(SECOND_INDEX, true, true, false);

            String announcement = mSelectMinutes+". " + mTimePicker.getSeconds();
            Utils.tryAccessibilityAnnounce(mTimePicker, announcement);
        }
    }

    @Override
    public void enablePicker() {

    }

    public boolean isOutOfRange(Timepoint current) {
        if(mMinTime != null && mMinTime.compareTo(current) > 0) return true;

        if(mMaxTime != null && mMaxTime.compareTo(current) < 0) return true;

        if(mSelectableTimes != null) return !Arrays.asList(mSelectableTimes).contains(current);

        return false;
    }

    @Override
    public boolean isOutOfRange(Timepoint current, int index) {
        if(current == null) return false;

        if(index == HOUR_INDEX) {
            if(mMinTime != null && mMinTime.getHour() > current.getHour()) return true;

            if(mMaxTime != null && mMaxTime.getHour()+1 <= current.getHour()) return true;

            if(mSelectableTimes != null) {
                for(Timepoint t : mSelectableTimes) {
                    if(t.getHour() == current.getHour()) return false;
                }
                return true;
            }

            return false;
        }
        else if(index == MINUTE_INDEX) {
            if(mMinTime != null) {
                Timepoint roundedMin = new Timepoint(mMinTime.getHour(), mMinTime.getMinute());
                if (roundedMin.compareTo(current) > 0) return true;
            }

            if(mMaxTime != null) {
                Timepoint roundedMax = new Timepoint(mMaxTime.getHour(), mMaxTime.getMinute(), 59);
                if (roundedMax.compareTo(current) < 0) return true;
            }

            if(mSelectableTimes != null) {
                for(Timepoint t : mSelectableTimes) {
                    if(t.getHour() == current.getHour() && t.getMinute() == current.getMinute()) return false;
                }
                return true;
            }

            return false;
        }
        else return isOutOfRange(current);
    }

    @Override
    public boolean isAmDisabled() {
        Timepoint midday = new Timepoint(12);

        if(mMinTime != null && mMinTime.compareTo(midday) > 0) return true;

        if(mSelectableTimes != null) {
            for(Timepoint t : mSelectableTimes) if(t.compareTo(midday) < 0) return false;
            return true;
        }

        return false;
    }

    @Override
    public boolean isPmDisabled() {
        Timepoint midday = new Timepoint(12);

        if(mMaxTime != null && mMaxTime.compareTo(midday) < 0) return true;

        if(mSelectableTimes != null) {
            for(Timepoint t : mSelectableTimes) if(t.compareTo(midday) >= 0) return false;
            return true;
        }

        return false;
    }

    /**
     * Round a given Timepoint to the nearest valid Timepoint
     * @param time Timepoint - The timepoint to round
     * @return Timepoint - The nearest valid Timepoint
     */
    private Timepoint roundToNearest(Timepoint time) {
        return roundToNearest(time, Timepoint.TYPE.HOUR);
    }

    @Override
    public Timepoint roundToNearest(Timepoint time, Timepoint.TYPE type) {

        if(mMinTime != null && mMinTime.compareTo(time) > 0) return mMinTime;

        if(mMaxTime != null && mMaxTime.compareTo(time) < 0) return mMaxTime;
        if(mSelectableTimes != null) {
            int currentDistance = Integer.MAX_VALUE;
            Timepoint output = time;
            for(Timepoint t : mSelectableTimes) {
                if(type == Timepoint.TYPE.MINUTE && t.getHour() != time.getHour()) continue;
                if(type == Timepoint.TYPE.SECOND && t.getHour() != time.getHour() && t.getMinute() != time.getMinute()) continue;
                int newDistance = Math.abs(t.compareTo(time));
                if(newDistance < currentDistance) {
                    currentDistance = newDistance;
                    output = t;
                }
                else break;
            }
            return output;
        }

        return time;
    }

    private void setHour(int value, boolean announce) {
        String format;
        if (mIs24HourMode) {
            format = "%02d";
        } else {
            format = "%d";
            value = value % 12;
            if (value == 0) {
                value = 12;
            }
        }

        CharSequence text = String.format(format, value);
        mHourView.setText(text);
        mHourSpaceView.setText(text);
        if (announce) {
            Utils.tryAccessibilityAnnounce(mTimePicker, text);
        }
    }

    private void setMinute(int value) {
        if (value == 60) {
            value = 0;
        }
        CharSequence text = String.format(Locale.getDefault(), "%02d", value);
        Utils.tryAccessibilityAnnounce(mTimePicker, text);
        mMinuteView.setText(text);
        mMinuteSpaceView.setText(text);
    }

    private void setSecond(int value) {
        if(value == 60) {
            value = 0;
        }
        CharSequence text = String.format(Locale.getDefault(), "%02d", value);
        Utils.tryAccessibilityAnnounce(mTimePicker, text);
        mSecondView.setText(text);
        mSecondSpaceView.setText(text);
    }

    // Show either Hours or Minutes.
    private void setCurrentItemShowing(int index, boolean animateCircle, boolean delayLabelAnimate,
                                       boolean announce) {
        mTimePicker.setCurrentItemShowing(index, animateCircle);

        TextView labelToAnimate;
        switch(index) {
            case HOUR_INDEX:
                int hours = mTimePicker.getHours();
                if (!mIs24HourMode) {
                    hours = hours % 12;
                }
                mTimePicker.setContentDescription(mHourPickerDescription + ": " + hours);
                if (announce) {
                    Utils.tryAccessibilityAnnounce(mTimePicker, mSelectHours);
                }
                labelToAnimate = mHourView;
                break;
            case MINUTE_INDEX:
                int minutes = mTimePicker.getMinutes();
                mTimePicker.setContentDescription(mMinutePickerDescription + ": " + minutes);
                if (announce) {
                    Utils.tryAccessibilityAnnounce(mTimePicker, mSelectMinutes);
                }
                labelToAnimate = mMinuteView;
                break;
            default:
                int seconds = mTimePicker.getSeconds();
                mTimePicker.setContentDescription(mSecondPickerDescription + ": " + seconds);
                if (announce) {
                    Utils.tryAccessibilityAnnounce(mTimePicker, mSelectSeconds);
                }
                labelToAnimate = mSecondView;
        }

        int hourColor = (index == HOUR_INDEX) ? mSelectedColor : mUnselectedColor;
        int minuteColor = (index == MINUTE_INDEX) ? mSelectedColor : mUnselectedColor;
        int secondColor = (index == SECOND_INDEX) ? mSelectedColor : mUnselectedColor;
        mHourView.setTextColor(hourColor);
        mMinuteView.setTextColor(minuteColor);
        mSecondView.setTextColor(secondColor);

        ObjectAnimator pulseAnimator = Utils.getPulseAnimator(labelToAnimate, 0.85f, 1.1f);
        if (delayLabelAnimate) {
            pulseAnimator.setStartDelay(PULSE_ANIMATOR_DELAY);
        }
        pulseAnimator.start();
    }

    /**
     * Simple node class to be used for traversal to check for legal times.
     * mLegalKeys represents the keys that can be typed to get to the node.
     * mChildren are the children that can be reached from this node.
     */
    private static class Node {
        private int[] mLegalKeys;
        private ArrayList<TimePickerWidget.Node> mChildren;

        public Node(int... legalKeys) {
            mLegalKeys = legalKeys;
            mChildren = new ArrayList<>();
        }

        public void addChild(TimePickerWidget.Node child) {
            mChildren.add(child);
        }

        public boolean containsKey(int key) {
            for (int legalKey : mLegalKeys) {
                if (legalKey == key) return true;
            }
            return false;
        }

        public TimePickerWidget.Node canReach(int key) {
            if (mChildren == null) {
                return null;
            }
            for (TimePickerWidget.Node child : mChildren) {
                if (child.containsKey(key)) {
                    return child;
                }
            }
            return null;
        }
    }

    public void notifyOnDateListener() {
        if (mCallback != null) {
            //mCallback.onTimeSet(mTimePicker, mTimePicker.getHours(), mTimePicker.getMinutes(), mTimePicker.getSeconds());
        }
    }




}
