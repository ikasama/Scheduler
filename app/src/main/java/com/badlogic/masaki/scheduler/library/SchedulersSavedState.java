package com.badlogic.masaki.scheduler.library;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Class that saves {@link Scheduler}'s instance state
 * Created by shojimasaki on 2016/04/28.
 */
public class SchedulersSavedState implements SavedState, Parcelable {
    /**
     * Used to save {@link Scheduler#mTaskStartedTime}
     */
    private long mTaskStartedTime;

    /**
     * Used to save {@link Scheduler#mElapsedRealTime}
     */
    private long mElapsedRealTime;

    /**
     * Used to save {@link Scheduler#mElapsedActiveTime}
     */
    private long mElapsedActiveTime;

    /**
     * Used to save {@link Scheduler#mFrameStartedTime}
     */
    private long mFrameStartedTime;

    /**
     * Used to save {@link Scheduler#mElapsedFrameTime}
     */
    private long mElapsedFrameTime;

    /**
     * Used to save {@link Scheduler#mCurrentFrameCount}
     */
    private int mCurrentFrameCount;

    /**
     * Getter
     * @return {@link SchedulersSavedState#mTaskStartedTime}
     */
    public long getTaskStartedTime() {
        return mTaskStartedTime;
    }

    /**
     * Setter
     * @param taskStartedTime
     */
    void setTaskStartedTime(long taskStartedTime) {
        mTaskStartedTime = taskStartedTime;
    }

    /**
     * Getter
     * @return {@link SchedulersSavedState#mElapsedRealTime}
     */
    public long getElapsedRealTime() {
        return mElapsedRealTime;
    }

    /**
     * Setter
     * @param elapsedRealTime
     */
    void setElapsedRealTime(long elapsedRealTime) {
        mElapsedRealTime = elapsedRealTime;
    }

    /**
     * Getter
     * @return {@link SchedulersSavedState#mElapsedActiveTime}
     */
    public long getElapsedActiveTime() {
        return mElapsedActiveTime;
    }

    /**
     * Setter
     * @param elapsedActiveTime
     */
    void setElapsedActiveTime(long elapsedActiveTime) {
        mElapsedActiveTime = elapsedActiveTime;
    }

    /**
     * Getter
     * @return {@link SchedulersSavedState#mFrameStartedTime}
     */
    public long getFrameStartedTime() {
        return mFrameStartedTime;
    }

    /**
     * Setter
     * @param frameStartedTime
     */
    void setFrameStartedTime(long frameStartedTime) {
        mFrameStartedTime = frameStartedTime;
    }

    /**
     * Getter
     * @return {@link SchedulersSavedState#mElapsedFrameTime}
     */
    public long getCurrentFrameTime() {
        return mElapsedFrameTime;
    }

    /**
     * Setter
     * @param currentFrameTime
     */
    void setCurrentFrameTime(long currentFrameTime) {
        mElapsedFrameTime = currentFrameTime;
    }

    /**
     * Getter
     * @return {@link SchedulersSavedState#mCurrentFrameCount}
     */
    public int getCurrentFrameCount() {
        return mCurrentFrameCount;
    }

    /**
     * Setter
     * @param currentFrameCount
     */
    void setCurrentFrameCount(int currentFrameCount) {
        mCurrentFrameCount = currentFrameCount;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mTaskStartedTime);
        dest.writeLong(mElapsedRealTime);
        dest.writeLong(mElapsedActiveTime);
        dest.writeLong(mFrameStartedTime);
        dest.writeLong(mElapsedFrameTime);
        dest.writeInt(mCurrentFrameCount);
    }

    public static final Parcelable.Creator<SchedulersSavedState> CREATOR =
            new Parcelable.Creator<SchedulersSavedState>() {
                @Override
                public SchedulersSavedState createFromParcel(Parcel source) {
                    return new SchedulersSavedState(source);
                }

                @Override
                public SchedulersSavedState[] newArray(int size) {
                    return new SchedulersSavedState[size];
                }
            };

    /**
     * Constructor that is package private to encapsulate its information
     */
    SchedulersSavedState() {

    }

    /**
     * Constructor for {@link #CREATOR}
     * @param in
     */
    private SchedulersSavedState(Parcel in) {
        mTaskStartedTime = in.readLong();
        mElapsedRealTime = in.readLong();
        mElapsedActiveTime = in.readLong();
        mFrameStartedTime = in.readLong();
        mElapsedFrameTime = in.readLong();
        mCurrentFrameCount = in.readInt();
    }
}
