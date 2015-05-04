/**
 * QueueTimer.java
 * @date 14-8-22 下午2:38
 * @author leo [leoyonn@gmail.com]
 * [CopyRight] All Rights Reserved.
 */

package me.lyso.mampa.timer;

import me.lyso.primitive.Heap;
import me.lyso.primitive.RingQueue;
import io.netty.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;

/**
 * A simple non-thread-safe version of Timer for Actor,
 * which don't create a thread factory to run Worker but use Actor's thread.
 * NOTICE: if use RingQueue, all timeout should has same delay!
 *
 * @author leo
 */
public class QueueTimer implements Timer {
    private static enum Type {
        RingQueue, Heap,
    }

    private static final Logger logger = LoggerFactory.getLogger(QueueTimer.class);
    private static final long DEADLINE_ACCURACY = (long) 1;
    private final Queue<TimeoutEvent<?>> queue;
    private final long uniqTimeoutInNanos;
    private final Type type;
    private long lastDeadline;

    /**
     * Constructor.
     *
     * @param queue
     */
    private QueueTimer(Queue<TimeoutEvent<?>> queue, long timeoutInNanos, Type type) {
        this.queue = queue;
        this.uniqTimeoutInNanos = timeoutInNanos;
        this.lastDeadline = 0;
        this.type = type;
    }

    /**
     * Create a QueueTimer backed by RingQueue, for witch timeout should be constant.
     *
     * @param initSize
     * @param maxCapacity
     * @return
     */
    public static QueueTimer createRingQueueTimer(int initSize, int maxCapacity, long timeoutInNanos) {
        return new QueueTimer(new RingQueue<TimeoutEvent<?>>(initSize, maxCapacity), timeoutInNanos, Type.RingQueue);
    }

    /**
     * Create a QueueTimer backed by Heap.
     *
     * @param initSize
     * @param maxCapacity
     * @return
     */
    public static QueueTimer createHeapTimer(int initSize, int maxCapacity) {
        return new QueueTimer(Heap.create(initSize, maxCapacity, TimeoutEvent.COMPARATOR), -1, Type.Heap);
    }

    @Override
    public Timeout add(TimeoutEvent<?> timeout) {
        if (type == Type.RingQueue) {
            if (timeout.deadline() <= 0) {
                timeout.deadline(System.nanoTime() + uniqTimeoutInNanos);
            } else if (lastDeadline > timeout.deadline()) {
                throw new IllegalStateException("Timer use RingQueue should has constant timeout!");
            }
            lastDeadline = timeout.deadline();
        }
        if (queue.add(timeout)) {
            return timeout;
        }
        return null;
    }

    @Override
    public List<TimeoutEvent<?>> expired() {
        long now = System.nanoTime() + DEADLINE_ACCURACY;
        if (queue.isEmpty() || queue.peek().deadline() > now) {
            return Collections.emptyList();
        }
        List<TimeoutEvent<?>> expired = new ArrayList<TimeoutEvent<?>>();
        while (!queue.isEmpty() && queue.peek().deadline() <= now) {
            TimeoutEvent<?> e = queue.poll();
            if (!e.isCancelled()) {
                expired.add(e);
            }
        }
        return expired;
    }

    @Override
    public long nextDelay() {
        if (queue.isEmpty()) {
            return -1;
        }
        return queue.peek().deadline() - System.nanoTime();
    }

    @Override
    public List<Timeout> stop() {
        List<Timeout> timeouts = new ArrayList<Timeout>();
        while (!queue.isEmpty()) {
            timeouts.add(queue.poll());
        }
        return timeouts;
    }
}