package me.lyso.mampa.actor;

import me.lyso.mampa.fsm.IStateType;

import java.util.concurrent.TimeUnit;

/**
 * A next state in FSM state transition.
 *
 * @author caofuxiang
 *         2014-02-25 10:50
 */
public class NextState {
    private int priority;
    private IStateType stateType;
    private long delay = 0;
    private TimeUnit timeUnit = TimeUnit.SECONDS;

    NextState() {
    }

    protected NextState set(IStateType stateType, long delay, TimeUnit timeUnit) {
        return set(0, stateType, delay, timeUnit);
    }

    protected NextState set(int priority, IStateType stateType, long delay, TimeUnit timeUnit) {
        this.priority = priority;
        this.stateType = stateType;
        this.delay = delay;
        this.timeUnit = timeUnit;
        return this;
    }

    public int priority() {
        return priority;
    }

    public IStateType getStateType() {
        return stateType;
    }

    public long getDelay() {
        return delay;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }
}
