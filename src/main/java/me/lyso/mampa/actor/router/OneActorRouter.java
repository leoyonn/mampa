/**
 * OneActorRouter.java
 * [CopyRight]
 * @author leo [leoyonn@gmail.com]
 * @date Feb 26, 2014 11:10:55 AM
 */
package me.lyso.mampa.actor.router;

/**
 * Actor router assumes that there is only one actor.
 *
 * @author leo
 */
public class OneActorRouter implements IActorRouter<String> {
    private static final OneActorRouter instance = new OneActorRouter();

    @Override
    public int route(String target, int actorNum) {
        return 0;
    }

    @Override
    public boolean matchAllActor(String target) {
        return false;
    }

    public static OneActorRouter instance() {
        return instance;
    }
}
