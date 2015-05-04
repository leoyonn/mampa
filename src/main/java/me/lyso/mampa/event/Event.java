/**
 * Event.java
 * [CopyRight]
 * @author leo [leoyonn@gmail.com]
 * @date Jan 14, 2014 5:16:52 PM
 */
package me.lyso.mampa.event;

import me.lyso.mampa.actor.Actor;
import me.lyso.mampa.actor.router.IActorRouter;
import me.lyso.mampa.fsm.IFsmType;
import me.lyso.perf.Perf;

import java.util.List;

/**
 * Event to send to an {@link Actor}.<BR>
 *
 * @param <K2> :Type of target for {@link IActorRouter}.
 * @author leo
 */
public class Event<K2> {
    private IFsmType fsmType;
    private K2 fsmTarget;
    private List<K2> batchTargets;
    private IEventType type;
    private Object data;
    private long born;
    private int priority;

    public static enum BatchType {
        All, Batch, One,
    }


    /**
     * Constructor.
     */
    public Event() {
    }

    /**
     * Constructor.
     *
     * @param fsmType
     * @param fsmTarget
     * @param type
     * @param data
     */
    public Event(IFsmType fsmType, K2 fsmTarget, IEventType type, Object data) {
        set(fsmType, fsmTarget, type, data, Perf.begin(), 0);
    }

    public K2 fsmTarget() {
        return fsmTarget;
    }

    public List<K2> batchTargets() {
        return batchTargets;
    }

    public boolean isBatch() {
        return batchTargets != null && batchTargets.size() > 0;
    }

    public Event<K2> fsmTarget(K2 target) {
        this.fsmTarget = target;
        return this;
    }

    public IFsmType fsmType () {
        return fsmType;
    }

    public Event<K2> fsmType(IFsmType fsmType) {
        this.fsmType = fsmType;
        return this;
    }

    public IEventType type() {
        return type;
    }

    public Event<K2> type(IEventType type) {
        this.type = type;
        return this;
    }

    public Object data() {
        return data;
    }

    public Event<K2> data(Object data) {
        this.data = data;
        return this;
    }

    /**
     * Shallow copy.
     *
     * @param that
     * @return
     */
    public Event<K2> copy(Event<K2> that) {
        set(that.fsmType, that.fsmTarget, that.type, that.data, that.born, this.priority);
        batchTargets = that.batchTargets;
        return this;
    }

    /**
     * Set values.
     *
     * @param fsmType
     * @param fsmTarget
     * @param type
     * @param data
     * @return
     */
    public Event<K2> set(IFsmType fsmType, K2 fsmTarget, IEventType type, Object data, long born, int priority) {
        this.fsmType = fsmType;
        this.fsmTarget = fsmTarget;
        this.batchTargets = null;
        this.type = type;
        this.data = data;
        this.born = born;
        this.priority = priority;
        return this;
    }

    /**
     * Set values.
     *
     * @param fsmType
     * @param batchTargets
     * @param type
     * @param data
     * @param born
     * @return
     */
    public Event<K2> set(IFsmType fsmType, List<K2> batchTargets, IEventType type, Object data, long born, int priority) {
        this.fsmType = fsmType;
        this.fsmTarget = null;
        this.batchTargets = batchTargets;
        this.type = type;
        this.data = data;
        this.born = born;
        this.priority = priority;
        return this;
    }

    public void clear() {
        this.fsmTarget = null;
        this.batchTargets = null;
        this.type = null;
        this.data = null;
        this.born = 0;
        this.priority = 0;
    }

    /**
     * Get this Event's life born time in ms.
     *
     * @return
     * @see #born(long)
     */
    public long born() {
        return born;
    }

    /**
     * Set this Event's life born time in ms using {@link Perf#begin()} (not System#currentMilliseconds()).
     *
     * @param born
     * @return
     */
    public Event<K2> born(long born) {
        this.born = born;
        return this;
    }

    public int priority() {
        return priority;
    }

    @Override
    public String toString() {
        return "Event." + type + "#" + (isBatch() ? batchTargets : fsmTarget) + "|" + "{" + data + "}";
    }

    /**
     * Factory to build Event.
     * <p>
     * Used by Disruptor's constructor.
     * 
     * @param <K2>
     * @author leo
     */
    public static class EventFactory<K2> implements com.lmax.disruptor.EventFactory<Event<K2>> {
        @Override
        public Event<K2> newInstance() {
            return new Event<K2>();
        }
    }
}
