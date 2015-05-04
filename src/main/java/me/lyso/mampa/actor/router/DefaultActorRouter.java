/**
 * DefaultActorRouter.java
 * [CopyRight]
 * @author leo [leoyonn@gmail.com]
 * @date Jan 22, 2014 1:42:08 PM
 */

package me.lyso.mampa.actor.router;

/**
 * Default router by hash of String target.
 *
 * @author leo
 */
public class DefaultActorRouter<K1> implements IActorRouter<K1> {
    public static final String AllTarget = "**";
    private static final DefaultActorRouter instance = new DefaultActorRouter();

    @Override
    public int route(K1 target, int actorNum) {
        if (target == null) {
            return 0;
        } else {
            return Math.abs(target.hashCode()) % actorNum;
        }
    }

    public static DefaultActorRouter<String> stringInstance() {
        //noinspection unchecked
        return instance;
    }

    @SuppressWarnings("unchecked")
    public static <K1> DefaultActorRouter<K1> instance() {
        return instance;
    }

    @Override
    public boolean matchAllActor(K1 target) {
        return AllTarget.equals(target);
    }
}
