/**
 *
 * ActorConfig.java
 * @date 14-7-4 上午11:40
 * @author leo [leoyonn@gmail.com]
 * [CopyRight] All Rights Reserved.
 */

package me.lyso.mampa.actor;

import com.lmax.disruptor.WaitStrategyType;
import me.lyso.mampa.actor.router.DefaultActorRouter;
import me.lyso.mampa.actor.router.IActorRouter;
import me.lyso.mampa.actor.router.IFsmRouter;
import me.lyso.mampa.fsm.AbstractRuleSet;
import me.lyso.mampa.fsm.FSM;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * name                 :ActorGroup/Actor's name.
 * actorNumber          :How many Actors in this Group.
 * actorRouter          :{@link IActorRouter} to route events to different {@link Actor}s.
 * fsmRouter            :{@link IActorRouter} to route events to different {@link FSM}s.
 * initialFsmCount      :Initialized FSM count per actor
 * maxFsmCount          :Max FSM count per actor
 * initialTimeoutCount  :Initialized timeout capacity per actor
 * maxTimeoutCount      :Max timeout capacity per actor
 * ringSizePerActor     :RingBuffer's size of an actor.
 * priorityConfigs      :Configurations for priority, each value is a count for batch consuming.
 * ruleSets             :Rule sets to run on this actor group.
 *
 * @author leo
 */
public class ActorConfig<K1, K2> {
    protected static final int DEFAULT_RING_SIZE = 1 << 12;
    protected static final int DEFAULT_INITIAL_FSM_COUNT = 1 << 5;
    protected static final int DEFAULT_MAX_FSM_COUNT = 1 << 10;
    protected static final int DEFAULT_WAIT_STRATEGY_TIME = 10; // MS

    /**
     * Used to generate Actor's context.
     *
     * @param <C>
     */
    public static interface ContextGenerator<C> {
        C generate(int actorIndex);
    }

    public static class Builder<K1, K2> {
        private ActorConfig<K1, K2> config;

        public Builder(String name) {
            config = new ActorConfig<K1, K2>();
            config.name = name;
        }

        public Builder<K1, K2> actorNumber(int actorNumber) {
            config.actorNumber = actorNumber;
            return this;
        }

        public Builder<K1, K2> actorRouter(IActorRouter<K1> actorRouter) {
            config.actorRouter = actorRouter;
            return this;
        }

        public Builder<K1, K2> fsmRouter(IFsmRouter<K2> fsmRouter) {
            config.fsmRouter = fsmRouter;
            return this;
        }

        public Builder<K1, K2> initialFsmCount(int initialFsmCount) {
            config.initialFsmCount = initialFsmCount;
            return this;
        }

        public Builder<K1, K2> maxFsmCount(int maxFsmCount) {
            config.maxFsmCount = maxFsmCount;
            return this;
        }

        public Builder<K1, K2> initialTimeoutCount(int initialTimeoutCount) {
            config.initialTimeoutCount = initialTimeoutCount;
            return this;
        }

        public Builder<K1, K2> maxTimeoutCount(int maxTimeoutCount) {
            config.maxTimeoutCount = maxTimeoutCount;
            return this;
        }

        public Builder<K1, K2> ringSizePerActor(int ringSizePerActor) {
            config.ringSizePerActor = ringSizePerActor;
            return this;
        }

        /**
         * For multi-priority, each priority queue has a ring size.
         *
         * @param ringSizesPerActor
         * @return
         */
        public Builder<K1, K2> ringSizesPerActor(int[] ringSizesPerActor) {
            config.ringSizesPerActor = ringSizesPerActor;
            return this;
        }

        public Builder<K1, K2> priorityConfigs(int[] priorityConfigs) {
            config.priorityConfigs = priorityConfigs;
            return this;
        }

        public Builder<K1, K2> registerContext(String name, Class<?> contextClazz) {
            if (config.contextClasses.put(name, contextClazz) != null) {
                throw new IllegalArgumentException("Duplicated context key: " + name);
            }
            return this;
        }

        public <C> Builder<K1, K2> registerContext(String name, ContextGenerator<C> generator) {
            if (config.contextGenerators.put(name, generator) != null) {
                throw new IllegalArgumentException("Duplicated context key: " + name);
            }
            return this;
        }

        public <C> Builder<K1, K2> registerContext(String name, C... contexts) {
            if (config.contexts.put(name, contexts) != null) {
                throw new IllegalArgumentException("Duplicated context key: " + name);
            }
            return this;
        }

        public Builder<K1, K2> ruleSets(AbstractRuleSet<K2, ?>[] ruleSets) {
            config.ruleSets = ruleSets;
            return this;
        }

        public Builder<K1, K2> waitStrategy(WaitStrategyType type) {
            config.waitStrategyType = type;
            return this;
        }

        public Builder<K1, K2> waitStrategyTime(long millis) {
            config.waitStrategyTime = millis;
            return this;
        }

        public Builder<K1, K2> uniqTimeout(long timeout, TimeUnit unit) {
            config.uniqTimeoutInNanos = unit.toNanos(timeout);
            return this;
        }

        public ActorConfig<K1, K2> build() {
            if (config.actorNumber == 0) {
                config.actorNumber = 1;
            }
            if (config.actorRouter == null) {
                config.actorRouter = new DefaultActorRouter<K1>();
            }
            if (config.initialFsmCount == 0) {
                config.initialFsmCount = DEFAULT_INITIAL_FSM_COUNT;
            }
            if (config.maxFsmCount == 0) {
                config.maxFsmCount = DEFAULT_MAX_FSM_COUNT;
            }
            if (config.initialTimeoutCount == 0) {
                config.initialTimeoutCount = config.initialFsmCount;
            }
            if (config.maxTimeoutCount == 0) {
                config.maxTimeoutCount = config.maxFsmCount * 2;
            }
            if (config.ringSizePerActor == 0) {
                config.ringSizePerActor = DEFAULT_RING_SIZE;
            }
            if (config.priorityConfigs != null && config.priorityConfigs.length > 1) {
                if (config.ringSizesPerActor == null) {
                    config.ringSizesPerActor = new int[config.priorityConfigs.length];
                    Arrays.fill(config.ringSizesPerActor, config.ringSizePerActor);
                }
                if (config.ringSizesPerActor.length != config.priorityConfigs.length) {
                    throw new IllegalArgumentException("ring buffer size should be same as batch-per-rounds size");
                }
            }
            if (config.ringSizesPerActor == null) {
                config.ringSizesPerActor = new int[]{config.ringSizePerActor};
            }
            if (config.actorNumber <= 0) {
                throw new IllegalArgumentException("Actor number should be positive!");
            }
            if (!config.contextClasses.isEmpty()) {
                for (Map.Entry<String, Class<?>> e : config.contextClasses.entrySet()) {
                    Object[] contexts = new Object[config.actorNumber];
                    for (int i = 0; i < config.actorNumber; i++) {
                        try {
                            contexts[i] = e.getValue().newInstance();
                        } catch (Exception ex) { // throws IllegalAccessException, InstantiationException
                            throw new IllegalArgumentException("Instantiate actor context key: " + e.getKey(), ex);
                        }
                    }
                    Object[] exists = config.contexts.put(e.getKey(), contexts);
                    if (exists != null) {
                        throw new IllegalArgumentException("Duplicated actor context key: " + e.getKey());
                    }
                }
            }
            if (!config.contextGenerators.isEmpty()) {
                for (Map.Entry<String, ContextGenerator<?>> e : config.contextGenerators.entrySet()) {
                    Object[] contexts = new Object[config.actorNumber];
                    for (int i = 0; i < config.actorNumber; i++) {
                        try {
                            contexts[i] = e.getValue().generate(i);
                        } catch (Exception ex) {
                            throw new IllegalArgumentException("Instantiate actor context key: " + e.getKey(), ex);
                        }
                    }
                    Object[] exists = config.contexts.put(e.getKey(), contexts);
                    if (exists != null) {
                        throw new IllegalArgumentException("Duplicated actor context key: " + e.getKey());
                    }
                }
            }
            for (Map.Entry<String, Object[]> e : config.contexts.entrySet()) {
                if (e.getValue().length != config.actorNumber) {
                    throw new IllegalArgumentException("Invalid size for actor context key: " + e.getKey());
                }
            }
            if (config.ruleSets == null || config.ruleSets.length == 0) {
                throw new IllegalArgumentException("At least 1 ruleset!");
            }
            if (config.waitStrategyType == null) {
                config.waitStrategyType = WaitStrategyType.Blocking;
            }
            if (config.waitStrategyType == WaitStrategyType.Park && config.waitStrategyTime <= 0) {
                config.waitStrategyTime = DEFAULT_WAIT_STRATEGY_TIME;
            }
            int ruleSetNum = config.ruleSets.length;
            for (AbstractRuleSet<K2, ?> ruleSet : config.ruleSets) {
                if (ruleSet.getFsmType().ordinal() >= ruleSetNum) {
                    throw new IllegalArgumentException("FsmType's ordinal exceeds size!");
                }
            }
            return config;
        }
    }

    public static <K1, K2> Builder<K1, K2> builder(String name, IActorRouter<K1> actorRouter, IFsmRouter<K2> fsmRouter,
            AbstractRuleSet<K2, ?>... ruleSets) {
        return new Builder<K1, K2>(name).actorRouter(actorRouter).fsmRouter(fsmRouter).ruleSets(ruleSets);
    }

    private String name;
    private int actorNumber;
    private IActorRouter<K1> actorRouter;
    private IFsmRouter<K2> fsmRouter;
    private int initialFsmCount;
    private int maxFsmCount;
    private int initialTimeoutCount;
    private int maxTimeoutCount;
    private int ringSizePerActor;
    private int[] ringSizesPerActor;
    private int[] priorityConfigs;
    private AbstractRuleSet<K2, ?>[] ruleSets;
    private Map<String, Object[]> contexts;
    private Map<String, Class<?>> contextClasses;
    private Map<String, ContextGenerator<?>> contextGenerators;
    private WaitStrategyType waitStrategyType;
    private long uniqTimeoutInNanos;
    private long waitStrategyTime; // in millis

    private ActorConfig() {
        contexts = new HashMap<String, Object[]>();
        contextClasses = new HashMap<String, Class<?>>();
        contextGenerators = new HashMap<String, ContextGenerator<?>>();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

    public String name() {
        return name;
    }

    public int actorNumber() {
        return actorNumber;
    }

    public IActorRouter<K1> actorRouter() {
        return actorRouter;
    }

    public IFsmRouter<K2> fsmRouter() {
        return fsmRouter;
    }

    public int initialFsmCount() {
        return initialFsmCount;
    }

    public int maxFsmCount() {
        return maxFsmCount;
    }

    public int initialTimeoutCount() {
        return initialTimeoutCount;
    }

    public int maxTimeoutCount() {
        return maxTimeoutCount;
    }

    public int ringSizePerActor() {
        return ringSizePerActor;
    }

    public int[] ringSizesPerActor() {
        return ringSizesPerActor;
    }

    public int[] priorityConfigs() {
        return priorityConfigs;
    }

    public AbstractRuleSet<K2, ?>[] ruleSets() {
        return ruleSets;
    }

    public Map<String, Object[]> contexts() {
        return contexts;
    }

    public WaitStrategyType waitStrategy() {
        return waitStrategyType;
    }

    public long waitStrategyTime() {
        return waitStrategyTime;
    }

    public long uniqTimeoutInNanos() {
        return uniqTimeoutInNanos;
    }
}
