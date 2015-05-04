/**
 * ActorDemo.java
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
import me.lyso.mampa.fsm.IFsmType;
import me.lyso.mampa.fsm.IFsmType.DefaultFsmType;
import me.lyso.mampa.fsm.IStateType;
import me.lyso.mampa.fsm.State;

/**
 * Demo: 猪的生活
 * 
 * @author leo
 */
public class ActorDemo {
    public static enum PigStateType implements IStateType {
        Humming, Howling, Sleeping, Eating, Stop,
    }

    public static enum PigEventType implements IEventType {
        Lash, Feed, Pat, Timeout;
    }

    public static class PigRuleSet extends AbstractRuleSet<String, Integer> {
        public PigRuleSet() {
            super(DefaultFsmType.One, PigStateType.values().length, PigEventType.values().length,
                  PigEventType.Timeout, PigStateType.Stop);
        }

        // 猪一出世就开始哼哼
        @Override
        public boolean init(State<Integer> state, String fsmTarget, IEventType etype, Object data, IActor<String> master) {
            System.out.println("Hello World!");
            state.type(PigStateType.Humming).value(10);
            return true;
        }

        // 不知所措
        @Override
        protected IAction<String, Integer> buildDefaultRule() {
            return new IAction<String, Integer>() {
                @Override
                public NextState exec(State<Integer> state, IFsmType fsmType, String fsmTarget, IEventType etype,
                        Object data, IActor<String> pig) throws Exception {
                    System.out.println(state + " x " + etype + ": WTF!!!");
                    return nextState(pig, PigStateType.Howling);
                }
            };
        }

        @Override
        protected void buildRules() {
            // 猪哼哼着时被摸一摸，接着哼哼
            addRule(PigStateType.Humming, PigEventType.Pat, new IAction<String, Integer>() {
                @Override
                public NextState exec(State<Integer> state, IFsmType fsmType, String fsmTarget, IEventType etype,
                        Object data, IActor<String> pig) {
                    System.out.println(state + " x " + etype + ": 爽，再摸摸");
                    if (state.value() > 50) {
                        return nextState(pig, PigStateType.Sleeping);
                    } else {
                        return nextState(pig, PigStateType.Humming);
                    }
                }
            });
            // 猪哼哼着时被喂一喂，开吃
            addRule(PigStateType.Humming, PigEventType.Feed, new IAction<String, Integer>() {
                @Override
                public NextState exec(State<Integer> state, IFsmType fsmType, String target, IEventType etype, Object edata,
                        IActor<String> pig) {
                    Food food = (Food)edata;
                    if (food == null || food.amount <= 0) {
                        System.out.println(state + " x " + etype + ": 妈的，别哄老子");
                        return nextState(pig, PigStateType.Howling);
                    }
                    food.take(10);
                    System.out.println(state + " x " + etype + ": 爽，再来点");
                    return nextState(pig, PigStateType.Eating);
                }
            });
            // 猪哼哼着时被抽一鞭，开始嚎
            addRule(PigStateType.Humming, PigEventType.Lash, new IAction<String, Integer>() {
                @Override
                public NextState exec(State<Integer> state, IFsmType fsmType, String target, IEventType etype, Object edata,
                        IActor<String> pig) {
                    System.out.println(state + " x " + etype + ": 汪汪汪！");
                    return nextState(pig, PigStateType.Howling);
                }
            });

            // 猪嚎嚎着时被抽一鞭，没意思，开始睡觉
            addRule(PigStateType.Howling, PigEventType.Lash, new IAction<String, Integer>() {
                @Override
                public NextState exec(State<Integer> state, IFsmType fsmType, String target, IEventType etype, Object edata,
                        IActor<String> pig) {
                    System.out.println(state + " x " + etype + ": 喵。。。");
                    return nextState(pig, PigStateType.Sleeping);
                }
            });
            // 猪嚎嚎着时被摸一摸，开始哼哼
            addRule(PigStateType.Howling, PigEventType.Pat, new IAction<String, Integer>() {
                @Override
                public NextState exec(State<Integer> state, IFsmType fsmType, String target, IEventType etype, Object edata,
                        IActor<String> pig) {
                    System.out.println(state + " x " + etype + ": 爽，再摸摸");
                    return nextState(pig, PigStateType.Humming);
                }
            });
            // 猪嚎嚎着时被喂一喂，开吃
            addRule(PigStateType.Howling, PigEventType.Feed, new IAction<String, Integer>() {
                @Override
                public NextState exec(State<Integer> state, IFsmType fsmType, String target, IEventType etype, Object edata,
                        IActor<String> pig) {
                    Food food = (Food)edata;
                    if (food == null || food.amount <= 0) {
                        System.out.println(state + " x " + etype + ": 妈的，别哄老子");
                        return nextState(pig, PigStateType.Howling);
                    }
                    food.take(10);
                    System.out.println(state + " x " + etype + ": 爽，再来点");
                    return nextState(pig, PigStateType.Eating);
                }
            });

            // 猪睡着时你干嘛都接着睡……
            addRule(PigStateType.Sleeping, PigEventType.Pat, new IAction<String, Integer>() {
                @Override
                public NextState exec(State<Integer> state, IFsmType fsmType, String target, IEventType etype, Object edata,
                        IActor<String> pig) {
                    System.out.println(state + " x " + etype + ": 爽，我接着睡");
                    return nextState(pig, PigStateType.Sleeping);
                }
            });
            // 猪睡着时你干嘛都接着睡…… 当然有吃的除外！
            addRule(PigStateType.Sleeping, PigEventType.Feed, new IAction<String, Integer>() {
                @Override
                public NextState exec(State<Integer> state, IFsmType fsmType, String target, IEventType etype, Object edata,
                        IActor<String> pig) {
                    Food food = (Food)edata;
                    if (food == null || food.amount <= 0) {
                        System.out.println(state + " x " + etype + ": 妈的，别哄老子");
                        return nextState(pig, PigStateType.Howling);
                    }
                    food.take(10);
                    System.out.println(state + " x " + etype + ": 爽，再来点");
                    return nextState(pig, PigStateType.Eating);
                }
            });
            // 猪睡着时你干嘛都接着睡…… 当然抽鞭子例外！
            addRule(PigStateType.Sleeping, PigEventType.Lash, new IAction<String, Integer>() {
                @Override
                public NextState exec(State<Integer> state, IFsmType fsmType, String target, IEventType etype, Object edata,
                        IActor<String> pig) {
                    System.out.println(state + " x " + etype + ": 汪汪汪！");
                    return nextState(pig, PigStateType.Howling);
                }
            });

            // 猪吃着时你干嘛都接着吃……
            addRule(PigStateType.Eating, PigEventType.Pat, new IAction<String, Integer>() {
                @Override
                public NextState exec(State<Integer> state, IFsmType fsmType, String target, IEventType etype, Object edata,
                        IActor<String> pig) {
                    System.out.println(state + " x " + etype + ": 爽，我接着吃");
                    return nextState(pig, PigStateType.Eating);
                }
            });
            // 猪吃着时你干嘛都接着吃……
            addRule(PigStateType.Eating, PigEventType.Feed, new IAction<String, Integer>() {
                @Override
                public NextState exec(State<Integer> state, IFsmType fsmType, String target, IEventType etype, Object edata,
                        IActor<String> pig) {
                    Food food = (Food)edata;
                    if (food == null || food.amount <= 0) {
                        System.out.println(state + " x " + etype + ": 妈的，别哄老子");
                        return nextState(pig, PigStateType.Howling);
                    }
                    food.take(10);
                    System.out.println(state + " x " + etype + ": 爽，再来点");
                    return nextState(pig, PigStateType.Eating);
                }
            });
            // 猪吃着时你干嘛都接着吃…… 毫无例外！
            addRule(PigStateType.Eating, PigEventType.Lash, new IAction<String, Integer>() {
                @Override
                public NextState exec(State<Integer> state, IFsmType fsmType, String target, IEventType etype, Object edata,
                        IActor<String> pig) {
                    System.out.println(state + " x " + etype + ": 汪汪汪！我接着吃");
                    return nextState(pig, PigStateType.Eating);
                }
            });

            // 猪哼哼累了就想吃，想吃了就嚎
            addTimeoutRule(PigStateType.Humming, new IAction<String, Integer>() {
                @Override
                public NextState exec(State<Integer> state, IFsmType fsmType, String target, IEventType originEtype, Object data,
                        IActor<String> pig) {
                    System.out.println(state + " x " + originEtype + ": 饿死我了！");
                    return nextState(pig, PigStateType.Howling, 2, TimeUnit.SECONDS);
                }
            });
            // 猪嚎嚎累了就睡
            addTimeoutRule(PigStateType.Howling, new IAction<String, Integer>() {
                @Override
                public NextState exec(State<Integer> state, IFsmType fsmType, String target, IEventType originEtype, Object data,
                        IActor<String> pig) {
                    System.out.println(state + " x " + originEtype + ": 累死了。。睡会再嚎");
                    return nextState(pig, PigStateType.Sleeping, 2, TimeUnit.SECONDS);
                }
            });
            // 猪睡太久了肯定要找吃的
            addTimeoutRule(PigStateType.Sleeping, new IAction<String, Integer>() {
                @Override
                public NextState exec(State<Integer> state, IFsmType fsmType, String target, IEventType originEtype, Object data,
                        IActor<String> pig) {
                    System.out.println(state + " x " + originEtype + ": 噢噢噢有吃的吗！");
                    return nextState(pig, PigStateType.Howling, 2, TimeUnit.SECONDS);
                }
            });
            // 猪吃多了会撑，然后就睡觉
            addTimeoutRule(PigStateType.Eating, new IAction<String, Integer>() {
                @Override
                public NextState exec(State<Integer> state, IFsmType fsmType, String target, IEventType originEtype, Object data,
                        IActor<String> pig) {
                    System.out.println(state + " x " + originEtype + ": 撑死了，这才是生活");
                    return nextState(pig, PigStateType.Sleeping, 2, TimeUnit.SECONDS);
                }
            });
        }
    }

    public static class Food {
        float amount;

        public Food(float amount) {
            this.amount = amount;
        }
        public String toString() {
            return "Food:" + amount + "kg";
        }

        public Food take(float amount) {
            this.amount -= amount;
            return this;
        }

        public Food fill(float amount) {
            this.amount += amount;
            return this;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        ActorConfig<String, String> config = ActorConfig.builder("DemoV1", DefaultActorRouter.stringInstance(),
                DefaultFsmRouter.stringInstance(), new PigRuleSet())
                .actorNumber(1).initialFsmCount(1).maxFsmCount(1).build();
        ActorGroup<String, String> pigs = new ActorGroup<String, String>(config);

        pigs.start();
        Food food = new Food(10);
        pigs.tell("猪八八", DefaultFsmType.One, "猪八八", PigEventType.Feed, food); Thread.sleep(1000);
        pigs.tell("猪八八", DefaultFsmType.One, "猪八八", PigEventType.Feed, food); Thread.sleep(1000);
        Thread.sleep(3000);
        pigs.tell("猪八八", DefaultFsmType.One, "猪八八", PigEventType.Lash, null); Thread.sleep(1000);
        pigs.tell("猪八八", DefaultFsmType.One, "猪八八", PigEventType.Lash, null); Thread.sleep(1000);
        pigs.tell("猪八八", DefaultFsmType.One, "猪八八", PigEventType.Pat,  null); Thread.sleep(1000);
        pigs.tell("猪八八", DefaultFsmType.One, "猪八八", PigEventType.Feed, null); Thread.sleep(1000);
        pigs.shutdown();
        Thread.sleep(10000);
    }
}
