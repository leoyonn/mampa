/**
 *
 * JustTest.java
 * @date 14-12-18 下午2:14
 * @author leo [leoyonn@gmail.com]
 * [CopyRight] All Rights Reserved.
 */

package me.lyso.mampa;

import me.lyso.mampa.actor.ActorConfig;
import me.lyso.mampa.actor.ActorGroup;
import me.lyso.mampa.actor.IActor;
import me.lyso.mampa.actor.NextState;
import me.lyso.mampa.actor.router.DefaultFsmRouter;
import me.lyso.mampa.actor.router.OneActorRouter;
import me.lyso.mampa.event.IAction;
import me.lyso.mampa.event.IEventType;
import me.lyso.mampa.fsm.AbstractRuleSet;
import me.lyso.mampa.fsm.IFsmType;
import me.lyso.mampa.fsm.IStateType;
import me.lyso.mampa.fsm.State;
import me.lyso.perf.Perf;
import junit.framework.Assert;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

/**
 * @author leo
 */
public class TimeoutTest {

    public static enum StateType implements IStateType {
        Ok, Fail, After, Stop,
    }

    public static enum EventType implements IEventType {
        Ping, Hit, Kill, After, Timeout,
    }

    public static enum FsmType implements IFsmType {
        Default,
    }

    public static class StateValue {
        static int indexer = 0;
        static int timeoutCount = 0;
        static StateValue[] data = new StateValue[10];
        private String info;
        private StateType state;

        public StateValue() {
            info = "Id-" + indexer + ": ";
            data[indexer] = this;
            indexer++;
        }

        public StateValue info(IStateType s, IEventType e, Object d) {
            this.info += s + " * " + e + "(" + d + "); ";
            System.out.println(this.info);
            return this;
        }

        public IStateType state(IStateType state) {
            this.state = (StateType) state;
            return state;
        }

        public StateValue s(String s) {
            info += s;
            return this;
        }

        public String toString() {
            return info;
        }

        public static void clear() {
            indexer = 0;
            timeoutCount = 0;
            data = new StateValue[10];
        }
    }

    public static class RuleSet extends AbstractRuleSet<String, StateValue> {
        public RuleSet() {
            super(FsmType.Default, StateType.values().length, EventType.values().length, EventType.Timeout, StateType.Stop);
        }

        @Override
        public boolean init(State<StateValue> state, String target, IEventType etype, Object data, IActor<String> master) {
            state.type(StateType.Ok).value(new StateValue().s("Init; "));
            return true;
        }

        @Override
        protected IAction<String, StateValue> buildDefaultRule() {
            return new IAction<String, StateValue>() {
                @Override
                public NextState exec(State<StateValue> state, IFsmType fsmType, String target, IEventType etype,
                        Object data, IActor<String> coder) {
                    state.value().info(state.type(), etype, data);
                    return nextState(coder, state.value().state(StateType.Fail));
                }
            };
        }

        @Override
        public void onStop(State<StateValue> s, IActor<String> master) {
            System.out.println(s + " stopped!");
        }

        @Override
        protected void buildRules() {
            addRule(EventType.Ping, new IAction<String, StateValue>() {
                @Override
                public NextState exec(State<StateValue> state, IFsmType fsmType, String target, IEventType etype,
                        Object data, IActor<String> master) {
                    state.value().info(state.type(), etype, data);
                    return nextState(master, state.value().state(StateType.Ok), 1000, TimeUnit.MILLISECONDS);
                }
            });

            addRule(EventType.After, new IAction<String, StateValue>() {
                @Override
                public NextState exec(State<StateValue> state, IFsmType fsmType, String target, IEventType etype,
                        Object data, IActor<String> master) {
                    master.dealAfter(FsmType.Default, "xxx", EventType.After, 1, 1000, TimeUnit.MILLISECONDS);
                    System.out.println("Deal-after-" + StateValue.timeoutCount);
                    state.value().info(state.type(), etype, data);
                    StateValue.timeoutCount++;
                    return nextState(master, state.value().state(StateType.After));
                }
            });

            addRule(EventType.Kill, new IAction<String, StateValue>() {
                @Override
                public NextState exec(State<StateValue> state, IFsmType fsmType, String target, IEventType etype,
                        Object data, IActor<String> master) {
                    state.value().info(state.type(), etype, data);
                    return nextState(master, state.value().state(StateType.Stop));
                }
            });

            addTimeoutRule(StateType.Ok, new IAction<String, StateValue>() {
                @Override
                public NextState exec(State<StateValue> state, IFsmType fsmType, String target, IEventType etype,
                        Object data, IActor<String> coder) {
                    StateValue.timeoutCount++;
                    state.value().info(state.type(), etype, data);
                    return nextState(coder, state.value().state(StateType.Fail), 1000, TimeUnit.MILLISECONDS);
                }
            });

            addTimeoutRule(StateType.Fail, new IAction<String, StateValue>() {
                @Override
                public NextState exec(State<StateValue> state, IFsmType fsmType, String target, IEventType etype,
                        Object data, IActor<String> coder) {
                    StateValue.timeoutCount++;
                    state.value().info(state.type(), etype, data);
                    return nextState(coder, state.value().state(StateValue.timeoutCount > 5 ? StateType.Stop : StateType.Fail), 1000, TimeUnit.MILLISECONDS);
                }
            });
        }
    }

    private void asserts(IStateType state) throws InterruptedException {
        asserts(0, state);
    }

    private void asserts(int i, IStateType state) throws InterruptedException {
        Thread.sleep(100);
        System.out.println(String.format("Is: %s, Should: %s", StateValue.data[i].state, state));
        Assert.assertEquals(state, StateValue.data[i].state);
    }

    @Test
    public void timeoutTest() throws InterruptedException {
        StateValue.clear();
        ActorConfig<String, String> config = ActorConfig.builder("Timeout-Test", OneActorRouter.instance(),
                DefaultFsmRouter.stringInstance(), new RuleSet())
                .actorNumber(1).initialFsmCount(1).maxFsmCount(1).priorityConfigs(new int[]{2, 1}).build();
        ActorGroup<String, String> actor = new ActorGroup<String, String>(config);
        System.out.println(Perf.allCounters());
        actor.start();
        actor.tell(0, null, FsmType.Default, "xxxx", EventType.Ping, 1);
        asserts(StateType.Ok);
        Thread.sleep(1100);
        asserts(StateType.Fail);
        Assert.assertEquals(1, StateValue.timeoutCount);
        actor.tell(0, null, FsmType.Default, "xxxx", EventType.Ping, 3);
        asserts(StateType.Ok);
        actor.tell(1, null, FsmType.Default, "xxxx", EventType.Hit, 5);
        asserts(StateType.Fail);
        actor.tell(0, null, FsmType.Default, "xxxx", EventType.Kill, 7);
        asserts(StateType.Stop);
        for (int i = 0; i < 4; i++) {
            int i1 = i * 2 + 1, i2 = i1 + 1;
            Assert.assertTrue(StateValue.data[0].info.contains("(" + i1 + ")"));
            Assert.assertFalse(StateValue.data[0].info.contains("(" + i2 + ")"));
        }
        actor.tell(0, null, FsmType.Default, "yyyy", EventType.Ping, 1);
        asserts(1, StateType.Ok);
        Thread.sleep(800);
        actor.tell(1, null, FsmType.Default, "yyyy", EventType.Ping, 1);
        asserts(1, StateType.Ok);
        Thread.sleep(800);
        actor.tell(0, null, FsmType.Default, "yyyy", EventType.Ping, 1);
        asserts(1, StateType.Ok);
        Thread.sleep(800);
        actor.tell(1, null, FsmType.Default, "yyyy", EventType.Ping, 1);
        asserts(1, StateType.Ok);
        Thread.sleep(800);
        Assert.assertEquals(1, StateValue.timeoutCount);
        asserts(1, StateType.Ok);
        Thread.sleep(300);
        Assert.assertEquals(2, StateValue.timeoutCount);
        asserts(1, StateType.Fail);
        System.out.println(Perf.allCounters());
        Thread.sleep(100);
        actor.shutdown();
        Thread.sleep(100);
    }

    @Test
    public void pureTimeoutTest() throws InterruptedException {
        StateValue.clear();
        ActorConfig<String, String> config = ActorConfig.builder("Pure-Timeout-Test", OneActorRouter.instance(),
                DefaultFsmRouter.stringInstance(), new RuleSet())
                .actorNumber(1).initialFsmCount(1).maxFsmCount(1).priorityConfigs(new int[]{2, 1}).build();
        ActorGroup<String, String> actor = new ActorGroup<String, String>(config);
        System.out.println(Perf.allCounters());
        actor.start();
        actor.tell(0, null, FsmType.Default, "xxxx", EventType.Ping, 1);
        asserts(StateType.Ok);
        Thread.sleep(1100);
        asserts(StateType.Fail);
        Assert.assertEquals(1, StateValue.timeoutCount);
        Thread.sleep(1000);
        asserts(StateType.Fail);
        Assert.assertEquals(2, StateValue.timeoutCount);
        Thread.sleep(1000);
        asserts(StateType.Fail);
        Assert.assertEquals(3, StateValue.timeoutCount);
        Thread.sleep(1000);
        asserts(StateType.Fail);
        Assert.assertEquals(4, StateValue.timeoutCount);
        Thread.sleep(1000);
        asserts(StateType.Fail);
        Assert.assertEquals(5, StateValue.timeoutCount);
        Thread.sleep(1000);
        asserts(StateType.Stop);
        actor.shutdown();
        Thread.sleep(100);
    }

    @Test
    public void dealAfterTest() throws InterruptedException {
        StateValue.clear();
        ActorConfig<String, String> config = ActorConfig.builder("DealAfter-Test", OneActorRouter.instance(),
                DefaultFsmRouter.stringInstance(), new RuleSet())
                .actorNumber(1).initialFsmCount(1).maxFsmCount(1).priorityConfigs(new int[]{2, 1}).build();
        ActorGroup<String, String> actor = new ActorGroup<String, String>(config);
        System.out.println(Perf.allCounters());
        actor.start();
        actor.tell(0, null, FsmType.Default, "xxxx", EventType.After, 1);
        asserts(StateType.After);
        Assert.assertEquals(1, StateValue.timeoutCount);
        Thread.sleep(1000);
        asserts(StateType.After);
        Assert.assertEquals(2, StateValue.timeoutCount);
        Thread.sleep(1000);
        asserts(StateType.After);
        Assert.assertEquals(3, StateValue.timeoutCount);
        Thread.sleep(1000);
        asserts(StateType.After);
        Assert.assertEquals(4, StateValue.timeoutCount);
        Thread.sleep(1000);
        asserts(StateType.After);
        Assert.assertEquals(5, StateValue.timeoutCount);
        Thread.sleep(1000);
        asserts(StateType.After);
        actor.shutdown();
        Thread.sleep(100);
    }
}

