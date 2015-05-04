/**
 *
 * Timer.java
 * @date 14-8-25 下午3:48
 * @author leo [leoyonn@gmail.com]
 * [CopyRight] All Rights Reserved.
 */

package me.lyso.mampa.timer;

import io.netty.util.Timeout;

import java.util.List;

/**
 * Interface of timer used by actor.
 *
 * @author leo
 */
public interface Timer {
    /**
     * Add a timeout event into timer.
     *
     * @param timeout
     * @throws java.lang.IllegalStateException if timer's queue if full.
     * @return
     */
    Timeout add(TimeoutEvent<?> timeout);

    /**
     * List all expired timeout event.
     *
     * @return
     */
    List<TimeoutEvent<?>> expired();

    /**
     * Get next nearest timeout-event's delay(in nanos).
     *
     * @return -1 means no events in timer.
     */
    long nextDelay();

    /**
     * Releases all resources acquired by this {@link Timer} and cancels all tasks which were scheduled but not executed yet.
     *
     * @return the handles associated with the tasks which were canceled by this method
     */
    List<Timeout> stop();
}