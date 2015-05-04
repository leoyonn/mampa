/**
 * IActorRouter.java
 * [CopyRight]
 * @author leo [leoyonn@gmail.com]
 * @date Jan 22, 2014 1:19:48 PM
 */
package me.lyso.mampa.actor.router;

import me.lyso.mampa.actor.Actor;
import me.lyso.mampa.actor.ActorGroup;

/**
 * Interface for route an event to appropriate {@link Actor} by #target.
 *
 * @param <K> :Type of target for {@link IActorRouter}.
 * @author leo
 */
public interface IActorRouter<K> {

    /**
     * Route to {@link Actor} of index [0, actorNum) in an {@link ActorGroup} by #target
     *
     * @param target
     * @param actorNum
     * @return
     */
    public int route(K target, int actorNum);

    /**
     * Mark if this target means route to all {@link Actor}s in an {@link ActorGroup}.
     *
     * @param target
     * @return
     */
    public boolean matchAllActor(K target);
}
