/**
 *
 * TimeoutEventHandler.java
 * @date 14-8-25 下午2:16
 * @author leo [leoyonn@gmail.com]
 * [CopyRight] All Rights Reserved.
 */

package me.lyso.mampa.timer;

import java.util.List;

/**
 * Handler to execute on timeout event expires.
 *
 * @author leo
 */
public interface TimeoutEventHandler {

    /**
     * Deal the event on #dealAfter 's delay expires.
     * NOTICE: only can be called in the Actor's wait-strategy.
     */
    void onExpiredEvents(List<TimeoutEvent<?>> events);
}
