/**
 * IStateType.java
 * [CopyRight]
 * @author leo [leoyonn@gmail.com]
 * @date Jan 14, 2014 6:06:27 PM
 */
package me.lyso.mampa.fsm;

/**
 * State type of an {@link FSM}. Should be enum.
 * 
 * @author leo
 */
public interface IStateType {
    int ordinal();

    /**
     * Default State type.
     * 
     * @author leo
     */
    public static enum BaseState implements IStateType {
        Init,
        Block,
        Idle,
        Dead;
    }
}
