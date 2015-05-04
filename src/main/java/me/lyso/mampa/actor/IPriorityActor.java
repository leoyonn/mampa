/**
 * IActor.java
 * [CopyRight]
 * @author leo [leoyonn@gmail.com]
 * @date Jan 22, 2014 1:16:54 PM
 */
package me.lyso.mampa.actor;

import io.netty.util.Timeout;
import me.lyso.mampa.actor.router.IActorRouter;
import me.lyso.mampa.actor.router.IFsmRouter;
import me.lyso.mampa.event.IEventType;
import me.lyso.mampa.fsm.FSM;
import me.lyso.mampa.fsm.IFsmType;
import me.lyso.mampa.fsm.IStateType;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Interface of a multiple-priority real {@link Actor}(Inner an {@link ActorGroup}). <BR>
 * Difference with {@link IActorGroup} is, IActor route to a group of Actor, should pass actorTarget for
 * {@link IActorRouter}.
 *
 * @see {@link IActorGroup}
 * @param <K2> :Type of target for routing to an {@link FSM} using {@link IFsmRouter}.
 * @author leo
 */
public interface IPriorityActor<K2> {
    /**
     * Tell the multiple-priority Actor an event with Type #etype and Data edata asynchronously.
     *
     * @param priority
     * @param fsmType
     * @param fsmTarget
     * @param etype
     * @param edata
     * @return
     */
    boolean tell(int priority, IFsmType fsmType, K2 fsmTarget, IEventType etype, Object edata);

    /**
     * Tell the multiple-priority Actor an event with Type #etype and Data edata synchronously.
     * <p>
     * Sync means make sure that the Actor knows this event, but do not guarantee it's processed.
     *
     * @param priority
     * @param fsmType
     * @param fsmTarget
     * @param etype
     * @param edata
     * @return
     */
    boolean tellSync(int priority, IFsmType fsmType, K2 fsmTarget, IEventType etype, Object edata);

    /**
     * Tell the Actor an event with Type #etype and Data edata asynchronously to multiple fsm targets.
     * <B>If some targets don't exists in this Actor, just ignore.</B>
     *
     * @param priority
     * @param fsmType
     * @param fsmTargets
     * @param etype
     * @param edata
     * @return
     */
    boolean batchTell(int priority, IFsmType fsmType, List<K2> fsmTargets, IEventType etype, Object edata);

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
     * @return
     */
    Timeout dealAfter(IFsmType fsmType, K2 fsmTarget, IEventType etype, Object edata, long delay, TimeUnit timeUnit);

    /**
     * Construct a NextState tuple for state transition. Do not store the instance because implementations
     * may reuse an instance for optimizing.
     *
     * @param priority
     * @param stateType
     * @param delay
     * @param timeUnit
     * @return
     */
    NextState nextState(int priority, IStateType stateType, long delay, TimeUnit timeUnit);

    /**
     * Index of this actor in an {@link ActorGroup}.
     *
     * @return
     */
    int index();

    /**
     * Get the fsmSize of an fsmType in this actor.
     *
     * @param fsmType
     * @return
     */
    int fsmSize(IFsmType fsmType);

    /**
     * Get the the fsmSize of all fsmTypes in this actor.
     *
     * @return
     */
    int fsmSize();

    /**
     * Get a context of this actor with #name.
     *
     * @return
     */
    <C> C context(String name);
}
