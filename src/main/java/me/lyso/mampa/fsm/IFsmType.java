/**
 * IFsmType.java
 * [CopyRight]
 * @author leo [leoyonn@gmail.com]
 * @date Mar 13, 2014 1:43:55 PM
 */
package me.lyso.mampa.fsm;

/**
 * Type to distinguish different kind of FSM's in an Actor.
 * 
 * @author leo
 */
public interface IFsmType {
    int ordinal();

    public static enum DefaultFsmType implements IFsmType {
        One,
    }

    public class IndexedFsmType implements IFsmType {
        public IndexedFsmType() {ord = -1;}
        public IndexedFsmType(int ord) {this.ord = ord;}

        public int ordinal() {return ord;}
        public void setOrdinal(int ord) {this.ord = ord;}

        public void setFsmName(Class fsmName) {this.fsmName = fsmName;}

        @Override
        public String toString() {
            if (fsmName != null) {
                return fsmName.toString();
            }
            else {
                return "" + ord;
            }
        }

        private int ord;
        private Class fsmName;
    }
}
