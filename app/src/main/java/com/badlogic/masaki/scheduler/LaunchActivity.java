package com.badlogic.masaki.scheduler;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.badlogic.masaki.scheduler.library.SchedulersSavedState;
import com.badlogic.masaki.scheduler.library.Scheduler;

import java.util.Calendar;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by shojimasaki on 2016/04/24.
 */
public class LaunchActivity extends AppCompatActivity implements Scheduler.OnSkipFrameListener,
        Scheduler.OnTaskCompletionListener {

    public static final String TAG = LaunchActivity.class.getSimpleName();

    private static final int TAG_SCHEDULER = 1234;
    private static final String KEY_TASK_COMPLETED = "task_completed";
    private static final String KEY_SCHEDULERS_STATE = "schedulers_state";

    private boolean mScheduledTaskCompleted = false;

    private TextView mTextView;
    private Button mButton;

    private Executor mExecutor;
    private Scheduler mScheduler;
    private SchedulersSavedState mSchedulersSavedState;

    private Handler mHandler;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        initViews();

        if (savedInstanceState != null) {
            mScheduledTaskCompleted = savedInstanceState.getBoolean(KEY_TASK_COMPLETED);
            mSchedulersSavedState = savedInstanceState.getParcelable(KEY_SCHEDULERS_STATE);

            mButton.setVisibility(mScheduledTaskCompleted ? View.VISIBLE : View.GONE);
        }

        if (!mScheduledTaskCompleted) {
            executeScheduledTask();
        } else {
            mTextView.setText("task completed");
        }
    }

    private void initViews() {
        mTextView = (TextView) findViewById(R.id.textView);
        mButton = (Button) findViewById(R.id.button);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LaunchActivity.this, MainActivity.class));
            }
        });
    }

    private void executeScheduledTask() {
        mHandler = new Handler();
        mExecutor = Executors.newFixedThreadPool(1);
        mScheduler = new Scheduler(700, 1000 * 10, Scheduler.DURATION_ACTIVE_TIME) {
            @Override
            protected void executeRegularTask() {
                Calendar calendar = Calendar.getInstance();
                final String obj = calendar.getTime().toString();
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mTextView.setText(obj);
                    }
                });
            }
        };
        mScheduler .allowSkipFrameWhenDelayed(true)
                .allowStopProcessWhilePaused(true)
                .setTag(TAG_SCHEDULER)
                .setOnSkipFrameListener(this)
                .setOnTaskCompletionListener(this);

        if (mSchedulersSavedState != null) {
            mScheduler.restoreInstanceState(mSchedulersSavedState);
        }

        mExecutor.execute(mScheduler);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mScheduler != null) {
            mScheduler.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(mScheduler != null) {
            mScheduler.resume();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mScheduler != null) {
            mScheduler.release();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(KEY_TASK_COMPLETED, mScheduledTaskCompleted);
        if (mScheduler != null) {
            mSchedulersSavedState = mScheduler.saveInstanceState();
            outState.putParcelable(KEY_SCHEDULERS_STATE, mSchedulersSavedState);
        }
    }

    @Override
    public void onSkipFrame(@Nullable final Object tag) {
        Log.d(TAG, "onSkipFrame called");
    }

    @Override
    public void onScheduledTaskCompleted(@Nullable final Object tag) {
        Log.d(TAG, "onScheduledTaskCompleted called");

        final Integer result = (Integer) tag;

        switch (result) {
            case TAG_SCHEDULER :
                Toast.makeText(this, "TaskCompleted", Toast.LENGTH_SHORT).show();
                mTextView.setText("task completed");
                mButton.setVisibility(View.VISIBLE);
                mScheduledTaskCompleted = true;

                mExecutor = null;
                mHandler = null;
                mScheduler = null;
                mSchedulersSavedState = null;
                break;

            default:
                break;
        }
    }
}
