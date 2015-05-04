/**
 *
 * SplitterTest.java
 * @date 14-5-14 下午1:48
 * @author leo [leoyonn@gmail.com]
 * [CopyRight] All Rights Reserved.
 */

package me.lyso.mampa;

import me.lyso.mampa.actor.router.ActorSplitter;
import me.lyso.mampa.actor.router.LongActorRouter;
import me.lyso.mampa.demo.ActorDemoV4;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author leo
 */
public class BatchFsmTargetsTest {
    @Test
    public void testSplit() {
        List<Long> actorTargets = new ArrayList<Long>();
        List<String> fsmTargets = new ArrayList<String>();
        for (int i = 0; i < 500; i++) {
            String id = String.valueOf(i);
            String _id = String.valueOf(1000 - i);
            actorTargets.add(Long.valueOf(i));
            fsmTargets.add(String.valueOf(i));
            actorTargets.add(Long.valueOf(1000 - i));
            fsmTargets.add(String.valueOf(1000 - i));
        }
        List<String>[] results = ActorSplitter.split(new LongActorRouter(), 10, actorTargets, fsmTargets);
        for (int i = 0; i < results.length; i++) {
            List<String> r = results[i];
            Assert.assertEquals(r.size(), 100);
            for (String s : r) {
                Assert.assertEquals(s.charAt(s.length() - 1) - '0', i);
            }
        }
    }

    @Test
    public void testBatchFsm() throws InterruptedException {
        new ActorDemoV4().runBatchFsm();
    }
}
