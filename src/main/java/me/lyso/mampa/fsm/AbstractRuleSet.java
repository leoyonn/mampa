/**
 * AbstractRuleSet.java
 * [CopyRight]
 * @author leo [leoyonn@gmail.com]
 * @date Jan 15, 2014 3:12:39 PM
 */
package me.lyso.mampa.fsm;

import me.lyso.mampa.actor.IActor;
import me.lyso.mampa.actor.NextState;
import me.lyso.mampa.actor.router.IFsmRouter;
import me.lyso.mampa.event.IEventType;
import me.lyso.mampa.event.IPriorityAction;

import java.util.concurrent.TimeUnit;

/**
 * Rules definition of an {@line FSM}: State(S) x Event(E) -> Action(A), State(S').
 * <p/>
 * Usage: implements {@link #buildRules()} by calling {@link #addRule(IStateType, IEventType, IPriorityAction)}s
 *
 * @param <K2> :Type of target for routing to an {@link FSM} using {@link IFsmRouter}.
 * @param <V>  :Type of value in {@link State}.
 * @author leo
 */
public abstract class AbstractRuleSet<K2, V> extends AbstractPriorityRuleSet<K2, V> {

    public AbstractRuleSet(int stateCount, int etypeCount, IEventType timeoutEvent, IStateType stopState) {
        this(null, new IFsmType.IndexedFsmType(), stateCount, etypeCount, timeoutEvent, stopState);
    }

    public AbstractRuleSet(IFsmType fsmType, int stateCount, int etypeCount, IEventType timeoutEvent, IStateType stopState) {
        this(null, fsmType, stateCount, etypeCount, timeoutEvent, stopState);
    }

    public AbstractRuleSet(java.lang.Class ruleSetName, int stateCount, int etypeCount, IEventType timeoutEvent, IStateType stopState) {
        this(ruleSetName, new IFsmType.IndexedFsmType(), stateCount, etypeCount, timeoutEvent, stopState);
    }

    public AbstractRuleSet(java.lang.Class ruleSetName, IFsmType fsmType, int stateCount, int etypeCount, IEventType timeoutEvent, IStateType stopState) {
        super(ruleSetName, fsmType, stateCount, etypeCount, timeoutEvent, stopState);
    }

    /**
     * Initialize a FSM.
     *
     * @param state :The state to be initialized as FSM's original state.
     * @param fsmTarget
     * @param etype
     * @param data
     * @param master
     * @return whether to create a new fsm.
     */
    public abstract boolean init(State<V> state, K2 fsmTarget, IEventType etype, Object data, IActor<K2> master);

    /**
     * What to do when this fsm stops.
     *
     * @param state
     * @param master
     * @deprecated Please Override {@link #onStop(State, IActor, Throwable)}
     */
    public void onStop(State<V> state, IActor<K2> master) {
    }

    /**
     * What to do when this fsm stops.
     * <p/>
     * You can check #cause for reason that caused this Stop.
     * If this is a normal stop, #cause would be null.
     *
     * @param state
     * @param master
     * @param cause
     */
    public void onStop(State<V> state, IActor<K2> master, Throwable cause) {
        onStop(state, master);
    }

    @Override
    protected void beforeBuildingRule() {
    }

    protected NextState nextState(IActor<K2> master, IStateType stateType, long delay, TimeUnit timeUnit) {
        return master.nextState(stateType, delay, timeUnit);
    }

    protected NextState nextState(IActor<K2> master, IStateType stateType) {
        return nextState(master, stateType, 0, TimeUnit.SECONDS);
    }

    protected NextState stopState(IActor<K2> master) {
        return nextState(master, stopState);
    }

}
