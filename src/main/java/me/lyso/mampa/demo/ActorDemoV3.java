/**
 * ActorDemoV3.java
 * [CopyRight]
 * @author leo [leoyonn@gmail.com]
 * @date Jan 14, 2014 5:16:42 PM
 */
package me.lyso.mampa.demo;

import me.lyso.mampa.actor.*;
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

import java.util.concurrent.TimeUnit;

/**
 * Multi rule-set(fsm) in an actor demo.
 *
 * @author leo
 */
public class ActorDemoV3 {

    public static enum StateType1 implements IStateType {
        Ok, Fail, Stop,
    }

    public static enum EventType1 implements IEventType {
        Ping, Hit, Kill, Timeout,
    }

    public static enum StateType2 implements IStateType {
        Ok, Fail, Stop,
    }

    public static enum EventType2 implements IEventType {
        Ping, Hit, Kill, Timeout,
    }

    public static enum FsmType implements IFsmType {
        F1, F2,
    }

    public static class StateValue {
        static int idx = 0;
        private String info;
        private static String info1; 
        private static String info2; 
        private static StateType1 state1;
        private static StateType2 state2;

        public StateValue() {
            info = "Id-" + (idx++) + ": ";
        }

        public StateValue info(IStateType s, IEventType e, Object d) {
            this.info += s + " * " + e + "(" + d + "); ";
            System.out.println(this.info);
            return this;
        }

        public IStateType state(IStateType state) {
            if (state instanceof StateType1) {
                StateValue.state1 = (StateType1) state;
                info1 = info;
            } else {
                StateValue.state2 = (StateType2) state;
                info2 = info;
            }
            return state;
        }

        public StateValue s(String s) {
            this.info += s;
            return this;
        }

        public String toString() {
            return info;
        }
    }

    public static class RuleSet1 extends AbstractRuleSet<String, StateValue> {
        public RuleSet1() {
            super(FsmType.F1, StateType1.values().length, EventType1.values().length, EventType1.Timeout, StateType1.Stop);
        }

        @Override
        public boolean init(State<StateValue> state, String target, IEventType etype, Object data, IActor<String> master) {
            state.type(StateType1.Ok).value(new StateValue().s("Init; "));
            return true;
        }

        @Override
        protected IAction<String, StateValue> buildDefaultRule() {
            return new IAction<String, StateValue>() {
                @Override
                public NextState exec(State<StateValue> state, IFsmType fsmType, String target, IEventType etype,
                        Object data, IActor<String> coder) {
                    state.value().info(state.type(), etype, data);
                    return nextState(coder, state.value().state(StateType1.Fail));
                }
            };
        }

        @Override
        protected void buildRules() {
            addRule(StateType1.Ok, EventType1.Hit, new IAction<String, StateValue>() {
                @Override
                public NextState exec(State<StateValue> state, IFsmType fsmType, String target, IEventType etype,
                        Object data, IActor<String> master) {
                    state.value().info(state.type(), etype, data);
                    return nextState(master, state.value().state(StateType1.Fail));
                }
            });

            addRule(StateType1.Ok, EventType1.Ping, new IAction<String, StateValue>() {
                @Override
                public NextState exec(State<StateValue> state, IFsmType fsmType, String target, IEventType etype,
                        Object data, IActor<String> master) {
                    state.value().info(state.type(), etype, data);
                    return nextState(master, state.value().state(StateType1.Ok), 100, TimeUnit.MILLISECONDS);
                }
            });

            addRule(StateType1.Fail, EventType1.Ping, new IAction<String, StateValue>() {
                @Override
                public NextState exec(State<StateValue> state, IFsmType fsmType, String target, IEventType etype,
                        Object data, IActor<String> master) {
                    state.value().info(state.type(), etype, data);
                    return nextState(master, state.value().state(StateType1.Ok), 100, TimeUnit.MILLISECONDS);
                }
            });

            addTimeoutRule(StateType1.Ok, new IAction<String, StateValue>() {
                @Override
                public NextState exec(State<StateValue> state, IFsmType fsmType, String target, IEventType etype,
                        Object data, IActor<String> coder) {
                    state.value().info(state.type(), etype, data);
                    return nextState(coder, state.value().state(StateType1.Fail));
                }
            });

            addRule(EventType1.Kill, new IAction<String, StateValue>() {
                @Override
                public NextState exec(State<StateValue> state, IFsmType fsmType, String target, IEventType etype,
                        Object data, IActor<String> master) {
                    state.value().info(state.type(), etype, data);
                    return nextState(master, state.value().state(StateType1.Stop));
                }
            });
        }
    }

    public static class RuleSet2 extends AbstractRuleSet<String, StateValue> {
        public RuleSet2() {
            super(FsmType.F2, StateType2.values().length, EventType2.values().length, EventType2.Timeout, StateType2.Stop);
        }

        @Override
        public boolean init(State<StateValue> state, String target, IEventType etype, Object data, IActor<String> master) {
            state.type(StateType2.Ok).value(new StateValue().s("Init; "));
            return true;
        }

        @Override
        protected IAction<String, StateValue> buildDefaultRule() {
            return new IAction<String, StateValue>() {
                @Override
                public NextState exec(State<StateValue> state, IFsmType fsmType, String target, IEventType etype,
                        Object data, IActor<String> coder) {
                    state.value().info(state.type(), etype, data);
                    return nextState(coder, state.value().state(StateType2.Fail));
                }
            };
        }

        @Override
        protected void buildRules() {
            addRule(StateType2.Ok, EventType2.Hit, new IAction<String, StateValue>() {
                @Override
                public NextState exec(State<StateValue> state, IFsmType fsmType, String target, IEventType etype,
                        Object data, IActor<String> master) {
                    state.value().info(state.type(), etype, data);
                    Assert.assertTrue(Actors.<String>current() == master);
                    return nextState(master, state.value().state(StateType2.Fail));
                }
            });

            addRule(StateType2.Ok, EventType2.Ping, new IAction<String, StateValue>() {
                @Override
                public NextState exec(State<StateValue> state, IFsmType fsmType, String target, IEventType etype,
                        Object data, IActor<String> master) {
                    state.value().info(state.type(), etype, data);
                    Assert.assertTrue(Actors.<String>current() == master);
                    return nextState(master, state.value().state(StateType2.Ok), 100, TimeUnit.MILLISECONDS);
                }
            });

            addRule(StateType2.Fail, EventType2.Ping, new IAction<String, StateValue>() {
                @Override
                public NextState exec(State<StateValue> state, IFsmType fsmType, String target, IEventType etype,
                        Object data, IActor<String> master) {
                    state.value().info(state.type(), etype, data);
                    Assert.assertTrue(Actors.<String>current() == master);
                    return nextState(master, state.value().state(StateType2.Ok), 100, TimeUnit.MILLISECONDS);
                }
            });

            addTimeoutRule(StateType2.Ok, new IAction<String, StateValue>() {
                @Override
                public NextState exec(State<StateValue> state, IFsmType fsmType, String target, IEventType etype,
                        Object data, IActor<String> master) {
                    state.value().info(state.type(), etype, data);
                    Assert.assertTrue(Actors.<String>current() == master);
                    return nextState(master, state.value().state(StateType2.Fail));
                }
            });

            addRule(EventType2.Kill, new IAction<String, StateValue>() {
                @Override
                public NextState exec(State<StateValue> state, IFsmType fsmType, String target, IEventType etype,
                        Object data, IActor<String> master) {
                    state.value().info(state.type(), etype, data);
                    Assert.assertTrue(Actors.<String>current() == master);
                    return nextState(master, state.value().state(StateType2.Stop));
                }
            });
        }
    }

    private void asserts(IStateType state) throws InterruptedException {
        Thread.sleep(50);
        if (state instanceof StateType1) {
            System.out.println(String.format("%s. Is: %s, Should: %s", state.getClass().getSimpleName(), StateValue.state1, state));
            Assert.assertEquals(StateValue.state1, state);
        } else {
            System.out.println(String.format("%s. Is: %s, Should: %s", state.getClass().getSimpleName(), StateValue.state2, state));
            Assert.assertEquals(StateValue.state2, state);
        }
    }

    public void run() throws InterruptedException {
        ActorConfig<String, String> config = ActorConfig.builder("DemoV2", OneActorRouter.instance(),
                DefaultFsmRouter.stringInstance(), new RuleSet2(), new RuleSet1())
                .actorNumber(1).initialFsmCount(1).maxFsmCount(1).build();
        ActorGroup<String, String> actor = new ActorGroup<String, String>(config);
        System.out.println(Perf.allCounters());
        actor.start();
        actor.tell(null, FsmType.F1, "xxxx", EventType1.Ping, 1);
        actor.tell(null, FsmType.F2, "xxxx", EventType2.Ping, 2);
        asserts(StateType1.Ok);
        asserts(StateType2.Ok);
        Thread.sleep(200);
        asserts(StateType1.Fail);
        asserts(StateType2.Fail);
        actor.tell(null, FsmType.F1, "xxxx", EventType1.Ping, 3);
        actor.tell(null, FsmType.F2, "xxxx", EventType2.Ping, 4);
        asserts(StateType1.Ok);
        asserts(StateType2.Ok);
        actor.tell(null, FsmType.F1, "xxxx", EventType1.Hit, 5);
        actor.tell(null, FsmType.F2, "xxxx", EventType2.Hit, 6);
        asserts(StateType1.Fail);
        asserts(StateType2.Fail);
        actor.tell(null, FsmType.F1, "xxxx", EventType1.Kill, 7);
        actor.tell(null, FsmType.F2, "xxxx", EventType2.Kill, 8);
        asserts(StateType1.Stop);
        asserts(StateType2.Stop);
        for (int i = 0; i < 4; i++) {
            int i1 = i * 2 + 1, i2 = i1 + 1;
            Assert.assertTrue(StateValue.info1.contains("(" + i1 + ")"));
            Assert.assertTrue(StateValue.info2.contains("(" + i2 + ")"));
            Assert.assertFalse(StateValue.info1.contains("(" + i2 + ")"));
            Assert.assertFalse(StateValue.info2.contains("(" + i1 + ")"));
        }
        System.out.println(Perf.allCounters());
        Thread.sleep(1000);
        actor.shutdown();
        Thread.sleep(1000);
    }

    public static void main(String[] args) throws InterruptedException {
        new ActorDemoV3().run();
    }
}
