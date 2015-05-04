/**
 *
 * WaitStrategyTest.java
 * @date 14-7-8 下午9:18
 * @author leo [leoyonn@gmail.com]
 * [CopyRight] All Rights Reserved.
 */

package me.lyso.mampa;

import com.lmax.disruptor.WaitStrategyType;
import me.lyso.mampa.actor.*;
import me.lyso.mampa.actor.router.DefaultActorRouter;
import me.lyso.mampa.actor.router.OneFsmRouter;
import me.lyso.mampa.event.IAction;
import me.lyso.mampa.event.IEventType;
import me.lyso.mampa.fsm.*;
import me.lyso.perf.Perf;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author leo
 */
@Ignore
public class WaitStrategyTest {
    int n = 5000;
    int t = 50;

    @Test
    public void test() throws InterruptedException {
        ActorConfig<String, String> config = ActorConfig.builder("WaitStrategyTest", DefaultActorRouter.stringInstance(),
                OneFsmRouter.stringInstance(), new RuleSet()).ringSizePerActor(16).waitStrategy(WaitStrategyType.Blocking)
                .actorNumber(4).initialFsmCount(1).maxFsmCount(1).build();
        ActorGroup<String, String> actor = new ActorGroup<String, String>(config);
        actor.start();
        System.out.println("Beginning...");
        long begin = System.currentTimeMillis();

        Thread[] threads = new Thread[t];
        for (int i = 0; i < t; i++) {
            threads[i] = new Thread(new Sender(actor));
        }
        for (int i = 0; i < t; i++) {
            threads[i].start();
        }
        for (int i = 0; i < t; i++) {
            threads[i].join();
        }
        System.out.println("Elapse: " + (System.currentTimeMillis() - begin));
        Thread.sleep(1000);
    }

    private void printPerf() {
        for (Map.Entry<String, Long> e : Perf.allCounters().entrySet()) {
            System.out.println(e.getKey() + "\t:\t" + e.getValue());
        }
    }

    static AtomicInteger idxer = new AtomicInteger(0);

    class Sender implements Runnable {

        private ActorGroup<String, String> actor;
        private int id;

        private Sender(ActorGroup<String, String> actor) {
            this.actor = actor;
            this.id = idxer.getAndIncrement();
        }

        @Override
        public void run() {
            for (int i = 0; i < n / t; i++) {
                String k = RandomStringUtils.randomAlphabetic(16);
                actor.tellSync(k, IFsmType.DefaultFsmType.One, k, EventType.Ping, 1);
                if (i % 20 == 0 && id == 0) {
                    printPerf();
                }
            }
        }
    }

    public static enum StateType implements IStateType {
        Ok, Fail, Stop,
    }

    public static enum EventType implements IEventType {
        Ping, Timeout,
    }

    public static enum FsmType implements IFsmType {
        Default,
    }

    public static class StateValue {
    }

    public static class RuleSet extends AbstractRuleSet<String, StateValue> {
        public RuleSet() {
            super(FsmType.Default, StateType.values().length, EventType.values().length, EventType.Timeout, StateType.Stop);
        }

        @Override
        public boolean init(State<StateValue> state, String target, IEventType etype, Object data, IActor<String> master) {
            state.type(StateType.Ok).value(new StateValue());
            return true;
        }

        @Override
        protected IAction<String, StateValue> buildDefaultRule() {
            return new IAction<String, StateValue>() {
                @Override
                public NextState exec(State<StateValue> state, IFsmType fsmType, String target, IEventType etype,
                                      Object data, IActor<String> coder) {
                    return nextState(coder, StateType.Fail);
                }
            };
        }

        @Override
        public void onStop(State<StateValue> s, IActor<String> master) {
            System.out.println(s + " stopped!");
        }

        @Override
        protected void buildRules() {
            addRule(StateType.Ok, EventType.Ping, new IAction<String, StateValue>() {
                @Override
                public NextState exec(State<StateValue> state, IFsmType fsmType, String target, IEventType etype,
                                      Object data, IActor<String> master) throws InterruptedException {
                    Thread.sleep(10);
                    return nextState(master, StateType.Ok);
                }
            });
        }
    }
}
