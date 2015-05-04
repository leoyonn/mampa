/**
 *
 * ActorSplitter.java
 * @date 14-5-14 下午1:36
 * @author leo [leoyonn@gmail.com]
 * [CopyRight] All Rights Reserved.
 */

package me.lyso.mampa.actor.router;

import java.util.ArrayList;
import java.util.List;

/**
 * @author leo
 */
public class ActorSplitter {
    /**
     * Split fsmTargets into batch by actorTargets using router.
     * <B>actorTargets should not be null and same size with fsmTargets.</B>
     *
     * @param router
     * @param actorNum
     * @param actorTargets
     * @param fsmTargets
     * @param <K1>
     * @param <K2>
     * @return
     */
    public static <K1, K2> List<K2>[] split(IActorRouter<K1> router, int actorNum, List<K1> actorTargets, List<K2> fsmTargets) {
        List<K2>[] results = (List<K2>[]) new List<?>[actorNum];
        for (int i = 0; i < actorTargets.size(); i++) {
            int idx = router.route(actorTargets.get(i), actorNum);
            List<K2> r = results[idx];
            if (r == null) {
                results[idx] = r = new ArrayList<K2>();
            }
            r.add(fsmTargets.get(i));
        }
        return results;
    }
}
