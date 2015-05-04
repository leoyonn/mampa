/**
 * IEventType.java
 * [CopyRight]
 * @author leo [leoyonn@gmail.com]
 * @date Jan 14, 2014 9:11:47 PM
 */
package me.lyso.mampa.event;

import me.lyso.mampa.actor.Actor;

/**
 * Interface for event type sent to {@link Actor}s
 * <p>
 * Custom EventType implementing this SHOULD BE enum.
 * 
 * @author leo
 */
public interface IEventType {
    /**
     * Ordinal of this type, should be [0, values().length)
     *  
     * @return
     */
    int ordinal();

    public class IndexedEventType implements IEventType {
        public IndexedEventType() {ord = -1;}
        public IndexedEventType(int ord) {this.ord = ord;}

        public int ordinal() {return ord;}
        public void setOrdinal(int ord) {this.ord = ord;}

        private int ord;
    }
    
    /**
     * Example event type define.
     * 
     * @author leo
     */
    public static enum BaseEventType implements IEventType {
        Start,
        Stop,
        Redis,
        Hbase,
        Mysql,
        Timeout,
        ;

        public IEventType timeout() {
            return Timeout;
        }
    }
}
