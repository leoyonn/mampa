/**
 *
 * Actors.java
 * @date 14-11-26 下午1:58
 * @author leo [leoyonn@gmail.com]
 * [CopyRight] All Rights Reserved.
 */

package me.lyso.mampa.actor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadFactory;

/**
 * Hold all actors and actor-groups in current process.
 * <p/>
 * Call  {@link #current()} / {@link #currentGroup()} to retrieve which actor/actor-group u r in.
 *
 * @author leo
 */
public final class Actors {
    private static final Logger logger = LoggerFactory.getLogger(Actors.class);
    private static final ConcurrentHashMap<String, IActor> actorPool = new ConcurrentHashMap<String, IActor>();
    private static final ConcurrentHashMap<String, IActorGroup> groupPool = new ConcurrentHashMap<String, IActorGroup>();

    private static final ThreadLocal<IActorGroup> groups = new ThreadLocal<IActorGroup>() {
        @Override
        protected IActorGroup initialValue() {
            String threadName = Thread.currentThread().getName();
            IActorGroup group = groupPool.get(threadName);
            logger.info("Initial value of group: {}: {}", threadName, group);
            return group;
        }

    };

    private static final ThreadLocal<IActor> actors = new ThreadLocal<IActor>() {
        @Override
        protected IActor initialValue() {
            String threadName = Thread.currentThread().getName();
            IActor actor = actorPool.get(threadName);
            logger.info("Initial value of actor: {}: {}", threadName, actor);
            return actor;
        }
    };

    /**
     * Get the current actor.
     *
     * @param <K2>
     * @return
     */
    public static <K2> IActor<K2> current() {
        //noinspection unchecked
        return (IActor<K2>) actors.get();
    }

    /**
     * Get the current actor-group.
     *
     * @param <K1>
     * @param <K2>
     * @return
     */
    public static <K1, K2> IActorGroup<K1, K2> currentGroup() {
        //noinspection unchecked
        return (IActorGroup<K1, K2>) groups.get();
    }

    protected static void add(IActorGroup group, IActor actor, String groupName, int groupIndex, int actorIndex) {
        groupPool.put(threadName(groupName, groupIndex, actorIndex), group);
        actorPool.put(threadName(groupName, groupIndex, actorIndex), actor);
        logger.info("Add new group {} and actor {}: name: {} groupIndex: {} actorIndex: {}", group, actor, groupName, groupIndex, actorIndex);
        logger.info("Group-pool: {}\nActor-pool: {}\n", groupPool, actorPool);
    }

    private static String threadName(String groupName, int groupIndex, int actorIndex) {
        return "MAMPA-" + groupName + "-G" + groupIndex + "-A" + actorIndex;
    }

    /**
     * Thread factory for Actor, One Thread factory can only generate ONLY ONE thread.
     *
     * @author leo
     */
    private static class ActorThreadFactory implements ThreadFactory {
        private final ThreadGroup threadGroup;
        private final String name;
        private final int priority;
        private volatile boolean exhausted = false;

        public ActorThreadFactory(String group, int groupIndex, int actorIndex) {
            this(group, groupIndex, actorIndex, Thread.NORM_PRIORITY);
        }

        public ActorThreadFactory(String groupName, int groupIndex, int actorIndex, int threadPriority) {
            SecurityManager s = System.getSecurityManager();
            this.threadGroup = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
            this.name = threadName(groupName, groupIndex, actorIndex);
            this.priority = threadPriority;
            logger.info("Create ActorThreadFactory of {} priority: {}.", name, priority);
        }

        public Thread newThread(Runnable r) {
            if (exhausted) {
                throw new IllegalStateException("ActorThreadFactory [" + name + "] should be used only once!");
            }
            exhausted = true;
            Thread t = new Thread(threadGroup, r, name, 0);
            logger.info("Create Thread of {} priority: {}.", name, priority);
            if (t.isDaemon()) {
                t.setDaemon(false);
            }
            if (t.getPriority() != priority) {
                t.setPriority(priority);
            }
            return t;
        }
    }

    /**
     * Create a thread factor for #group#actorIndex with thread#priority.
     *
     * @param group
     * @param groupIndex
     * @param actorIndex
     * @param threadPrirority
     * @return
     */
    protected static ThreadFactory threadFactory(String group, int groupIndex, int actorIndex, int threadPrirority) {
        return new ActorThreadFactory(group, groupIndex, actorIndex, threadPrirority);
    }

    /**
     * Create a thread factor for #group#actorIndex with {@link Thread#NORM_PRIORITY}.
     *
     * @param group
     * @param groupIndex
     * @param actorIndex
     * @return
     */
    protected static ThreadFactory threadFactory(String group, int groupIndex, int actorIndex) {
        return new ActorThreadFactory(group, groupIndex, actorIndex);
    }

}
