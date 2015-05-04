/**
 * IAction.java
 * [CopyRight]
 * @author leo [leoyonn@gmail.com]
 * @date Jan 15, 2014 2:39:32 PM
 */
package me.lyso.mampa.event;

import me.lyso.mampa.actor.Actor;
import me.lyso.mampa.actor.IActor;
import me.lyso.mampa.actor.IPriorityActor;
import me.lyso.mampa.actor.NextState;
import me.lyso.mampa.actor.router.IFsmRouter;
import me.lyso.mampa.fsm.FSM;
import me.lyso.mampa.fsm.IFsmType;
import me.lyso.mampa.fsm.State;

/**
 * Action to execute when an {@link Actor} got an {@link Event} at some {@link State}.
 * Use the name 'IAction' for compatible reason.
 *
 * @param <K2> :Type of target for routing to an {@link FSM} using {@link IFsmRouter}.
 * @param <V>  :Type of value in {@link State}.
 * @author leo
 */
public abstract class IAction<K2, V> implements IPriorityAction<K2, V> {

    /**
     * Execute action according to {@link Event} #{eid|etype|data}.<BR>
     *
     * @param state     :Current state before executing this action
     * @param fsmType
     * @param fsmTarget
     * @param etype
     * @param data
     * @param master
     * @return :Next state after this action, should not be null.
     * @throws Exception
     */
    public abstract NextState exec(State<V> state, IFsmType fsmType, K2 fsmTarget, IEventType etype, Object data,
            IActor<K2> master) throws Exception;

    public final NextState exec(State<V> state, IFsmType fsmType, K2 fsmTarget, IEventType etype, Object data,
            IPriorityActor<K2> master) throws Exception {
        return exec(state, fsmType, fsmTarget, etype, data, (IActor<K2>) master);
    }
}
