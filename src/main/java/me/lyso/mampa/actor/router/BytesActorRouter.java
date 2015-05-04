/**
 * BytesActorRouter.java
 * @date 14-6-25 下午8:48
 * @author leo [leoyonn@gmail.com]
 * [CopyRight] All Rights Reserved.
 */

package me.lyso.mampa.actor.router;

/**
 * Actor router using last 5 bytes as integer mod actor number.
 *
 * @author leo
 */
public class BytesActorRouter implements IActorRouter<byte[]> {
    private static final int DEFAULT_LEN_COUNTING = 5;
    public static final BytesActorRouter instance = new BytesActorRouter(DEFAULT_LEN_COUNTING);
    public static final BytesActorRouter fullInstance = new BytesActorRouter(-1);

    /**
     * how much bytes counting in this routing strategy, all if <= 0.
     */
    private final int lenCounting;

    public BytesActorRouter(int lenCounting) {
        this.lenCounting = lenCounting;
    }

    @Override
    public int route(byte[] target, int actorNum) {
        if (target == null || target.length == 0) {
            return 0;
        }
        int l = (lenCounting > 0 && target.length > lenCounting) ? lenCounting : target.length, h = 0;
        for (int i = 0; i < l; i++) {
            h = 31 * h + target[i];
        }
        return Math.abs(h) % actorNum;
    }

    @Override
    public boolean matchAllActor(byte[] target) {
        return false;
    }
}
