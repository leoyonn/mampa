package me.lyso.mampa.fsm;
/**
 * AbstractRuleSet.java
 * [CopyRight]
 * @author leo [leoyonn@gmail.com]
 * @date Jan 15, 2014 3:12:39 PM
 */


import me.lyso.mampa.actor.IPriorityActor;
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
public abstract class AbstractPriorityRuleSet<K2, V> {
    protected static int ruleSetId;
    protected final IFsmType fsmType;
    protected final int stateCount;
    protected final int etypeCount;
    protected final IPriorityAction<K2, V>[][] rules;
    protected final java.lang.Class ruleSetName;
    protected IEventType timeoutEvent;
    protected IEventType cancelEvent;
    protected IEventType inputEvent;
    protected IEventType resultEvent;
    protected IStateType stopState;

    public AbstractPriorityRuleSet(int stateCount, int etypeCount, IEventType timeoutEvent, IStateType stopState) {
        this(null, new IFsmType.IndexedFsmType(),stateCount, etypeCount, timeoutEvent, stopState);
    }

    public AbstractPriorityRuleSet(java.lang.Class ruleSetName, int stateCount, int etypeCount, IEventType timeoutEvent, IStateType stopState) {
        this(ruleSetName, new IFsmType.IndexedFsmType(),stateCount, etypeCount, timeoutEvent, stopState);
    }

    public AbstractPriorityRuleSet(java.lang.Class ruleSetName, IFsmType fsmType, int stateCount, int etypeCount, IEventType timeoutEvent, IStateType stopState) {
        this.fsmType = fsmType;
        this.timeoutEvent = timeoutEvent;
        this.stopState = stopState;
        this.ruleSetName = ruleSetName;
        if (stateCount <= 0 || etypeCount <= 0) {
            throw new IllegalArgumentException("state count and etype count should be positive!");
        }
        this.stateCount = stateCount;
        this.etypeCount = etypeCount;
        //noinspection unchecked
        this.rules = (IPriorityAction<K2, V>[][]) new IPriorityAction<?, ?>[stateCount + 1][etypeCount + 1];
        beforeBuildingRule();
        buildRules();
        IPriorityAction<K2, V> defaultRule = buildDefaultRule();
        if (defaultRule == null) {
            throw new IllegalArgumentException("Default action should not be null!");
        }
        rules[stateCount][etypeCount] = defaultRule;
    }

    /**
     * Get the a and s' of s x e.
     * <ul>
     * <li>Case 1: rule [s x e] exists, returns;
     * <li>Case 2: rule [s x all-events] exists, returns;
     * <li>Case 3: rule [all-states x e] exists, returns;
     * <li>Case 4: returns rule [all-states x all-events].
     * </ul>
     *
     * @param s
     * @param e
     * @return
     */
    public IPriorityAction<K2, V> handle(IStateType s, IEventType e) {
        checkSize(s);
        checkSize(e);
        IPriorityAction<K2, V> a = rules[s.ordinal()][e.ordinal()];
        if (a != null) {
            return a;
        } else if ((a = rules[s.ordinal()][etypeCount]) != null) {
            return a;
        } else if ((a = rules[stateCount][e.ordinal()]) != null) {
            return a;
        } else {
            return rules[stateCount][etypeCount];
        }
    }

    /**
     * Action before building any rule.
     *
     * @return
     */
    protected abstract void beforeBuildingRule();


    /**
     * Call {@link #addRule}, {@link #addTimeoutRule} to build all rules
     */
    protected abstract void buildRules();

    /**
     * Default action when no rules found for State(s) x Timeout(e).
     *
     * @return
     */
    protected abstract IPriorityAction<K2, V> buildDefaultRule();

    /**
     * State(s) x Event(e) -> Action(a), State(s_)<BR>
     * s_ is Action(a)'s result.
     *
     * @see {@link #handle(IStateType, IEventType)}
     * @param s
     * @param e
     * @param a
     * @return
     */
    protected AbstractPriorityRuleSet<K2, V> addRule(IStateType s, IEventType e, IPriorityAction<K2, V> a) {
        checkSize(s);
        checkSize(e);
        return innerAddRule(s, e, a);
    }

    private AbstractPriorityRuleSet<K2, V> innerAddRule(IStateType s, IEventType e, IPriorityAction<K2, V> a) {
        if (a == null) {
            throw new IllegalArgumentException("Action should not be null!");
        }
        int     stateIndex = s == null ? stateCount : s.ordinal(),
                etypeIndex = e == null ? etypeCount : e.ordinal();
        if (rules[stateIndex][etypeIndex] != null) {
            throw new IllegalArgumentException(String.format("Duplicate rule for %s x %s!", s, e));
        }
        rules[stateIndex][etypeIndex] = a;
        return this;
    }

    /**
     * Default rule for [State(s) x all-events] if rule for [State(s) x Event(e)] not found.
     *
     * @see {@link #handle(IStateType, IEventType)}
     * @param s
     * @param a
     * @return
     */
    protected AbstractPriorityRuleSet<K2, V> addRule(IStateType s, IPriorityAction<K2, V> a) {
        checkSize(s);
        return innerAddRule(s, null, a);
    }

    /**
     * Default rule for [all-states x Event(s)] if rule for [State(s) x Event(e)] not found.
     *
     * @see {@link #handle(IStateType, IEventType)}
     * @param e
     * @param a
     * @return
     */
    protected AbstractPriorityRuleSet<K2, V> addRule(IEventType e, IPriorityAction<K2, V> a) {
        checkSize(e);
        return innerAddRule(null, e, a);
    }

    /**
     * State(s) x Timeout(e) -> Action(a), State(s_)<BR>
     * s_ is Action(a)'s result.
     *
     * @see {@link #handle(IStateType, IEventType)}
     * @param s
     * @param a
     * @return
     */
    protected AbstractPriorityRuleSet<K2, V> addTimeoutRule(IStateType s, IPriorityAction<K2, V> a) {
        return addRule(s, getTimeoutEventType(), a);
    }

    protected NextState nextState(int priority, IPriorityActor<K2> master, IStateType stateType, long delay, TimeUnit timeUnit) {
        return master.nextState(priority, stateType, delay, timeUnit);
    }

    protected NextState nextState(int priority, IPriorityActor<K2> master, IStateType stateType) {
        return nextState(priority, master, stateType, 0, TimeUnit.SECONDS);
    }

    protected NextState stopState(int priority, IPriorityActor<K2> master) {
        return nextState(priority, master, stopState);
    }

    private void checkSize(IStateType s) {
        if (s == null || s.ordinal() >= stateCount) {
            throw new IllegalArgumentException("Invalid State (ordinal should be less than max-size): " + s);
        }
    }
    private void checkSize(IEventType e) {
        if (e == null || e.ordinal() >= etypeCount) {
            throw new IllegalArgumentException("Invalid EventType (ordinal should be less than max-size): " + e);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + fsmType + "|" + stateCount + "|" + etypeCount + ">";
    }

    public IEventType getTimeoutEventType() {
        return timeoutEvent;
    }
    public void setTimeoutEventType(IEventType timeoutEvent) {
        this.timeoutEvent = timeoutEvent;
    }

    public IEventType getCancelEventType() {
        return cancelEvent;
    }
    public void setCancelEventType(IEventType cancelEvent) {
        this.cancelEvent = cancelEvent;
    }

    public IEventType getInputEventType() {
        return inputEvent;
    }
    public void setInputEventType(IEventType inputEvent) {this.inputEvent = inputEvent;}

    public IEventType getResultEventType() {
        return resultEvent;
    }
    public void setResultEventType(IEventType resultEvent) {
        this.resultEvent = resultEvent;
    }

    public IStateType getStopState() {
        return stopState;
    }
    public void setStopState(IStateType stopState) {
        this.stopState = stopState;
    }

    public java.lang.Class getRuleSetName(){return ruleSetName;}

    public IFsmType getFsmType() {return fsmType;}
}
