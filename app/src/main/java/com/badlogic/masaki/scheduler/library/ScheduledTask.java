package com.badlogic.masaki.scheduler.library;

import android.app.Activity;

/**
 * An interface that represents a scheduled task and extends {@link Runnable}
 * Often used to run code in a different {@link Thread}
 * Created by shojimasaki on 2016/04/28.
 */
public interface ScheduledTask extends Runnable {
    /**
     * Used to the get task's state paused
     * Often used on {@link Activity#onPause()}
     */
    void pause();

    /**
     * Used to get the task's state running
     * Often used on {@link Activity#onResume()}
     */
    void resume();

    /**
     * Used to get the task's state destroyed
     * Often used on {@link Activity#onDestroy()}
     * Once this method is called, the task will be finished to run
     */
    void release();

    /**
     * Used to get the task's state cancelled
     */
    void cancel();
}
