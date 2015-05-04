/**
 * DefaultFSMRouter.java
 * [CopyRight]
 * @author leo [leoyonn@gmail.com]
 * @date Feb 19, 2014 2:21:54 PM
 */
package me.lyso.mampa.actor.router;

import me.lyso.mampa.fsm.FSM;

/**
 * Default {@link FSM} router, just return this target, use hash-code as target to find an FSM.
 * 
 * @author leo
 */
public class DefaultFsmRouter<K> implements IFsmRouter<K> {

    public static final String AllTarget = "**";

    private static final DefaultFsmRouter<String> stringInstance = new DefaultFsmRouter<String>() {
        private long taskId = 0;
        @Override
        public String getTargetIdForTask() {
            return String.valueOf(taskId++);
        }
    };

    private static final DefaultFsmRouter<Long> longInstance = new DefaultFsmRouter<Long>() {
        private long taskId = 0;
        @Override
        public Long getTargetIdForTask() {
            return taskId++;
        }
    };

    private static final DefaultFsmRouter instance = new DefaultFsmRouter();

    public static DefaultFsmRouter<String> stringInstance() {
        return stringInstance;
    }

    public static DefaultFsmRouter<Long> longInstance() {
        return longInstance;
    }

    @SuppressWarnings("unchecked")
    public static <K1> DefaultFsmRouter<K1> instance() {
        return instance;
    }

    @Override
    public Object fsmId(K target) {
        return target;
    }

    @Override
    public boolean matchAllFsm(K target) {
        return AllTarget.equals(target);
    }

    @Override
    public K getTargetIdForTask() {
        throw new UnsupportedOperationException("getTargetIdForTask not implemented in DefaultFsmRouter.");
    }
}
