/**
 * IActor.java
 * [CopyRight]
 * @author leo [leoyonn@gmail.com]
 * @date Jan 22, 2014 1:16:54 PM
 */
package me.lyso.mampa.actor;

import me.lyso.mampa.actor.router.ActorSplitter;
import me.lyso.mampa.actor.router.IActorRouter;
import me.lyso.mampa.actor.router.IFsmRouter;
import me.lyso.mampa.event.IEventType;
import me.lyso.mampa.fsm.FSM;
import me.lyso.mampa.fsm.IFsmType;

import java.util.List;

/**
 * Interface of multiple-priority {@link ActorGroup}.
 *
 * @param <K1> :Type of target for routing to an {@link Actor} using {@link IActorRouter}.
 * @param <K2> :Type of target for routing to an {@link FSM} using {@link IFsmRouter}.
 * @author leo
 * @see IActor
 */
public interface IPriorityActorGroup<K1, K2> {
    /**
     * Tell the multiple-priority Actor an event with Type #etype and Data edata asynchronously.
     *
     * @param priority
     * @param actorTarget
     * @param fsmType
     * @param fsmTarget
     * @param etype
     * @param edata
     * @return
     */
    boolean tell(int priority, K1 actorTarget, IFsmType fsmType, K2 fsmTarget, IEventType etype, Object edata);

    /**
     * Tell the multiple-priority Actor an event with Type #etype and Data edata synchronously.
     * <p/>
     * Sync means make sure that the Actor knows this event, but do not guarantee it's processed.
     *
     * @param priority
     * @param actorTarget
     * @param fsmType
     * @param fsmTarget
     * @param etype
     * @param edata
     * @return
     */
    boolean tellSync(int priority, K1 actorTarget, IFsmType fsmType, K2 fsmTarget, IEventType etype, Object edata);

    /**
     * Tell the ActorGroup an event with Type #etype and Data edata asynchronously to multiple fsm targets.
     * Pair of a < actorTarget, fsmTarget > targeting one fsm.
     * 
     * <B>#fsmTargets will be split according to this ActorGroup's {@link IActorRouter}</B>
     * <B>If some targets don't exists in this Actor, just ignore.</B>
     *
     * @param priority
     * @param actorTargets
     * @param fsmType
     * @param fsmTarget
     * @param etype
     * @param edata
     * @return
     * @see ActorSplitter
     */
    boolean batchTell(int priority, List<K1> actorTargets, IFsmType fsmType, List<K2> fsmTarget, IEventType etype, Object edata);

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
}
