/**
 *
 * ActorsTest.java
 * @date 14-11-26 下午7:44
 * @author leo [leoyonn@gmail.com]
 * [CopyRight] All Rights Reserved.
 */

package me.lyso.mampa;

import me.lyso.mampa.actor.router.DefaultFsmRouter;
import me.lyso.mampa.actor.router.OneActorRouter;
import me.lyso.mampa.demo.ActorDemoV4.EventType;
import me.lyso.mampa.demo.ActorDemoV4.FsmType;
import me.lyso.mampa.demo.ActorDemoV4.StateType;
import me.lyso.mampa.demo.ActorDemoV4.StateValue;
import me.lyso.mampa.event.IAction;
import me.lyso.mampa.event.IEventType;
import me.lyso.mampa.fsm.AbstractRuleSet;
import me.lyso.mampa.fsm.IFsmType;
import me.lyso.mampa.fsm.State;
import junit.framework.Assert;
import me.lyso.mampa.actor.*;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

/**
 * @author leo
 */
public class ActorsTest {
    static ActorGroup<String, String> group;
    static CountDownLatch waiter = new CountDownLatch(1);
    static boolean ok = false;

    public static class RuleSet extends AbstractRuleSet<String, StateValue> {
        public RuleSet() {
            super(FsmType.Default, StateType.values().length, EventType.values().length, EventType.Timeout, StateType.Stop);
        }

        @Override
        public boolean init(State<StateValue> state, String target, IEventType etype, Object data, IActor<String> master) {
            state.type(StateType.Ok).value(new StateValue().s("Init; "));

            System.out.println(Actors.current());
            Assert.assertTrue(Actors.<String>current() == master);
            Assert.assertTrue(Actors.<String, String>currentGroup() == group);

            int n = 10000000;
            IActor a;
            IActorGroup g;
            long begin = System.nanoTime();
            for (int i = 0; i < n; i++) {
                a = Actors.current();
                g = Actors.currentGroup();
            }
            System.out.printf("Actors     : %d times %.3f ms.\n", n, (System.nanoTime() - begin) / 1000000f);
            ok = true;
            waiter.countDown();
            return true;
        }

        @Override
        protected IAction<String, StateValue> buildDefaultRule() {
            return new IAction<String, StateValue>() {
                @Override
                public NextState exec(State<StateValue> state, IFsmType fsmType, String fsmTarget, IEventType etype, Object data,
                        IActor<String> master) throws Exception {
                    return null;
                }
            };
        }

        @Override
        protected void buildRules() {
        }
    }

    @Test
    public void test() throws InterruptedException {
        Thread.currentThread().setName("MAMPA-Test-G0-A0");
        StateValue.clear();
        ActorConfig<String, String> config = ActorConfig.builder("DemoV4.MultiPriority", OneActorRouter.instance(),
                DefaultFsmRouter.stringInstance(), new RuleSet())
                .actorNumber(1).initialFsmCount(1).maxFsmCount(1).priorityConfigs(new int[]{2, 1}).build();
        group = new ActorGroup<String, String>(config);
        group.start();
        group.tell(0, null, FsmType.Default, "xxxx", EventType.Ping, 1);
        waiter.await();
        Assert.assertTrue(true);
    }
}
