/**
 * LongActorRouter.java
 * [CopyRight]
 * @author leo [leoyonn@gmail.com]
 * @date Jan 22, 2014 1:42:08 PM
 */

package me.lyso.mampa.actor.router;

/**
 * Default router using long mod #actor-number.
 *
 * @author leo
 */
public class LongActorRouter implements IActorRouter<Long> {
    private static final LongActorRouter instance = new LongActorRouter();

    @Override
    public int route(Long target, int actorNum) {
        return target == null ? 0 : (int) (target % actorNum);
    }

    @Override
    public boolean matchAllActor(Long target) {
        return false;
    }

    public static LongActorRouter instance() {
        return instance;
    }

}
