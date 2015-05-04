/**
 *
 * TimeoutEvent.java
 * @date 14-8-22 下午8:09
 * @author leo [leoyonn@gmail.com]
 * [CopyRight] All Rights Reserved.
 */

package me.lyso.mampa.timer;

import me.lyso.mampa.event.Event;
import me.lyso.mampa.event.IEventType;
import me.lyso.mampa.fsm.IFsmType;
import io.netty.util.*;
import io.netty.util.Timer;

import java.util.Comparator;
import java.util.concurrent.TimeUnit;

/**
 * A timeout event used in actor.
 *
 * @author leo
 */
public class TimeoutEvent<K2> extends Event<K2> implements Timeout, TimerTask, Comparable<TimeoutEvent<K2>> {
    private long deadline;
    private boolean cancel;

    public TimeoutEvent(IFsmType fsmType, K2 fsmTarget, IEventType type, Object data, long deadline) {
        super(fsmType, fsmTarget, type, data);
        this.deadline = deadline;
    }

    public long deadline() {
        return deadline;
    }

    public TimeoutEvent<K2> deadline(long deadline) {
        this.deadline = deadline;
        return this;
    }

    @Override
    public Timer timer() {
        return null;
    }

    @Override
    public TimerTask task() {
        return null;
    }

    @Override
    public boolean isExpired() {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return cancel;
    }

    @Override
    public boolean cancel() {
        this.cancel = true;
        return true;
    }

    @Override
    public void run(Timeout timeout) throws Exception {

    }

    @Override
    public void clear() {
        super.clear();
        cancel = false;
    }

    @Override
    public String toString() {
        return super.toString() + "|deadline:" + deadline + "|cancelled:" + cancel;
    }

    @Override
    public int compareTo(TimeoutEvent<K2> o) {
        return Long.valueOf(this.deadline).compareTo(o.deadline);
    }

    public static final Comparator<TimeoutEvent<?>> COMPARATOR = new Comparator<TimeoutEvent<?>>() {
        @Override
        public int compare(TimeoutEvent<?> o1, TimeoutEvent<?> o2) {
            //noinspection unchecked
            return ((TimeoutEvent<Object>) o1).compareTo((TimeoutEvent<Object>) o2);
        }
    };

    public static <K2> TimeoutEvent<K2> create(IFsmType fsmType, K2 fsmTarget, IEventType type, Object data, long delay, TimeUnit unit) {
        return new TimeoutEvent<K2>(fsmType, fsmTarget, type, data, System.nanoTime() + unit.toNanos(delay));
    }
}
