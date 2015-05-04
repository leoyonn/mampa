/**
 * State.java
 * [CopyRight]
 * @author leo [leoyonn@gmail.com]
 * @date Feb 13, 2014 10:46:47 AM
 */
package me.lyso.mampa.fsm;

import io.netty.util.Timeout;

/**
 * State of an {@link FSM}. inner user-defined {@link #value} can be used for more complicated situations.
 *
 * @author leo
 */
public class State<V> {
    private IStateType type;
    private V value;
    private Timeout timeout;

    public State(IStateType type, V v) {
        this.type = type;
        this.value = v;
    }

    public IStateType type() {
        return type;
    }

    public boolean cancelTimeout() {
        boolean canceled = false;
        if (timeout != null) {
            canceled = timeout.cancel();
            timeout = null;
        }
        return canceled;
    }

    public State<V> timeout(Timeout timeout) {
        this.timeout = timeout;
        return this;
    }

    public State<V> type(IStateType type) {
        if (type == null) {
            throw new IllegalArgumentException("State type should not be null!");
        }
        this.type = type;
        return this;
    }

    public V value() {
        return value;
    }

    public State<V> value(V value) {
        this.value = value;
        return this;
    }

    public void clear() {
        value = null;
        type = null;
        timeout = null;
    }

    @Override
    public String toString() {
        return "State<" + type + "|" + value + ">";
    }
}
