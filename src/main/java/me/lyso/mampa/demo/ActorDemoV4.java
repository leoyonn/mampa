/**
 * ActorDemoV3.java
 * [CopyRight]
 * @author leo [leoyonn@gmail.com]
 * @date Jan 14, 2014 5:16:42 PM
 */
package me.lyso.mampa.demo;

import me.lyso.mampa.actor.ActorConfig;
import me.lyso.mampa.actor.ActorGroup;
import me.lyso.mampa.actor.IActor;
import me.lyso.mampa.actor.NextState;
import me.lyso.mampa.actor.router.DefaultActorRouter;
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

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Multi-priority actor demo.
 *
 * @author leo
 */
public class ActorDemoV4 {

    public static enum StateType implements IStateType {
        Ok, Fail, Stop,
    }

    public static enum EventType implements IEventType {
        Ping, Hit, Kill, Timeout,
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
            addRule(StateType.Ok, EventType.Hit, new IAction<String, StateValue>() {
                @Override
                public NextState exec(State<StateValue> state, IFsmType fsmType, String target, IEventType etype,
                        Object data, IActor<String> master) {
                    state.value().info(state.type(), etype, data);
                    return nextState(master, state.value().state(StateType.Fail));
                }
            });

            addRule(StateType.Ok, EventType.Ping, new IAction<String, StateValue>() {
                @Override
                public NextState exec(State<StateValue> state, IFsmType fsmType, String target, IEventType etype,
                                      Object data, IActor<String> master) {
                    state.value().info(state.type(), etype, data);
                    return nextState(master, state.value().state(StateType.Ok), 1, TimeUnit.SECONDS);
                }
            });

            addRule(StateType.Fail, EventType.Ping, new IAction<String, StateValue>() {
                @Override
                public NextState exec(State<StateValue> state, IFsmType fsmType, String target, IEventType etype,
                        Object data, IActor<String> master) {
                    state.value().info(state.type(), etype, data);
                    return nextState(master, state.value().state(StateType.Ok));
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
                    StateValue.timeoutCount ++;
                    state.value().info(state.type(), etype, data);
                    return nextState(coder, state.value().state(StateType.Fail));
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

    public void runMultiPriorityAndCancelTimeout() throws InterruptedException {
        StateValue.clear();
        ActorConfig<String, String> config = ActorConfig.builder("DemoV4.MultiPriority", OneActorRouter.instance(),
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

    public void runMatchAllActor() throws InterruptedException {
        StateValue.clear();
        ActorConfig<String, String> config = ActorConfig.builder("DemoV4.AllActor", DefaultActorRouter.stringInstance(),
                DefaultFsmRouter.stringInstance(), new RuleSet())
                .actorNumber(4).initialFsmCount(1).maxFsmCount(1).priorityConfigs(new int[]{2, 1}).build();
        ActorGroup<String, String> actor = new ActorGroup<String, String>(config);
        System.out.println(Perf.allCounters());
        actor.start();
        actor.tell(0, "111", FsmType.Default, "xxxx", EventType.Ping, 1);
        asserts(0, StateType.Ok);
        actor.tell(0, "222", FsmType.Default, "xxxx", EventType.Ping, 1);
        asserts(1, StateType.Ok);
        actor.tell(0, "333", FsmType.Default, "xxxx", EventType.Ping, 1);
        asserts(2, StateType.Ok);
        actor.tell(0, "444", FsmType.Default, "xxxx", EventType.Ping, 1);
        asserts(3, StateType.Ok);
        actor.tell(0, "111", FsmType.Default, "xxxx", EventType.Kill, 7);
        asserts(0, StateType.Stop);
        asserts(1, StateType.Ok);
        asserts(2, StateType.Ok);
        asserts(3, StateType.Ok);
        actor.tell(0, DefaultActorRouter.AllTarget, FsmType.Default, DefaultFsmRouter.AllTarget, EventType.Kill, 7);
        asserts(0, StateType.Stop);
        asserts(1, StateType.Stop);
        asserts(2, StateType.Stop);
        asserts(3, StateType.Stop);
        asserts(StateType.Stop);
        System.out.println(Perf.allCounters());
        Thread.sleep(100);
        actor.shutdown();
        Thread.sleep(100);
    }

    public void runBatchFsm() throws InterruptedException {
        StateValue.clear();
        ActorConfig<String, String> config = ActorConfig.builder("DemoV4.BatchFsm", DefaultActorRouter.stringInstance(),
                DefaultFsmRouter.stringInstance(), new RuleSet())
                .actorNumber(4).initialFsmCount(1).maxFsmCount(1).priorityConfigs(new int[]{2, 1}).build();
        ActorGroup<String, String> actor = new ActorGroup<String, String>(config);
        System.out.println(Perf.allCounters());
        actor.start();
        actor.tell(0, "111", FsmType.Default, "111", EventType.Ping, 1);
        asserts(0, StateType.Ok);
        actor.tell(0, "222", FsmType.Default, "222", EventType.Ping, 1);
        asserts(1, StateType.Ok);
        actor.tell(0, "333", FsmType.Default, "333", EventType.Ping, 1);
        asserts(2, StateType.Ok);
        actor.tell(0, "444", FsmType.Default, "444", EventType.Ping, 1);
        asserts(3, StateType.Ok);
        actor.batchTell(Arrays.asList("111", "222"), FsmType.Default, Arrays.asList("111", "222"), EventType.Kill, 7);
        asserts(0, StateType.Stop);
        asserts(1, StateType.Stop);
        asserts(2, StateType.Ok);
        asserts(3, StateType.Ok);
        actor.batchTell(Arrays.asList("111", "333", "555"), FsmType.Default, Arrays.asList("111", "333", "555"), EventType.Kill, 7);
        asserts(0, StateType.Stop);
        asserts(1, StateType.Stop);
        asserts(2, StateType.Stop);
        asserts(3, StateType.Ok);
        asserts(4, StateType.Stop);
        System.out.println(Perf.allCounters());
        Thread.sleep(100);
        actor.shutdown();
        Thread.sleep(100);
    }

    public static void main(String[] args) throws InterruptedException {
        new ActorDemoV4().runMultiPriorityAndCancelTimeout();
    }
}
