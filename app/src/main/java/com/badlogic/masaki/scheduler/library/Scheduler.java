package com.badlogic.masaki.scheduler.library;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Abstract class that implements {@link ScheduledTask}
 * Subclasses override {@link #executeRegularTask()}, in which regular tasks will be executed
 * Created by shojimasaki on 2016/04/30.
 */
public abstract class Scheduler implements ScheduledTask , Handler.Callback {

    public static final String TAG = Scheduler.class.getSimpleName();

    /**
     * Used as flag to indicate that {@link #mTaskDuration} is the duration of real time
     */
    public static final int DURATION_REAL_TIME = 1;

    /**
     * Used as flag to indicate that {@link #mTaskDuration} is the duration of the device's active time
     */
    public static final int DURATION_ACTIVE_TIME = 1 << 1;

    /**
     * Used when {@link Handler#sendMessage(Message)} is called
     * Indicates that the task's regular task has been skipped
     */
    public static final int MSG_SKIP_FRAME = 1 << 2;

    /**
     * Used when {@link Handler#sendMessage(Message)} is called
     * Indicates that the task's duration has passed
     */
    public static final int MSG_TASK_COMPLETED = 1 << 3;

    /**
     * Used when {@link Handler#sendMessage(Message)} is called
     * Indicates that the task has been cancelled
     */
    public static final int MSG_TASK_CANCELED = 1 << 4;

    /**
     * Used as an argument of {@link #setTaskDuration(long)} (mTaskDuration)
     * Indicates that the task continues endlessly until destroyed
     */
    public static final long TASK_DURATION_INFINITE = 0;

    /**
     * Minimum interval for {@link #mInterval}
     */
    public static final long MINIMUM_INTERVAL = 1;

    /**
     * If a number equal to or less than this is set to {@link #setTaskDuration(long)},
     * IllegalArgumentException is thrown
     */
    public static final long INVALID_TIME = -1;

    /**
     * Used when {@link #sendMessage(int)} is called
     */
    private final Handler mHandler = new Handler(Looper.getMainLooper(), this);

    /**
     * Thread to which this class is bound
     */
    private Thread mBoundThread;

    /**
     * Callback used when a regular task is skipped
     */
    private OnSkipFrameListener mSkipFrameListener;

    /**
     * Callback used when the duration of the task has passed
     */
    private OnTaskCompletionListener mCompletionListener;

    /**
     * Callback used when the task is cancelled
     */
    private OnTaskCancelListener mCancelListener;

    /**
     * Represents the current state of the task
     */
    private State mCurrentState = State.IDLED;

    /**
     * Lock for {@link #mCurrentState}
     */
    private final ReentrantLock mStateLock = new ReentrantLock(false);

    /**
     * Condition used when {@link #mCurrentState} changes to paused from running, and vice versa
     */
    private final Condition mStateCondition = mStateLock.newCondition();

    /**
     * Interval of the periodic task
     */
    private long mInterval;

    /**
     * Saves the time the task has started
     */
    private long mTaskStartedTime;

    /**
     * Duration of the task
     */
    private long mTaskDuration = TASK_DURATION_INFINITE;

    /**
     * Elapsed time from the launch to the current
     */
    private long mElapsedRealTime;

    /**
     * Elapsed time the task was active from the launch to the current
     */
    private long mElapsedActiveTime;

    /**
     * Time when the run loop started
     */
    private long mFrameStartedTime;

    /**
     * Elapsed time from the start of the run loop
     */
    private long mElapsedFrameTime;

    /**
     * Last frame time in the run loop
     */
    private long mLastTime;

    /**
     * Current frame count in the run loop
     */
    private int mCurrentFrameCount;

    /**
     * Flags that a frame of the run loop is skipped when the regular task is delayed
     */
    private boolean mSkipFrameWhenDelayed = true;

    /**
     * Flags that the process in the run loop is paused while {@link #mCurrentState} is State.PAUSED
     */
    private boolean mStopProcessWhilePaused = true;

    /**
     * Task's duration type ({@link #DURATION_REAL_TIME} or {@link #DURATION_ACTIVE_TIME})
     */
    private int mDurationType = DURATION_REAL_TIME;

    /**
     * This class' tag
     */
    private Object mTag;

    /**
     * Flag whether the run loop process is in time or not
     */
    private boolean mProcessInTime;

    /**
     * Callback interface whose method is called when the task is completed
     */
    public interface OnTaskCompletionListener {
        /**
         * Called when the task is completed, and tells the main thread of the completion of the task
         *
         * @param tag {@link #mTag} set by {@link #setTag(int)} (Nullable), often used to identify this instance
         */
        void onScheduledTaskCompleted(@Nullable final Object tag);
    }

    /**
     * Callback interface whose method is called when a frame in the run loop is skipped
     */
    public interface OnSkipFrameListener {
        /**
         * Called when a frame in the run loop is skipped, and tells the main thread of the skip of the regular task
         *
         * @param tag {@link #mTag} set by {@link #setTag(int)} (Nullable), often used to identify this instance
         */
        void onSkipFrame(@Nullable final Object tag);
    }

    /**
     * Callback interface whose method is called when the task is cancelled
     */
    public interface OnTaskCancelListener {
        /**
         * Called when the task is cancelled, and tells the main thread of the cancellation of the task
         * @param tag
         */
        void onScheduledTaskCancelled(@Nullable final Object tag);
    }

    /**
     * Represents the state of the task
     */
    enum State {
        /**
         * State of idle
         */
        IDLED,

        /**
         * State of running
         */
        RUNNING,

        /**
         * State of pause
         */
        PAUSED,

        /**
         * State of being destroyed
         */
        DESTROYED,

        /**
         * State of being cancelled
         */
        CANCELLED,
    }


    /**
     * Constructor
     *
     * @param interval the interval of the regular task
     * @param durationType must be {@link #DURATION_REAL_TIME} or {@link #DURATION_ACTIVE_TIME}
     *
     * @see #DURATION_REAL_TIME
     * @see #DURATION_ACTIVE_TIME
     */
    public Scheduler(long interval, int durationType) {
        if (interval < MINIMUM_INTERVAL) {
            throw new IllegalArgumentException("interval < " + MINIMUM_INTERVAL  + " : " + interval);
        }

        if ((durationType != DURATION_REAL_TIME) && (durationType != DURATION_ACTIVE_TIME)) {
            throw new IllegalArgumentException("durationType must be Scheduler.DURATION_REAL_TIME " +
                    "or Scheduler.DURATION_ACTIVE_TIME");
        }

        mInterval = interval;
        mDurationType = durationType;
        mTaskStartedTime = mElapsedFrameTime = mFrameStartedTime = System.currentTimeMillis();
    }

    /**
     * Constructor
     *
     * @param interval the interval of the regular task
     * @param taskDuration the duration of the task (millis)
     * @param durationType must be set {@link #DURATION_REAL_TIME} or {@link #DURATION_ACTIVE_TIME}
     *
     * @see #DURATION_REAL_TIME
     * @see #DURATION_ACTIVE_TIME
     */
    public Scheduler(long interval, long taskDuration, int durationType) {
        this(interval, durationType);

        if (taskDuration <= INVALID_TIME) {
            throw new IllegalArgumentException("taskDuration < " + INVALID_TIME + " : " + taskDuration);
        }

        mTaskDuration = taskDuration;
    }

    /**
     * Constructor
     *
     * @param interval the interval of the regular task
     * @param taskDuration the duration of the task (millis)
     * @param durationType must be set {@link #DURATION_REAL_TIME} or {@link #DURATION_ACTIVE_TIME}
     * @param skipFrameWhenDelayed true if the regular task is skipped when delayed
     *
     * @see #DURATION_REAL_TIME
     * @see #DURATION_ACTIVE_TIME
     */
    public Scheduler(long interval, long taskDuration, int durationType, boolean skipFrameWhenDelayed) {
        this(interval, taskDuration, durationType);
        mSkipFrameWhenDelayed = skipFrameWhenDelayed;
    }

    /**
     * Sets interval to {@link Scheduler#mInterval}
     * @param interval interval of the regular task
     * @return Scheduler's instance
     */
    public Scheduler setInterval(long interval) {
        if (interval < MINIMUM_INTERVAL) {
            throw new IllegalArgumentException("interval < " + MINIMUM_INTERVAL  + " : " + interval);
        }

        mInterval = interval;
        return this;
    }

    /**
     * Sets duration to {@link Scheduler#mTaskDuration}
     * @param taskDuration duration of the task
     * @return Scheduler's instance
     */
    public Scheduler setTaskDuration(long taskDuration) {
        if (taskDuration <= INVALID_TIME) {
            throw new IllegalArgumentException("taskDuration < " + INVALID_TIME + " : " + taskDuration);
        }

        mTaskDuration = taskDuration;
        return this;
    }

    /**
     * Sets skipFrameWhenDelayed to {@link Scheduler#mSkipFrameWhenDelayed}
     * @param skipFrameWhenDelayed true to allow a frame of the run loop is skipped when delayed
     * @return Scheduler's instance
     */
    public Scheduler allowSkipFrameWhenDelayed(boolean skipFrameWhenDelayed) {
        mSkipFrameWhenDelayed = skipFrameWhenDelayed;
        return this;
    }

    /**
     * Sets stopProcessWhilePaused to {@link Scheduler#mStopProcessWhilePaused}
     * @param stopProcessWhilePaused true to allow the task is paused while {@link #mCurrentState} is State.PAUSED
     * @return Scheduler's instance
     */
    public Scheduler allowStopProcessWhilePaused(boolean stopProcessWhilePaused) {
        mStopProcessWhilePaused = stopProcessWhilePaused;
        return this;
    }

    /**
     * Sets the tag associated with this instance
     * @param tag tag associated with the instance
     * @return Scheduler's instance
     */
    public Scheduler setTag(final int tag) {
        mTag = tag;
        return this;
    }

    /**
     * Sets skipFrameListener to {@link Scheduler#mSkipFrameListener}
     * @param skipFrameListener callback that will run
     * @return Scheduler's instance
     */
    public Scheduler setOnSkipFrameListener(OnSkipFrameListener skipFrameListener) {
        mSkipFrameListener = skipFrameListener;
        return this;
    }

    /**
     * Sets completionListener to {@link Scheduler#mCompletionListener}
     * @param completionListener callback that will run
     * @return Scheduler's instance
     */
    public Scheduler setOnTaskCompletionListener(OnTaskCompletionListener completionListener) {
        mCompletionListener = completionListener;
        return this;
    }

    /**
     * Sets cancelListener to {@link Scheduler#mCancelListener}
     * @param cancelListener callback that will run
     * @return Scheduler's instance
     */
    public Scheduler setOnTaskCancelListener(OnTaskCancelListener cancelListener) {
        mCancelListener = cancelListener;
        return this;
    }

    @Override
    public void run() {
        mBoundThread = Thread.currentThread();
        mLastTime = System.currentTimeMillis();
        long deltaTime;

        executeRegularTask();

        /*
        the run loop continues while mCurrentState is not State.DESTROYED
         */
        while (isAvailable()) {
            mCurrentFrameCount++;

            if (getCurrentState() == State.CANCELLED) {
                sendMessage(MSG_TASK_CANCELED);
                return;
            }

            stopProcessIfPaused();

            final long currentTime = System.currentTimeMillis();
            deltaTime = currentTime - mLastTime;

            mElapsedActiveTime += deltaTime;
            mLastTime = currentTime;
            mElapsedRealTime = mLastTime - mTaskStartedTime;

            if (isTimeLeftLessThanInterval()) {
                sendMessage(MSG_TASK_COMPLETED);
                return;
            }

            /*
            calculates the threshold that indicates the current frame is in time
             */
            mElapsedFrameTime = currentTime - mFrameStartedTime;
            final long threshold = mInterval * mCurrentFrameCount;
            mProcessInTime = mElapsedFrameTime <= threshold;

            if (!mProcessInTime && mSkipFrameWhenDelayed) {
                sendMessage(MSG_SKIP_FRAME);
                continue;
            }

            /*
            adjusts the frame time to the scheduled time
             */
            sleep(threshold - mElapsedFrameTime);

            /*
            executes the regular task overridden by the subclass
             */
            executeRegularTask();
        }

    }

    /**
     * Calculates if the time left is less than the interval
     * @return true if the time left is less than the interval
     */
    private boolean isTimeLeftLessThanInterval() {
        /*
        in case of TASK_DURATION_INFINITE, no need to calculate
         */
        if (mTaskDuration == TASK_DURATION_INFINITE) {
            return false;
        }

        /*
        the time left differs according to the duration type
         */
        final long timeLeft;
        if (mDurationType == DURATION_REAL_TIME) {
            timeLeft = mTaskDuration - mElapsedRealTime;
        } else {
            timeLeft = mTaskDuration - mElapsedActiveTime;
        }

        if (timeLeft < mInterval) {
            if (timeLeft > 0) {

                /*
                sleep to adjust the time to finish
                 */
                sleep(timeLeft);
            }
            return true;
        }

        return false;
    }
    /**
     * Checks if this instance is still able to run
     * @return true if this instance is still able to run
     */
    public boolean isAvailable() {
        mStateLock.lock();
        try {
            return mCurrentState != State.DESTROYED;
        } finally {
            mStateLock.unlock();
        }
    }

    /**
     * Stops the task while {@link Scheduler#mCurrentState} is State.PAUSED
     */
    private void stopProcessIfPaused() {
        if (mStopProcessWhilePaused) {
            mStateLock.lock();
            try {
                while (mCurrentState == State.PAUSED) {
                    mStateCondition.await();
                    mLastTime = System.currentTimeMillis();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                mStateLock.unlock();
            }
        }
    }

    /**
     * Sends a message to the main thread via {@link #mHandler}
     * @param what integer that is set to {@link Message#what} when {@link #mHandler#sendMessage(int)} is called
     *
     * @see OnSkipFrameListener#onSkipFrame(Object)
     * @see OnTaskCompletionListener#onScheduledTaskCompleted(Object)
     */
    private void sendMessage(int what) {
        Message msg = Message.obtain();
        msg.what = what;
        mHandler.sendMessage(msg);
    }

    /**
     * Calls {@link Thread#sleep(long)}
     * @param millis sleep milliseconds
     */
    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Does the regular task within this method
     * Overridden by the subclasses
     */
    protected abstract void executeRegularTask();

    @Override
    public boolean handleMessage(Message msg) {
        /*
        calls callback methods according to msg.what
         */
        switch (msg.what) {
            case MSG_SKIP_FRAME :
                if (mSkipFrameListener != null) {
                    mSkipFrameListener.onSkipFrame(mTag);
                    return true;
                }
                return false;

            case MSG_TASK_COMPLETED :
                if (mCompletionListener != null) {
                    mCompletionListener.onScheduledTaskCompleted(mTag);
                    return true;
                }
                return false;

            case MSG_TASK_CANCELED :
                if (mCancelListener != null) {
                    mCancelListener.onScheduledTaskCancelled(mTag);
                    return true;
                }
                return false;

            default:
                return false;
        }
    }

    @Override
    public void pause() {
        mStateLock.lock();
        try {
            mCurrentState = State.PAUSED;
        } finally {
            mStateLock.unlock();
        }
    }

    @Override
    public void resume() {
        mStateLock.lock();
        try {
            mCurrentState = State.RUNNING;
            mStateCondition.signal();
        } finally {
            mStateLock.unlock();
        }
    }

    @Override
    public void release() {
        mStateLock.lock();
        try {
            mCurrentState = State.DESTROYED;
        } finally {
            mStateLock.unlock();
        }
    }

    @Override
    public void cancel() {
        mStateLock.lock();
        try {
            mCurrentState = State.CANCELLED;
        } finally {
            mStateLock.unlock();
        }
    }

    /**
     * Saves the current Scheduler's states related with time into {@link SchedulersSavedState}
     * @return created {@link SchedulersSavedState} instance in which the Scheduler's states was saved
     */
    public SchedulersSavedState saveInstanceState() {
        SchedulersSavedState savedState = new SchedulersSavedState();
        savedState.setTaskStartedTime(mTaskStartedTime);
        savedState.setElapsedRealTime(mElapsedRealTime);
        savedState.setElapsedActiveTime(mElapsedActiveTime);
        savedState.setFrameStartedTime(mFrameStartedTime);
        savedState.setCurrentFrameTime(mElapsedFrameTime);
        savedState.setCurrentFrameCount(mCurrentFrameCount);

        return savedState;
    }

    /**
     * Used to restore Scheduler's instance state, especially its private states via {@link SchedulersSavedState}
     * @param savedState {@link SchedulersSavedState} from which Scheduler's states is restored
     */
    public void restoreInstanceState(SchedulersSavedState savedState) {
        mTaskStartedTime = savedState.getTaskStartedTime();
        mElapsedRealTime = savedState.getElapsedRealTime();
        mElapsedActiveTime = savedState.getElapsedActiveTime();
        mFrameStartedTime = savedState.getFrameStartedTime();
        mElapsedFrameTime = savedState.getCurrentFrameTime();
        mCurrentFrameCount = savedState.getCurrentFrameCount();
    }

    /**
     * Getter
     * @return the thread this task is bound to
     */
    public Thread getBoundThread() {
        return mBoundThread;
    }

    /**
     * Getter
     * @return the current state of the task
     */
    public State getCurrentState() {
        mStateLock.lock();
        try {
            return mCurrentState;
        } finally {
            mStateLock.unlock();
        }
    }
}
