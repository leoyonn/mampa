/**
 * IdActorRouter.java
 * [CopyRight]
 * @author leo [leoyonn@gmail.com]
 * @date Jan 22, 2014 8:51:11 PM
 */
package me.lyso.mampa.actor.router;

/**
 * Default router using long-type Id.
 * 
 * @author leo
 */
public class IdActorRouter implements IActorRouter<Long> {
    private static final IdActorRouter instance = new IdActorRouter();

    @Override
    public int route(Long target, int actorNum) {
        if (target == null) {
            return 0;
        }
        target = Math.abs(target);
        if (target < 10) {
            return (int) (target % actorNum);
        } else {
            return (int) (target / 10 % actorNum);
        }
    }

    @Override
    public boolean matchAllActor(Long target) {
        return false;
    }

    public static IdActorRouter instance() {
        return instance;
    }
}
