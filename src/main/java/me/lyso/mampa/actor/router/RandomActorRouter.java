/**
 *
 * RandomActorRouter.java
 * @date 14-8-6 下午1:50
 * @author leo [leoyonn@gmail.com]
 * [CopyRight] All Rights Reserved.
 */

package me.lyso.mampa.actor.router;

import me.lyso.perf.Perf;

import java.util.Random;

/**
 * Actor router route to a random actor.
 *
 * @author leo
 */
public class RandomActorRouter<K> implements IActorRouter<K> {
    public static final RandomActorRouter<byte[]> bytesInstance = new RandomActorRouter<byte[]>();
    public static final RandomActorRouter<String> stringInstance = new RandomActorRouter<String>();
    public static final RandomActorRouter<Long> longInstance = new RandomActorRouter<Long>();
    private Random rand = new Random();

    public RandomActorRouter() {
        rand.setSeed(Perf.begin());
    }

    @Override
    public int route(K target, int actorNum) {
        return rand.nextInt(actorNum);
    }

    @Override
    public boolean matchAllActor(K target) {
        return false;
    }
}
