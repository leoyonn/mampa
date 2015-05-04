/**
 * ActorGroup.java
 * [CopyRight]
 * @author leo [leoyonn@gmail.com]
 * @date Jan 22, 2014 1:16:36 PM
 */
package me.lyso.mampa.actor;

import me.lyso.mampa.actor.router.ActorSplitter;
import me.lyso.mampa.actor.router.IActorRouter;
import me.lyso.mampa.actor.router.IFsmRouter;
import me.lyso.mampa.event.IEventType;
import me.lyso.mampa.fsm.FSM;
import me.lyso.mampa.fsm.IFsmType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A group of {@link Actor}s which as save behavior, using #target to dispatch events to actors.
 *
 * @param <K1> :Type of target for routing to an {@link Actor} using {@link IActorRouter}.
 * @param <K2> :Type of target for routing to an {@link FSM} using {@link IFsmRouter}.
 * @author leo
 */
public class ActorGroup<K1, K2> implements IActorGroup<K1, K2>, IPriorityActorGroup<K1, K2> {
    protected static final Logger LOGGER = LoggerFactory.getLogger(ActorGroup.class);
    private static final AtomicInteger groupIndex = new AtomicInteger(0);
    private final IActorRouter<K1> actorRouter;
    private final int size;
    private final List<Actor<K2>> actors;

    public List<Actor<K2>> actors() {
        return actors;
    }

    /**
     * Constructor.
     *
     * @param config :Configuration for actor.
     */
    public ActorGroup(ActorConfig<K1, K2> config) {
        this.size = config.actorNumber();
        this.actors = new ArrayList<Actor<K2>>(size);
        this.actorRouter = config.actorRouter();
        int groupIndex = ActorGroup.groupIndex.getAndIncrement();
        try {
            for (int i = 0; i < size; i++) {
                Actor<K2> actor = new Actor<K2>(i, Actors.threadFactory(config.name(), groupIndex, i), config);
                actors.add(actor);
                Actors.add(this, actor, config.name(), groupIndex, i);
            }
        } catch (Exception ex) { // IllegalAccessException, InstantiationException
            throw new IllegalArgumentException("Init actor/actor-router/fsm-router got exception!", ex);
        }
        LOGGER.info("Built actors with config {}", config);
    }

    @Override
    public boolean tell(K1 actorTarget, IFsmType fsmType, K2 fsmTarget, IEventType etype, Object edata) {
        if (actorRouter.matchAllActor(actorTarget)) {
            boolean ok = true;
            for (Actor<K2> actor : actors) {
                ok &= actor.tell(fsmType, fsmTarget, etype, edata);
            }
            return ok;
        } else {
            return findActor(actorTarget).tell(fsmType, fsmTarget, etype, edata);
        }
    }

    @Override
    public boolean tellSync(K1 actorTarget, IFsmType fsmType, K2 fsmTarget, IEventType etype, Object edata) {
        if (actorRouter.matchAllActor(actorTarget)) {
            boolean ok = true;
            for (Actor<K2> actor : actors) {
                ok &= actor.tellSync(fsmType, fsmTarget, etype, edata);
            }
            return ok;
        } else {
            return findActor(actorTarget).tellSync(fsmType, fsmTarget, etype, edata);
        }
    }

    @Override
    public boolean batchTell(List<K1> actorTargets, IFsmType fsmType, List<K2> fsmTargets, IEventType etype, Object edata) {
        if (actorTargets == null || size == 1) {
            return actorTargets == null ? findActor(null).batchTell(fsmType, fsmTargets, etype, edata)
                    : findActor(actorTargets.get(0)).tell(fsmType, fsmTargets.get(0), etype, edata);
        } else if (actorTargets.size() != fsmTargets.size()) {
            throw new IllegalArgumentException("Actor targets size don't match Fsm targets size.");
        }
        List<K2>[] splittedFsmTargets = ActorSplitter.split(actorRouter, size, actorTargets, fsmTargets);
        boolean ok = true;
        for (int i = 0; i < size; i++) {
            if (splittedFsmTargets[i] != null && splittedFsmTargets.length > 0) {
                ok &= actors.get(i).batchTell(fsmType, splittedFsmTargets[i], etype, edata);
            }
        }
        return ok;
    }

    @Override
    public int fsmSize(IFsmType fsmType) {
        int size = 0;
        for (Actor actor : actors) {
            size += actor.fsmSize(fsmType);
        }
        return size;
    }

    @Override
    public int fsmSize() {
        int size = 0;
        for (Actor actor : actors) {
            size += actor.fsmSize();
        }
        return size;
    }

    @Override
    public boolean tell(int priority, K1 actorTarget, IFsmType fsmType, K2 fsmTarget, IEventType etype, Object edata) {
        if (actorRouter.matchAllActor(actorTarget)) {
            boolean ok = true;
            for (Actor<K2> actor : actors) {
                ok &= actor.tell(priority, fsmType, fsmTarget, etype, edata);
            }
            return ok;
        } else {
            return findActor(actorTarget).tell(priority, fsmType, fsmTarget, etype, edata);
        }
    }

    @Override
    public boolean tellSync(int priority, K1 actorTarget, IFsmType fsmType, K2 fsmTarget, IEventType etype, Object edata) {
        if (actorRouter.matchAllActor(actorTarget)) {
            boolean ok = true;
            for (Actor<K2> actor : actors) {
                ok &= actor.tellSync(priority, fsmType, fsmTarget, etype, edata);
            }
            return ok;
        } else {
            return findActor(actorTarget).tellSync(priority, fsmType, fsmTarget, etype, edata);
        }
    }

    @Override
    public boolean batchTell(int priority, List<K1> actorTargets, IFsmType fsmType, List<K2> fsmTargets, IEventType etype, Object edata) {
        if (actorTargets == null || size == 1) {
            return actorTargets == null ? findActor(null).batchTell(priority, fsmType, fsmTargets, etype, edata)
                    : findActor(actorTargets.get(0)).tell(priority, fsmType, fsmTargets.get(0), etype, edata);
        } else if (actorTargets.size() != fsmTargets.size()) {
            throw new IllegalArgumentException("Actor targets size don't match Fsm targets size.");
        }
        List<K2>[] splittedFsmTargets = ActorSplitter.split(actorRouter, size, actorTargets, fsmTargets);
        boolean ok = true;
        for (int i = 0; i < size; i++) {
            if (splittedFsmTargets[i] != null && splittedFsmTargets.length > 0) {
                ok &= actors.get(i).batchTell(priority, fsmType, splittedFsmTargets[i], etype, edata);
            }
        }
        return ok;
    }

    /**
     * Find corresponding Actor by target using {@link #actorRouter}.
     *
     * @param actorTarget
     * @return
     */
    private Actor<K2> findActor(K1 actorTarget) {
        int idx = actorRouter.route(actorTarget, size);
        if (idx >= size) {
            throw new IllegalStateException("Routed index " + idx + " should stay in [0, " + size + ")!");
        }
        return actors.get(idx);
    }

    /**
     * Start the actor.<BR>
     * MUST be called once and only once.
     *
     * @return
     */
    public ActorGroup<K1, K2> start() {
        for (Actor<K2> actor : actors) {
            actor.start();
        }
        LOGGER.info("{} started gracefully...", this);
        return this;
    }

    public void shutdown() {
        for (Actor<K2> actor : actors) {
            actor.shutdown();
        }
        LOGGER.info("{} shutdown gracefully...", this);
    }

    /**
     * Count of actors.
     *
     * @return
     */
    public int size() {
        return size;
    }

    @Override
    public String toString() {
        return "ActorGroup<" + getClass().getSimpleName() + ">#" + size;
    }
}
