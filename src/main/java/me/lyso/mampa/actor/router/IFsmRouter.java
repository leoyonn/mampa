/**
 * IActorRouter.java
 * [CopyRight]
 * @author leo [leoyonn@gmail.com]
 * @date Jan 22, 2014 1:19:48 PM
 */
package me.lyso.mampa.actor.router;

import me.lyso.mampa.fsm.FSM;

/**
 * Interface for route an event to appropriate {@link FSM} by #target.
 * 
 * @param <K> :Type of target for {@link IFsmRouter}.
 * @author leo
 */
public interface IFsmRouter<K> {
    /**
     * Route to {@link FSM}.
     * 
     * @param target
     * @return
     */
    public Object fsmId(K target);

    /**
     * Mark if this target means route to all {@link FSM}s.
     * 
     * @param target
     * @return
     */
    public boolean matchAllFsm(K target);

    /**
     * Route to {@link FSM}.
     *
     * @return assigned target id
     */
    public K getTargetIdForTask();
}
