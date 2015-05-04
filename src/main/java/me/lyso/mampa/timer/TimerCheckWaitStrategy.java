/**
 *
 * TimerCheckWaitStrategy.java
 * @date 14-8-25 下午2:14
 * @author leo [leoyonn@gmail.com]
 * [CopyRight] All Rights Reserved.
 */

package me.lyso.mampa.timer;

import com.lmax.disruptor.*;

import java.util.List;

/**
 * Wait strategy used for checking timer's timeout-events.
 * Timeout duration is bounded to {@value #MaxTimeoutNanos}.
 *
 * @author leo
 */
public class TimerCheckWaitStrategy extends MultiPriorityWaitStrategy implements WaitStrategy, SetTimeout {
    private static final long MaxTimeoutNanos = 100 * (long) 1e6; // bound: 100ms
    private final WaitStrategy original;
    private final TimeoutEventHandler handler;
    private final Timer timer;

    public TimerCheckWaitStrategy(TimeoutEventHandler handler, Timer timer, WaitStrategy original) {
        this.handler = handler;
        this.timer = timer;
        this.original = original;
    }

    @Override
    public long waitFor(long sequence, Sequence cursor, Sequence dependentSequence,
            SequenceBarrier barrier) throws AlertException, InterruptedException, TimeoutException {
        checkTimer();
        return original.waitFor(sequence, cursor, dependentSequence, barrier);
    }

    @Override
    public void waitForAll() throws InterruptedException {
        checkTimer();
        ((MultiPriorityWaitStrategy) original).waitForAll();
    }

    private void checkTimer() {
        List<TimeoutEvent<?>> expired = timer.expired();
        if (expired.size() > 0) {
            handler.onExpiredEvents(expired);
        }
        setTimeout(timer.nextDelay());
    }

    @Override
    public void signalAllWhenBlocking() {
        original.signalAllWhenBlocking();
    }

    public static TimerCheckWaitStrategy create(TimeoutEventHandler handler, Timer timer, WaitStrategyType waitStrategyType,
            boolean multiPriority, long millis) {
        WaitStrategy original = null;
        switch (waitStrategyType) {
            case Blocking: {
                original = multiPriority ? new BlockingMultiPriorityWaitStrategy() : new BoundedTimeoutBlockingWaitStrategy();
                break;
            }
            case Sleeping: {
                original = multiPriority ? new SleepingMultiPriorityWaitStrategy() : new SleepingWaitStrategy();
                break;
            }
            case Park: {
                original = multiPriority ? new ParkMultiPriorityWaitStrategy(millis) : new ParkWaitStrategy(millis);
                break;
            }
        }
        return new TimerCheckWaitStrategy(handler, timer, original);
    }

    @Override
    public void setTimeout(long timeoutInNanos) {
        if (original instanceof SetTimeout) {
            ((SetTimeout) original).setTimeout(timeoutInNanos <= 0 || timeoutInNanos > MaxTimeoutNanos ? MaxTimeoutNanos : timeoutInNanos);
        }
    }
}
