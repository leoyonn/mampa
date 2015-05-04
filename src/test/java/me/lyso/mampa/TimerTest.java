/**
 *
 * TimerTest.java
 * @date 14-8-25 下午4:48
 * @author leo [leoyonn@gmail.com]
 * [CopyRight] All Rights Reserved.
 */

package me.lyso.mampa;

import me.lyso.mampa.TimeoutTest.EventType;
import me.lyso.mampa.TimeoutTest.FsmType;
import me.lyso.mampa.timer.QueueTimer;
import me.lyso.mampa.timer.TimeoutEvent;
import me.lyso.mampa.timer.Timer;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

/**
 * @author leo
 */
public class TimerTest {
    @Test
    public void test() throws InterruptedException {
        Timer timer = QueueTimer.createHeapTimer(1, 1000);
        timer.add(TimeoutEvent.create(FsmType.Default, "target", EventType.Hit, "..", 100, TimeUnit.MILLISECONDS));
        timer.add(TimeoutEvent.create(FsmType.Default, "target", EventType.Hit, "..", 200, TimeUnit.MILLISECONDS));
        timer.add(TimeoutEvent.create(FsmType.Default, "target", EventType.Hit, "..", 300, TimeUnit.MILLISECONDS));
        timer.add(TimeoutEvent.create(FsmType.Default, "target", EventType.Hit, "..", 400, TimeUnit.MILLISECONDS));
        Assert.assertEquals(100, timer.nextDelay() / 1000000L, 1.0);
        Assert.assertEquals(0, timer.expired().size());
        Thread.sleep(150);
        Assert.assertEquals(1, timer.expired().size());
        Assert.assertEquals(50, timer.nextDelay() / 1000000L, 1.0);
        Assert.assertEquals(3, timer.stop().size());
        timer.stop();
    }
}
