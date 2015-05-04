/**
 * ActorDemoV2.java
 * [CopyRight]
 * @author leo [leoyonn@gmail.com]
 * @date Jan 14, 2014 6:08:49 PM
 */

package me.lyso.mampa.demo;

import java.util.concurrent.TimeUnit;

import me.lyso.mampa.actor.ActorConfig;
import me.lyso.mampa.actor.ActorGroup;
import me.lyso.mampa.actor.IActor;
import me.lyso.mampa.actor.NextState;
import me.lyso.mampa.actor.router.DefaultActorRouter;
import me.lyso.mampa.actor.router.DefaultFsmRouter;
import me.lyso.mampa.event.IAction;
import me.lyso.mampa.event.IEventType;
import me.lyso.mampa.fsm.AbstractRuleSet;
import me.lyso.mampa.fsm.FSM;
import me.lyso.mampa.fsm.IFsmType;
import me.lyso.mampa.fsm.IStateType;
import me.lyso.mampa.fsm.State;

/**
 * Demo: 码农的生活单调版
 * 
 * @author leo
 */
public class ActorDemoV2 {
    public static enum CoderStateType implements IStateType {
        Coding, Eating, Thinking, Stop;
    }

    public static enum CoderEventType implements IEventType {
        WorkIn, GirlIn, FoodIn, Timeout;
    }

    /**
     * Only 1 kind of {@link FSM}
     * 
     * @author leo
     */
    public static enum CoderFsmType implements IFsmType {
        Default,
    }

    public static class CoderStateValue {
        /** 饥饿感：0-100，越大越饿 */
        private int hunger = 30;

        /** 自尊心：0-100，越大越强 */
        private int pride = 100;

        public CoderStateValue hunger(int delta) {
            hunger += delta;
            if (hunger < 0) {
                hunger = 0;
            } else if (hunger > 100) {
                hunger = 100;
            }
            return this;
        }

        public CoderStateValue pride(int delta) {
            pride += delta;
            if (pride < 0) {
                pride = 0;
            } else if (pride > 100) {
                pride = 100;
            }
            return this;
        }

        public String toString() {
            return "<hunger:" + hunger + "|pride:" + pride + ">";
        }
    }

    public static class CoderRuleSet extends AbstractRuleSet<String, CoderStateValue> {
        public CoderRuleSet() {
            super(CoderFsmType.Default, CoderStateType.values().length, CoderEventType.values().length,
                  CoderEventType.Timeout, CoderStateType.Stop);
        }

        @Override
        public boolean init(State<CoderStateValue> state, String target, IEventType etype, Object data,
                IActor<String> master) {
            // 只要有事, 程序员就会无条件出现并开始写代码
            state.type(CoderStateType.Coding).value(new CoderStateValue());
            print(state, null, "Hello World!");
            return true;
        }

        @Override
        protected IAction<String, CoderStateValue> buildDefaultRule() {
            return new IAction<String, CoderStateValue>() {
                @Override
                public NextState exec(State<CoderStateValue> state, IFsmType fsmType, String target, IEventType etype,
                        Object food, IActor<String> coder) {
                    print(state, etype, "WTF!!!");
                    return nextState(coder, CoderStateType.Thinking);
                }
            };
        }

        @Override
        protected void buildRules() {

            // 正在抠定时又来了一个任务！
            addRule(CoderStateType.Coding, CoderEventType.WorkIn, new IAction<String, CoderStateValue>() {
                @Override
                public NextState exec(State<CoderStateValue> state, IFsmType fsmType, String target, IEventType etype,
                        Object amount, IActor<String> coder) {
                    CoderStateValue innerState = state.value();
                    if (innerState.hunger > 80) {
                        print(state, etype, "饿死了，不干了，思考人生。");
                        innerState.hunger(10);
                        innerState.pride(-20);
                        return nextState(coder, CoderStateType.Thinking, 1, TimeUnit.SECONDS);
                    } else if (innerState.pride < 20) {
                        print(state, etype, "人生很无趣，干什么干。");
                        innerState.hunger(10);
                        innerState.pride(-10);
                        return nextState(coder, CoderStateType.Thinking, 1, TimeUnit.SECONDS);
                    } else {
                        print(state, etype, "我是码农，我爱编码。");
                        innerState.hunger((Integer) amount); // 干多少就应该吃多少
                        innerState.pride(1);
                        return nextState(coder, CoderStateType.Coding, 2, TimeUnit.SECONDS);
                    }
                }
            });

            // 正在思考时有吃的了！
            addRule(CoderStateType.Thinking, CoderEventType.FoodIn, new IAction<String, CoderStateValue>() {
                @Override
                public NextState exec(State<CoderStateValue> state, IFsmType fsmType, String target, IEventType etype,
                        Object data, IActor<String> coder) {
                    int amount = (Integer) data;
                    CoderStateValue innerState = state.value();
                    print(state, etype, "搞了点吃的。");
                    innerState.hunger(-amount);
                    innerState.pride(amount / 20);
                    return nextState(coder, CoderStateType.Eating);
                }
            });

            // 工作太久也会累 （用 SECOND 代 DAY）
            addTimeoutRule(CoderStateType.Coding, new IAction<String, CoderStateValue>() {
                @Override
                public NextState exec(State<CoderStateValue> state, IFsmType fsmType, String target, IEventType etype,
                        Object data, IActor<String> coder) {
                    CoderStateValue innerState = state.value();
                    print(state, etype, "累了，休息会，思考人生。");
                    innerState.hunger(20);
                    innerState.pride(-5);
                    return nextState(coder, CoderStateType.Thinking, 1, TimeUnit.SECONDS);
                }
            });

            // 思考一段时间看还能不能起来工作
            addTimeoutRule(CoderStateType.Thinking, new IAction<String, CoderStateValue>() {
                @Override
                public NextState exec(State<CoderStateValue> state, IFsmType fsmType, String target, IEventType etype,
                        Object data, IActor<String> coder) {
                    CoderStateValue innerState = state.value();
                    if (innerState.hunger < 80 && innerState.pride > 20) {
                        innerState.hunger(5);
                        innerState.pride(5);
                        print(state, etype, "继续干活。");
                        return nextState(coder, CoderStateType.Coding, 2, TimeUnit.SECONDS);
                    } else if (innerState.hunger == 100 && innerState.pride == 0) {
                        print(state, etype, "死了算了。");
                        return stopState(coder);
                    } else {
                        innerState.hunger(5);
                        innerState.pride(-20);
                        print(state, etype, "无趣的人生。。。");
                        return nextState(coder, CoderStateType.Thinking, 1, TimeUnit.SECONDS);
                    }
                }
            });

            // 吃饱了
            addTimeoutRule(CoderStateType.Eating, new IAction<String, CoderStateValue>() {
                @Override
                public NextState exec(State<CoderStateValue> state, IFsmType fsmType, String target, IEventType etype,
                        Object data, IActor<String> coder) {
                    CoderStateValue innerState = state.value();
                    if (innerState.hunger < 80 && innerState.pride > 20) {
                        innerState.hunger(5);
                        innerState.pride(5);
                        print(state, etype, "吃饱了，起来干活。");
                        return nextState(coder, CoderStateType.Coding, 2, TimeUnit.SECONDS);
                    } else {
                        innerState.hunger(5);
                        innerState.pride(-5);
                        print(state, etype, "即使吃了。。。也是无趣的人生。。。");
                        return nextState(coder, CoderStateType.Thinking, 1, TimeUnit.SECONDS);
                    }
                }
            });

        }
    }

    private static long begin = System.currentTimeMillis();

    private static void print(State<CoderStateValue> state, IEventType etype, String talk) {
        long sec = (System.currentTimeMillis() - begin) / 1000;
        System.out.println(String.format("Day %d: %s x %s : %s", sec, state.toString(),
                etype == null ? "Init" : etype.toString(), talk));
    }

    public static void main(String[] args) throws InterruptedException {
        ActorConfig<String, String> config = ActorConfig.builder("DemoV2", DefaultActorRouter.stringInstance(),
                DefaultFsmRouter.stringInstance(), new CoderRuleSet())
                .actorNumber(1).initialFsmCount(1).maxFsmCount(1).build();
        ActorGroup<String, String> coders = new ActorGroup<String, String>(config);
        coders.start();

        coders.tell("猿弟", CoderFsmType.Default, "猿弟", CoderEventType.WorkIn, 20);
        coders.tell("猿哥", CoderFsmType.Default, "猿哥", CoderEventType.WorkIn, 1000);
        Thread.sleep(20000);
        // coder.tell("猿弟", CoderEventType.FoodIn, 100);
        // Thread.sleep(20000);

        coders.shutdown();
        Thread.sleep(1000);
    }
}
