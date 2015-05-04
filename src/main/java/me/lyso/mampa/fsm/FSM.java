/**
 * FSM.java
 * [CopyRight]
 * @author leo [leoyonn@gmail.com]
 * @date Jan 14, 2014 5:16:58 PM
 */

package me.lyso.mampa.fsm;

import io.netty.util.Timeout;
import me.lyso.mampa.actor.IPriorityActor;
import me.lyso.mampa.actor.NextState;
import me.lyso.mampa.actor.router.IFsmRouter;
import me.lyso.mampa.event.IEventType;
import me.lyso.mampa.event.IPriorityAction;
import me.lyso.mampa.utils.PerfConstants;
import me.lyso.perf.Perf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Finite State Machine which do {@link #transition(IFsmType, IEventType, Object, IPriorityActor)} according to:
 * <p>
 * State(S) x Event(E) -> Action(A), State(S')
 * 
 * @param <K2>  :Type of target for routing to an {@link FSM} using {@link IFsmRouter}.
 * @param <V> :Type of value in {@link State}.
 * @author leo
 */
public class FSM<K2, V> {
    protected static final Logger LOGGER = LoggerFactory.getLogger(FSM.class);
    protected AbstractRuleSet<K2, V> ruleSet;
    protected State<V> state;
    private long born;
    private K2 target;

    public FSM() {
        this(null);
    }

    public FSM(AbstractRuleSet<K2, V> ruleSet) {
        this(ruleSet, new State<V>(null, null));
    }

    public FSM(AbstractRuleSet<K2, V> ruleSet, State<V> initState) {
        this.ruleSet = ruleSet;
        this.state = initState;
    }

    /**
     * <b>Do [s x e -> a + s'], s' comes from a's result. </b>
     *
     * @param fsmType
     * @param etype
     * @param edata
     * @param master
     */
    public Throwable transition(IFsmType fsmType, IEventType etype, Object edata, IPriorityActor<K2> master) {
        long begin = Perf.begin();
        // For timeout event, edata is the corresponding state type.
        if (etype.equals(ruleSet.getTimeoutEventType()) && !state.type().equals(edata)) {
            Perf.elapse(PerfConstants.PrefixTransitionTimeoutMismatch + fsmType + "~" + state.type() + ":" + edata, begin);
            LOGGER.debug("Current state is {}, ignore timeout event on {}.", state, edata);
            return null;
        }

        // Handle event.
        IPriorityAction<K2, V> action = ruleSet.handle(state.type(), etype);
        IStateType oldStateType = state.type();
        NextState nextState = null;
        try {
            //noinspection unchecked
            nextState = action.exec(state, fsmType, target, etype, edata, master);
        } catch (Throwable ex) {
            Perf.elapse(PerfConstants.PrefixTransitionFail, begin);
            perfTransitionFail(fsmType, oldStateType, etype, null, begin);
            state(ruleSet.getStopState(), false);
            LOGGER.error("Got exception while execute transition: {}", ex);
            return ex;
        }
        if (nextState == null || nextState.getStateType() == null) {
            perfTransitionFail(fsmType, oldStateType, etype, null, begin);
            state(ruleSet.getStopState(), false);
            LOGGER.error("Got null next-state while executing action [{} x {}]", oldStateType, etype);
            return new IllegalStateException("Invalid next state");
        }

        // For transition with timeout.
        long delay = nextState.getDelay();
        TimeUnit timeUnit = nextState.getTimeUnit();
        IStateType stateType = nextState.getStateType();
        LOGGER.debug("Transition from {} to {}.", state, stateType);
        boolean hasTimeout = delay > 0 && timeUnit != null;
        state(stateType, hasTimeout);
        if (hasTimeout) {
            Timeout timeout = master.dealAfter(fsmType, target, ruleSet.getTimeoutEventType(), stateType, delay, timeUnit);
            this.state.timeout(timeout);
        }
        perfTransitionOk(fsmType, oldStateType, etype, stateType, begin);
        return null;
    }

    /**
     * Set stateType of current state.
     *
     * @param stateType
     * @param hasTimeout
     * @return
     */
    protected FSM<K2, V> state(IStateType stateType, boolean hasTimeout) {
        if (state.type() != stateType || hasTimeout) {
            if (this.state.cancelTimeout()) {
                LOGGER.debug("Cancelled timeout for state {}.", state);
            }
        }
        this.state.type(stateType);
        return this;
    }

    /**
     * Set ruleSet of this fsm, used when borrowed from a pool.
     * 
     * @param ruleSet
     * @return
     */
    public FSM<K2, V> ruleSet(AbstractRuleSet<K2, V> ruleSet) {
        this.ruleSet = ruleSet;
        return this;
    }

    /**
     * Initialize this FSM with state #type and #value.
     *
     * @param type
     * @param value
     * @return
     */
    public FSM<K2, V> initState(IStateType type, V value) {
        this.state.type(type).value(value);
        return this;
    }

    /**
     * Get this FSM's life born time in ms.
     *
     * @return
     * @see #born(long)
     */
    public long born() {
        return born;
    }

    /**
     * Set this FSM's life born time in ms using {@link Perf#begin()} (not System#currentMilliseconds()).
     *
     * @param born
     * @return
     */
    public FSM<K2, V> born(long born) {
        this.born = born;
        return this;
    }

    /**
     * Current state of FSM
     *
     * @return
     */
    public State<V> state() {
        return state;
    }

    public void clear() {
        if (state != null) {
            state.clear();
        }
    }

    @Override
    public String toString() {
        return "FSM<" + state + ">";
    }

    public FSM<K2, V> target(K2 target) {
        this.target = target;
        return this;
    }

    public AbstractRuleSet<K2, V> getRuleSet() {return ruleSet;}

    public K2 target() {
        return target;
    }

    private static void perfTransitionOk(IFsmType fsmType, IStateType s, IEventType e, IStateType s_, long begin) {
        Perf.elapse(PerfConstants.PrefixTransitionOk + fsmType + "~" + s + ".X." + e + ".:." + s_, begin);
    }

    private static void perfTransitionFail(IFsmType fsmType, IStateType s, IEventType e, IStateType s_, long begin) {
        Perf.elapse(PerfConstants.PrefixTransitionFail + fsmType + "~" + s + ".X." + e + ".:." + s_, begin);
    }

}
