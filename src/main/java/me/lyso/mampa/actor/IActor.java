/**
 * IActor.java
 * [CopyRight]
 * @author leo [leoyonn@gmail.com]
 * @date Jan 22, 2014 1:16:54 PM
 */
package me.lyso.mampa.actor;

import me.lyso.mampa.actor.router.IActorRouter;
import me.lyso.mampa.actor.router.IFsmRouter;
import me.lyso.mampa.event.IEventType;
import me.lyso.mampa.fsm.FSM;
import me.lyso.mampa.fsm.IFsmType;
import me.lyso.mampa.fsm.IStateType;
import io.netty.util.Timeout;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Interface of a real {@link Actor}(Inner an {@link ActorGroup}). <BR>
 * Difference with {@link IActorGroup} is, IActor route to a group of Actor, should pass actorTarget for
 * {@link IActorRouter}.
 * 
 * @see {@link IActorGroup}
 * @param <K2> :Type of target for routing to an {@link FSM} using {@link IFsmRouter}.
 * @author leo
 */
public interface IActor<K2> extends IPriorityActor<K2> {
    /**
     * Tell the Actor an event with Type #etype and Data edata asynchronously.
     * 
     * @param fsmType
     * @param fsmTarget
     * @param etype
     * @param edata
     * @return
     */
    boolean tell(IFsmType fsmType, K2 fsmTarget, IEventType etype, Object edata);

    /**
     * Tell the Actor an event with Type #etype and Data edata synchronously.
     * <p/>
     * Sync means make sure that the Actor knows this event, but do not guarantee it's processed.
     *
     * @param fsmType
     * @param fsmTarget
     * @param etype
     * @param edata
     * @return
     */
    boolean tellSync(IFsmType fsmType, K2 fsmTarget, IEventType etype, Object edata);

    /**
     * Tell the Actor an event with Type #etype and Data edata asynchronously to multiple fsm targets.
     * <B>If some targets don't exists in this Actor, just ignore.</B>
     *
     * @param fsmType
     * @param fsmTarget
     * @param etype
     * @param edata
     * @return
     */
    boolean batchTell(IFsmType fsmType, List<K2> fsmTarget, IEventType etype, Object edata);

    /**
     * Ask the actor to process an event after specified intervals.
     * NOTICE: only can be called in the Actor's thread, such as rule-set.
     *
     * @param fsmType
     * @param fsmTarget
     * @param etype
     * @param edata
     * @param delay
     * @param timeUnit
     * @throws java.lang.IllegalStateException if timer's queue if full.
     * @return
     */
    Timeout dealAfter(IFsmType fsmType, K2 fsmTarget, IEventType etype, Object edata, long delay, TimeUnit timeUnit);

    /**
     * Construct a NextState tuple for state transition. Do not store the instance because implementations
     * may reuse an instance for optimizing.
     *
     * @param stateType
     * @param delay
     * @param timeUnit
     * @return
     */
    NextState nextState(IStateType stateType, long delay, TimeUnit timeUnit);
}
