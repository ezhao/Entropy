package com.herokuapp.ezhao.entropy;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.doomonafireball.betterpickers.numberpicker.NumberPickerBuilder;
import com.doomonafireball.betterpickers.numberpicker.NumberPickerDialogFragment;

import java.util.ArrayList;
import java.util.Random;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;


public class EntropyActivity extends ActionBarActivity implements NumberPickerDialogFragment.NumberPickerDialogHandler {
    private static final int START_RANGE_REFERENCE = 0;
    private static final int END_RANGE_REFERENCE = 1;

    private Random randomGenerator;
    private NumberPickerBuilder npb;
    private int startRange;
    private int endRange;
    private boolean historyMode = false;
    private boolean repeatMode = true;
    private ArrayList<Long> historyArray;

    @InjectView(R.id.tvRandomNumber) TextView tvRandomNumber;
    @InjectView(R.id.tvStartRange) TextView tvStartRange;
    @InjectView(R.id.tvEndRange) TextView tvEndRange;
    @InjectView(R.id.rlRandomHistory) RelativeLayout rlRandomHistory;
    @InjectView(R.id.rlRandomHistoryWrapper) RelativeLayout rlRandomHistoryWrapper;
    @InjectView(R.id.llHistoryList) LinearLayout llHistoryList;
    @InjectView(R.id.hsvHistoryList) HorizontalScrollView hsvHistoryList;
    @InjectView(R.id.cbRepeatSetting) CheckBox cbRepeatSetting;
    @InjectView(R.id.tvRepeatLabel) TextView tvRepeatLabel;
    TextView tvRepeatWarning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entropy);

        ButterKnife.inject(this);

        randomGenerator = new Random();

        npb = new NumberPickerBuilder()
                .setFragmentManager(getSupportFragmentManager())
                .setStyleResId(R.style.BetterPickersDialogFragment_Light)
                .setDecimalVisibility(View.INVISIBLE)
                .setMinNumber(Integer.MIN_VALUE)
                .setMaxNumber(Integer.MAX_VALUE);

        historyArray = new ArrayList<>();

        // Move horizontal view to end when views are added
        hsvHistoryList.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                hsvHistoryList.fullScroll(View.FOCUS_RIGHT);
            }
        });

        generateRandomNumber();

        tvRepeatWarning = (TextView) getLayoutInflater().inflate(R.layout.item_history_warning, null);
    }

    @OnClick(R.id.rlRandomNumber)
    public void nextRandomNumber(View view) {
        // Make sure there are numbers left to pick
        if (historyMode && !repeatMode && (endRange - startRange) < historyArray.size()) {
            //Toast.makeText(this, "All numbers picked! Reset history or enable repeats.", Toast.LENGTH_SHORT).show();
            if (tvRepeatWarning.getParent() == null) {
                llHistoryList.addView(tvRepeatWarning);
            }
            return;
        }

        long nextNumber = generateRandomNumber();

        // Add this newly generated number to the history
        if (historyMode) {
            TextView historyItem = (TextView) getLayoutInflater().inflate(R.layout.item_history, null);
            historyItem.setText(String.valueOf(nextNumber));
            llHistoryList.addView(historyItem);
        }
    }

    private long generateRandomNumber() {
        // Set the range
        try {
            startRange = Integer.parseInt(tvStartRange.getText().toString());
            endRange = Integer.parseInt(tvEndRange.getText().toString());
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a whole number", Toast.LENGTH_SHORT).show();
        }

        // Get the value
        int adjustment = 0;
        if (historyMode && !repeatMode) {
            adjustment = historyArray.size();
            if (endRange < startRange) {
                adjustment = -1 * adjustment;
            }
        }
        long nextNumber = Math.round((endRange - startRange - adjustment) * randomGenerator.nextDouble() + startRange);

        Log.d("EMILY", "Start: " + startRange + " end: " + endRange + " rawRand: " + nextNumber);

        while (historyMode && !repeatMode && historyArray.contains(nextNumber)) {
            if (endRange > startRange) {
                nextNumber++;
            } else {
                nextNumber--;
            }
        }

        // Remember the value
        if (historyMode) {
            historyArray.add(nextNumber);
        }

        // Display the value with animation
        final long animatorNextNumber = nextNumber;
        ObjectAnimator blink = ObjectAnimator.ofFloat(tvRandomNumber, "alpha", 0.1f);
        blink.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                tvRandomNumber.setText(String.valueOf(animatorNextNumber));
                ObjectAnimator blinkBack = ObjectAnimator.ofFloat(tvRandomNumber, "alpha", 1.0f);
                blinkBack.setDuration(100);
                blinkBack.start();
            }
        });
        blink.start();

        return nextNumber;
    }

    @OnClick(R.id.tvStartRange)
    public void setStartRange(View view) {
        setRange(view, START_RANGE_REFERENCE);
    }

    @OnClick(R.id.tvEndRange)
    public void setEndRange(View view) {
        setRange(view, END_RANGE_REFERENCE);
    }

    public void setRange(final View view, final int reference) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, "alpha", 0.1f);
        animator.setDuration(100);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                npb.setReference(reference);
                npb.show();
                view.setAlpha(1.0f);
            }
        });
        animator.start();
    }

    @OnClick(R.id.cbHistorySetting)
    public void changeHistorySetting(View view) {
        CheckBox cbHistorySetting = (CheckBox) view;
        if (cbHistorySetting.isChecked()) {
            historyMode = true;
            float translationY = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 120, getResources().getDisplayMetrics());
            ObjectAnimator showSection = ObjectAnimator.ofInt(rlRandomHistory, "minimumHeight", Math.round(translationY));
            showSection.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    rlRandomHistoryWrapper.setAlpha(0.0f);
                    rlRandomHistoryWrapper.setVisibility(View.VISIBLE);
                    ObjectAnimator.ofFloat(rlRandomHistoryWrapper, "alpha", 1.0f).start();
                }
            });
            showSection.setDuration(150);
            showSection.start();

            cbRepeatSetting.setEnabled(true);
            ObjectAnimator.ofFloat(tvRepeatLabel, "alpha", 1.0f).start();
        } else {
            historyMode = false;
            clearHistory();
            rlRandomHistoryWrapper.setVisibility(View.GONE);
            ObjectAnimator hideHistory = ObjectAnimator.ofInt(rlRandomHistory, "minimumHeight", 0);
            hideHistory.setDuration(150);
            hideHistory.start();

            cbRepeatSetting.setEnabled(false);
            cbRepeatSetting.setChecked(true);
            repeatMode = true;
            ObjectAnimator.ofFloat(tvRepeatLabel, "alpha", 0.33f).start();
        }
    }

    @OnClick(R.id.cbRepeatSetting)
    public void changeRepeatSetting(View view) {
        if (cbRepeatSetting.isChecked()) {
            repeatMode = true;
        } else {
            repeatMode = false;
        }
    }

    @OnClick(R.id.tvHistorySectionReset)
    public void tapHistoryReset(View view) {
        clearHistory();
    }

    private void clearHistory() {
        historyArray.clear();
        llHistoryList.removeAllViews();
    }

    @Override
    public void onDialogNumberSet(int reference, int number, double decimal, boolean isNegative, double fullNumber) {
        switch (reference) {
            case START_RANGE_REFERENCE:
                tvStartRange.setText(String.valueOf(number));
                return;
            case END_RANGE_REFERENCE:
                tvEndRange.setText(String.valueOf(number));
                return;
            default:
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_entropy, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
