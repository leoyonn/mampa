/**
 * Actor.java
 * [CopyRight]
 * @author leo [leoyonn@gmail.com]
 * @date Jan 14, 2014 5:16:42 PM
 */
package me.lyso.mampa.actor;

import com.lmax.disruptor.*;
import com.lmax.disruptor.TimeoutException;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.MultiPriorityDisruptor;
import com.lmax.disruptor.dsl.ProducerType;
import io.netty.util.Timeout;
import me.lyso.mampa.actor.router.IFsmRouter;
import me.lyso.mampa.event.Event;
import me.lyso.mampa.event.Event.BatchType;
import me.lyso.mampa.event.Event.EventFactory;
import me.lyso.mampa.event.IEventType;
import me.lyso.mampa.fsm.*;
import me.lyso.mampa.fsm.IFsmType.IndexedFsmType;
import me.lyso.mampa.timer.*;
import me.lyso.perf.Perf;
import me.lyso.pool.IPool;
import me.lyso.pool.PoolFactory;
import me.lyso.pool.PoolableObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

import static me.lyso.mampa.utils.PerfConstants.*;

/**
 * <ul>
 * <li>An Actor runs on 1 Thread, exclusively.
 * <li>An Actor has many {@link FSM}s.
 * <li>An Actor receives events by {@link #tell(IFsmType, Object, IEventType, Object)}  or
 * {@link #tellSync(IFsmType, Object, IEventType, Object)}, and finds corresponding {@link FSM} to process.
 * </ul>
 *
 * @param <K2> :Type of target for routing to an {@link FSM} using {@link IFsmRouter}.
 * @author leo
 * @see ActorGroup
 */
public class Actor<K2> implements EventHandler<Event<K2>>, TimeoutEventHandler, IActor<K2> {
    protected static final Logger logger = LoggerFactory.getLogger(Actor.class);

    private final int index;
    private final String name;
    private final IFsmRouter<K2> fsmRouter;
    private final FsmBox<K2>[] fsmBoxes;
    private final int maxFsmCount;
    private final NextState nextState;
    private final ExecutorService eventHandleExecutor;
    private final Disruptor<Event<K2>> singleDisruptor;
    private final MultiPriorityDisruptor<Event<K2>> multiDisruptor;
    private final int prioritySize;
    private final QueueTimer timer;
    private final Map<String, Object> contexts;

    private final String[] PerfMailboxRemainingSize;
    private final String[] PerfTellActor;
    private final String[] PerfTellSyncOk;
    private final String[] PerfTellFail;
    private final String[] PerfTellOk;
    private final String[] PerfTellPublishOk;
    private final String[] PerfEventInQueueTime;
    private final String[] PerfEventOk;
    private final String[] PerfEventBatchFsmOk;
    private final String[] PerfEventAllFsmOk;
    private final String[] PerfEventExeption;

    private long asyncTaskFsmId = 0;
    private RingBuffer<Event<K2>> singleRingBuffer;

    public Actor(int actorIndex, ThreadFactory threadFactory, ActorConfig<?, K2> config) throws IllegalAccessException, InstantiationException {
        this.index = actorIndex;
        this.name = config.name();
        this.fsmRouter = config.fsmRouter();
        this.maxFsmCount = config.maxFsmCount();
        this.nextState = new NextState();
        this.fsmBoxes = FsmBox.create(config.ruleSets(), config.initialFsmCount(), config.maxFsmCount(), config.name(), index);
        if (config.uniqTimeoutInNanos() > 0) {
            this.timer = QueueTimer.createRingQueueTimer(config.initialTimeoutCount(), config.maxTimeoutCount(),
                    config.uniqTimeoutInNanos());
        } else {
            this.timer = QueueTimer.createHeapTimer(config.initialTimeoutCount(), config.maxTimeoutCount());
        }
        eventHandleExecutor = Executors.newSingleThreadExecutor(threadFactory);

        if (config.priorityConfigs() == null || config.priorityConfigs().length <= 1) {
            prioritySize = 1;
            singleDisruptor = new Disruptor<Event<K2>>(new EventFactory<K2>(), config.ringSizePerActor(),
                    eventHandleExecutor, ProducerType.MULTI,
                    TimerCheckWaitStrategy.create(this, timer, config.waitStrategy(), false, config.waitStrategyTime()));
            //noinspection unchecked
            singleDisruptor.handleEventsWith(this);
            multiDisruptor = null;
        } else {
            prioritySize = config.priorityConfigs().length;
            multiDisruptor = new MultiPriorityDisruptor<Event<K2>>(new EventFactory<K2>(), config.ringSizesPerActor(),
                    eventHandleExecutor, ProducerType.MULTI,
                    TimerCheckWaitStrategy.create(this, timer, config.waitStrategy(), true, config.waitStrategyTime()), config.priorityConfigs());
            multiDisruptor.handleEventsWith(this);
            singleDisruptor = null;
            singleRingBuffer = null;
        }
        this.contexts = new HashMap<String, Object>();
        for (Map.Entry<String, Object[]> e : config.contexts().entrySet()) {
            contexts.put(e.getKey(), e.getValue()[index]);
        }

        // Perf constants
        this.PerfMailboxRemainingSize = new String[prioritySize];
        this.PerfTellActor = new String[prioritySize];
        this.PerfTellSyncOk = new String[prioritySize];
        this.PerfTellFail = new String[prioritySize];
        this.PerfTellOk = new String[prioritySize];
        this.PerfTellPublishOk = new String[prioritySize];
        this.PerfEventInQueueTime = new String[prioritySize];
        this.PerfEventOk = new String[prioritySize];
        this.PerfEventBatchFsmOk = new String[prioritySize];
        this.PerfEventAllFsmOk = new String[prioritySize];
        this.PerfEventExeption = new String[prioritySize];

        for (int i = 0; i < prioritySize; i++) {
            String suffix = name + "~" + index + "/" + config.actorNumber() + ".P" + i,
                    suffixGauge = suffix + ".GAUGE";
            PerfMailboxRemainingSize[i] = (PrefixMailboxOccupiedSize + suffixGauge).intern();
            PerfTellActor[i] = (PrefixTellActorIndex + suffix).intern();
            PerfTellSyncOk[i] = (PrefixTellSyncOk + suffix).intern();
            PerfTellFail[i] = (PrefixTellFail + suffix).intern();
            PerfTellOk[i] = (PrefixTellOk + suffix).intern();
            PerfTellPublishOk[i] = (PrefixTellPublishOk + suffix).intern();
            PerfEventInQueueTime[i] = (PrefixEventInQueueTime + suffix).intern();
            PerfEventOk[i] = (PrefixEventOk + suffix).intern();
            PerfEventBatchFsmOk[i] = (PrefixEventBatchFsmOk + suffix).intern();
            PerfEventAllFsmOk[i] = (PrefixEventAllFsmOk + suffix).intern();
            PerfEventExeption[i] = (PrefixEventExeption + suffix).intern();
        }
    }

    /**
     * Start the actor.<BR>
     * MUST be called once and only once.
     *
     * @return
     */
    public Actor<K2> start() {
        if (prioritySize == 1) {
            singleRingBuffer = singleDisruptor.start();
            logger.info("Started singleDisruptor: {}.", singleDisruptor);
        } else {
            multiDisruptor.start();
            logger.info("Started multiDisruptor: {}.", multiDisruptor);
        }
        return this;
    }

    private FsmBox<K2> fsmBox(IFsmType fsmType) {
        if (fsmType.ordinal() >= fsmBoxes.length) {
            throw new IllegalArgumentException("FsmType-" + fsmType + "'s ordinal exceed rule-set's size!");
        }
        return fsmBoxes[fsmType.ordinal()];
    }

    private IFsmType map2FsmType(Class fsmName) {
        for (int i = 0; i < fsmBoxes.length; i++ ) {
            Class curFsmName = fsmBoxes[i].ruleSet.getRuleSetName();
            if (curFsmName != null && curFsmName.equals(fsmName))
                return fsmBoxes[i].ruleSet.getFsmType();
        }
        return null;
    }

    /**
     * Find corresponding {@link FSM} by target.
     *
     * @param box
     * @param target
     * @return the associated FSM, or null when it's absent
     */
    protected FSM<K2, ?> findFSM(FsmBox<K2> box, K2 target) {
        Object fsmId = fsmRouter.fsmId(target);
        return box.fsms.get(fsmId);
    }

    /**
     * Create a new FSM instance for specified target, may override existed one.
     *
     * @param box
     * @param target
     * @param etype
     * @param edata
     * @return newly added fsm.
     */
    @SuppressWarnings("unchecked")
    protected FSM<K2, ?> enroll(FsmBox<K2> box, K2 target, IEventType etype, Object edata) {
        Perf.set(box.PerfFsmMapSize, box.fsms.size());
        Perf.set(box.PerfFsmPoolIdleSize, box.fsmPool.idleCount());
        if ((box.size = box.fsms.size()) > maxFsmCount) {
            Perf.count(box.PerfFsmCountExceed);
            logger.warn("Max FSM count exceeded! {} / {}.", box.fsms.size(), maxFsmCount);
            return null;
        }

        long begin = Perf.begin();
        FSM<K2, ?> fsm = null;
        // 1. borrow fsm from pool
        try {
            fsm = box.fsmPool.borrow();
        } catch (Throwable ex) {
            // Fatal error, wait for system restoring.
            Perf.elapse(box.PerfFsmBorrowFail, begin);
            logger.error("Borrow FSM from pool get exception {}", ex);
            return null;
        }
        AbstractRuleSet<K2, Object> ruleSet = (AbstractRuleSet<K2, Object>) box.ruleSet;
        ((FSM<K2, Object>) fsm).ruleSet(ruleSet);
        boolean success = false;

        // 2. init this fsm
        try {
            success = ruleSet.init((State<Object>) fsm.state(), target, etype, edata, this);
        } catch (Throwable ex) {
            Perf.elapse(box.PerfFsmInitFail, begin);
            logger.error("Got exception while initializing with: " + etype + ".", ex);
            success = false;
        }

        // 3. put into fsms
        if (success && fsm.state().type().equals(box.ruleSet.getStopState())) {
            // Defense for illegal user codes.
            Perf.elapse(box.PerfFsmInitStop, begin);
            logger.info("New FSM {} meet stop state.", fsm);
            success = false;
        }
        if (success) {
            fsm.born(begin).target(target);
            box.fsms.put(fsmRouter.fsmId(target), fsm);
            box.size = box.fsms.size();
            logger.debug("Allocate new FSM {} with target {}.", fsm, target);
            Perf.elapse(box.PerfFsmEnrollOk, begin);
            return fsm;
        }

        // 4. on failure
        try {
            box.fsmPool.returns(fsm);
        } catch (Exception ex) {
            Perf.elapse(box.PerfFsmReturnsFail, begin);
            logger.error("Return FSM from pool get exception", ex);
        }
        Perf.elapse(box.PerfFsmEnrollFail, begin);
        return null;
    }

    /**
     * Release this FSM.
     *
     * @param box
     * @param target
     * @return
     */
    protected FSM<K2, ?> dismiss(FsmBox<K2> box, K2 target, Throwable cause) {
        Perf.count(box.PerfFsmDismiss);
        FSM<K2, ?> removed = box.fsms.remove(fsmRouter.fsmId(target));
        box.size = box.fsms.size();
        if (removed != null) {
            //noinspection unchecked
            ((AbstractRuleSet<K2, Object>) box.ruleSet).onStop(((FSM<K2, Object>) removed).state(), this, cause);
            Perf.elapse(box.PerfFsmLifeTime, removed.born());
            logger.debug("FSM {} of target {} has left.", removed, target);
            try {
                box.fsmPool.returns(removed);
            } catch (Exception ex) {
                Perf.count(box.PerfFsmReturnsFail);
                logger.error("Return FSM from pool get exception", ex);
            }
        }
        return removed;
    }

    /**
     * Do transition using the fsm found. #fsm should not be null.
     *
     * @param fsm
     * @param fsmType
     * @param eventType
     * @param data
     */
    private void doTransition(FsmBox<K2> box, FSM<K2, ?> fsm, IFsmType fsmType, IEventType eventType, Object data) {
        Throwable cause = fsm.transition(fsmType, eventType, data, this);
        if (fsm.state().type().equals(box.ruleSet.getStopState())) {
            dismiss(box, fsm.target(), cause);
        }
        logger.debug("Process result state: {}!", fsm);
    }

    @Override
    public boolean tell(IFsmType fsmType, K2 fsmTarget, IEventType etype, Object edata) {
        return tell(0, fsmType, fsmTarget, etype, edata);
    }

    @Override
    public boolean tellSync(IFsmType fsmType, K2 fsmTarget, IEventType etype, Object edata) {
        return tellSync(0, fsmType, fsmTarget, etype, edata);
    }

    @Override
    public Timeout dealAfter(IFsmType fsmType, K2 fsmTarget, IEventType etype, Object edata, long delay,
            TimeUnit timeUnit) {
        return timer.add(TimeoutEvent.create(fsmType, fsmTarget, etype, edata, delay, timeUnit));
    }

    @Override
    public void onExpiredEvents(List<TimeoutEvent<?>> events) {
        for (TimeoutEvent<?> e : events) {
            try {
                if (!e.isCancelled()) {
                    //noinspection unchecked
                    onEvent((Event<K2>) e, 0, true);
                }
            } catch (Exception ex) {
                logger.warn("Processing delayed event " + e + " got exception", ex);
            }
        }
    }

    @Override
    public boolean batchTell(IFsmType fsmType, List<K2> fsmTargets, IEventType etype, Object edata) {
        return batchTell(0, fsmType, fsmTargets, etype, edata);
    }

    @Override
    public boolean tell(int priority, IFsmType fsmType, K2 fsmTarget, IEventType etype, Object edata) {
        return publishToRingBuffer(priority, false, fsmType, fsmTarget, null, etype, edata);
    }

    @Override
    public boolean tellSync(int priority, IFsmType fsmType, K2 fsmTarget, IEventType etype, Object edata) {
        return publishToRingBuffer(priority, true, fsmType, fsmTarget, null, etype, edata);
    }

    @Override
    public boolean batchTell(int priority, IFsmType fsmType, List<K2> fsmTargets, IEventType etype, Object edata) {
        return publishToRingBuffer(priority, false, fsmType, null, fsmTargets, etype, edata);
    }

    @Override
    public NextState nextState(int priority, IStateType stateType, long delay, TimeUnit timeUnit) {
        return nextState.set(priority, stateType, delay, timeUnit);
    }

    @Override
    public NextState nextState(IStateType stateType, long delay, TimeUnit timeUnit) {
        return nextState(0, stateType, delay, timeUnit);
    }

    @Override
    public int index() {
        return index;
    }

    public int fsmSize(IFsmType fsmType) {
        return fsmBox(fsmType).size;
    }

    public int fsmSize() {
        int size = 0;
        for (FsmBox<K2> box : fsmBoxes) {
            size += box.size;
        }
        return size;
    }

    @Override
    public <C> C context(String name) {
        //noinspection unchecked
        return (C) contexts.get(name);
    }

    /**
     * Save event to ring buffer of the Actor.
     *
     * @param sync      :Synchronously or Asynchronously publish.
     * @param fsmType
     * @param fsmTarget
     * @param etype
     * @param edata
     * @return
     */
    private boolean publishToRingBuffer(int priority, boolean sync, IFsmType fsmType, K2 fsmTarget,
            List<K2> batchTargets, IEventType etype, Object edata) {
        long begin = Perf.begin();
        long sequence = 0;
        Perf.count(PerfTellActor[priority]);
        if (sync) {
            sequence = nextSequence(priority);
            Perf.elapse(PerfTellSyncOk[priority], begin);
        } else {
            try {
                sequence = tryNextSequence(priority);
            } catch (InsufficientCapacityException ex) {
                Perf.elapse(PerfTellFail[priority], begin);
                return false;
            }
        }
        Perf.elapse(PerfTellOk[priority], begin);
        Event<K2> e = null;
        try {
            e = getAtSequence(priority, sequence);
            if (batchTargets != null && batchTargets.size() > 0) {
                e.set(fsmType, batchTargets, etype, edata, begin, priority);
            } else {
                e.set(fsmType, fsmTarget, etype, edata, begin, priority);
            }
            logger.debug("{} publishing an event: {} to ringBuffer!", this, e);
        } finally {
            publishSequence(priority, sequence);
        }
        logger.debug("{} successfully published an event: {} to ringBuffer!", this, e);
        Perf.elapse(PerfTellPublishOk[priority], begin);
        return true;
    }

    private long nextSequence(int priority) {
        return prioritySize == 1 ? singleRingBuffer.next() : multiDisruptor.next(priority);
    }

    private long tryNextSequence(int priority) throws InsufficientCapacityException {
        return prioritySize == 1 ? singleRingBuffer.tryNext() : multiDisruptor.tryNext(priority);
    }

    private Event<K2> getAtSequence(int priority, long sequence) {
        return prioritySize == 1 ? singleRingBuffer.get(sequence) : multiDisruptor.get(priority, sequence);
    }

    private void publishSequence(int priority, long sequence) {
        if (prioritySize == 1) {
            Perf.set(PerfMailboxRemainingSize[priority], (int) (singleRingBuffer.getBufferSize() - singleRingBuffer.remainingCapacity()));
            singleRingBuffer.publish(sequence);
        } else {
            Perf.set(PerfMailboxRemainingSize[priority], (int) multiDisruptor.occupied(priority));
            multiDisruptor.publish(priority, sequence);
        }
    }

    @Override
    public void onEvent(Event<K2> e, long sequence, boolean endOfBatch) throws Exception {
        long begin = Perf.begin();
        int priority = e.priority();
        try {
            logger.debug("{} processing event {}!", this, e);
            Perf.elapse(PerfEventInQueueTime[priority], e.born());
            FsmBox<K2> box = fsmBox(e.fsmType());
            K2 target = e.fsmTarget();
            BatchType batchType = e.isBatch() ? BatchType.Batch : fsmRouter.matchAllFsm(target) ? BatchType.All : BatchType.One;
            switch (batchType) {
                case One: {
                    tryEnrollAndTransition(box, target, e);
                    Perf.elapse(PerfEventOk[priority], begin);
                    break;
                }
                case Batch: {
                    logger.debug("{} processing batch-fsm-event {}!", this, e);
                    for (K2 target1 : e.batchTargets()) {
                        tryEnrollAndTransition(box, target1, e);
                    }
                    Perf.elapse(PerfEventBatchFsmOk[priority], begin);
                    break;
                }
                case All: {
                    logger.info("{} processing all-fsm-event {}!", this, e);
                    List<FSM<K2, ?>> all = new ArrayList<FSM<K2, ?>>(box.fsms.values());
                    for (FSM<K2, ?> fsm : all) {
                        doTransition(box, fsm, e.fsmType(), e.type(), e.data());
                    }
                    Perf.elapse(PerfEventAllFsmOk[priority], begin);
                    break;
                }
            }
        } catch (Throwable ex) {
            logger.warn("{} processing event {} got exception: {} ", this, e, ex);
            Perf.count(PerfEventExeption[priority]);
        } finally {
            e.clear();
        }
    }

    private FSM<K2, ?> tryEnrollAndTransition(FsmBox<K2> box, K2 target, Event<K2> e) {
        FSM<K2, ?> fsm = findFSM(box, target);
        if (fsm == null) {
            fsm = enroll(box, target, e.type(), e.data());
        }
        if (fsm != null) {
            doTransition(box, fsm, e.fsmType(), e.type(), e.data());
        }
        return fsm;
    }


    @Override
    public String toString() {
        return getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());
    }

    public void shutdown() {
        List<Timeout> timeouts = timer.stop();
        for (Timeout t : timeouts) {
            t.cancel();
        }
        if (prioritySize == 1) {
            singleDisruptor.shutdown();
        } else {
            try {
                multiDisruptor.shutdown();
            } catch (TimeoutException ex) {
                logger.warn("Shutdown multi-disruptor got exception: {}", ex);
            }
        }
        eventHandleExecutor.shutdown();
    }


    /**
     * @author leo
     * @param <K2>
     */
    private static class FsmBox<K2> {
        private final AbstractRuleSet<K2, ?> ruleSet;
        private final Map<Object, FSM<K2, ?>> fsms;
        private final IPool<FSM<K2, ?>> fsmPool;

        private final String PerfFsmMapSize;
        private final String PerfFsmPoolIdleSize;

        private final String PerfFsmBorrowFail;
        private final String PerfFsmInitFail;
        private final String PerfFsmCountExceed;
        private final String PerfFsmInitStop;
        private final String PerfFsmEnrollOk;
        private final String PerfFsmEnrollFail;
        private final String PerfFsmReturnsFail;
        private final String PerfFsmDismiss;
        private final String PerfFsmLifeTime;

        private FsmBox(AbstractRuleSet<K2, ?> ruleSet, int initialFsmCount, int maxFsmCount, String actorName, int actorIndex) {
            this.ruleSet = ruleSet;
            this.fsms = new HashMap<Object, FSM<K2, ?>>(initialFsmCount);
            this.fsmPool = new PoolFactory<FSM<K2, ?>>(new PoolableObjectFactory<FSM<K2, ?>>() {
                @Override
                public FSM<K2, Object> alloc() throws Exception {
                    return new FSM<K2, Object>();
                }

                @Override
                public void passivate(FSM<K2, ?> fsm) throws Exception {
                    fsm.clear();
                }

                @Override
                public void destroy(FSM<K2, ?> fsm) throws Exception {
                    fsm.clear();
                }
            }, initialFsmCount, maxFsmCount).create();

            // Init perf constants
            String suffix = actorName + "~" + actorIndex + "~" + ruleSet.getFsmType(),
                    suffixGauge = suffix + ".GAUGE";
            this.PerfFsmReturnsFail = (PrefixFsmReturnsFail + suffix).intern();
            this.PerfFsmBorrowFail = (PrefixFsmBorrowFail + suffix).intern();
            this.PerfFsmEnrollOk = (PrefixFsmEnrollOk + suffix).intern();
            this.PerfFsmEnrollFail = (PrefixFsmEnrollFail + suffix).intern();
            this.PerfFsmInitFail = (PrefixFsmInitFail + suffix).intern();
            this.PerfFsmCountExceed = (PrefixFsmCountExceed + suffix).intern();
            this.PerfFsmInitStop = (PrefixFsmInitStop + suffix).intern();
            this.PerfFsmDismiss = (PrefixFsmDismiss + suffix).intern();
            this.PerfFsmLifeTime = (PrefixFsmLifeTime + suffix).intern();

            this.PerfFsmMapSize = (PrefixFsmMapSize + suffixGauge).intern();
            this.PerfFsmPoolIdleSize = (PrefixFsmPoolIdleSize + suffixGauge).intern();
        }

        private static <K2> FsmBox<K2>[] create(AbstractRuleSet<K2, ?>[] ruleSets, int initialFsmCount, int maxFsmCount,
                String actorName, int actorIndex) {
            int size = ruleSets.length;
            boolean rescan = false;
            //noinspection unchecked
            FsmBox<K2>[] boxes = (FsmBox<K2>[]) new FsmBox<?>[size];

            for (int i = 0; i < size; i++) {
                boxes[i] = null;
            }

            for (int i = 0; i < size; i++) {
                int ordinal = ruleSets[i].getFsmType().ordinal();
                if (ordinal < 0) {
                    rescan = true;
                    continue;
                }
                if (ordinal >= size || boxes[ordinal] != null) {
                    throw new IllegalArgumentException("FsmType's Ordinal illegal(duplicated or exceeded size)!");
                }
                boxes[ordinal] = new FsmBox<K2>(ruleSets[i], initialFsmCount, maxFsmCount, actorName, actorIndex);
            }

            if (rescan) {
                for (int i = 0; i < size; i++) {
                    int ordinal = ruleSets[i].getFsmType().ordinal();
                    if (ordinal < 0) {
                        IndexedFsmType indexedFsmType = (IndexedFsmType) ruleSets[i].getFsmType();
                        ordinal = 0;
                        // find the next unoccupied index number and use it as fsmtype's ordinal
                        while (boxes[ordinal] != null) ordinal++;
                        indexedFsmType.setOrdinal(ordinal);
                        indexedFsmType.setFsmName(ruleSets[i].getRuleSetName());
                        boxes[ordinal] = new FsmBox<K2>(ruleSets[i], initialFsmCount, maxFsmCount, actorName, actorIndex);
                    }
                }
            }

            return boxes;
        }

        private int size;
    }
}
