/**
 * OneFsmRouter.java
 * [CopyRight]
 * @author leo [leoyonn@gmail.com]
 * @date Feb 19, 2014 2:21:54 PM
 */
package me.lyso.mampa.actor.router;

import me.lyso.mampa.fsm.FSM;

/**
 * Default {@link FSM} router to only 1 FSM.
 * 
 * @author leo
 */
public class OneFsmRouter<K> implements IFsmRouter<K> {
    private static final OneFsmRouter<String> stringInstance = new OneFsmRouter<String>();

    public static OneFsmRouter<String> stringInstance() {
        return stringInstance;
    }

    @Override
    public Object fsmId(K target) {
        return null;
    }

    @Override
    public boolean matchAllFsm(K target) {
        return false;
    }

    @Override
    @Deprecated
    public K getTargetIdForTask() {return null;}
}
