 /**
 *
 * NamedSingleThreadPool.java
 * @date 14-7-12 上午11:53
 * @author leo [leoyonn@gmail.com]
 * [CopyRight] All Rights Reserved.
 */
 package me.lyso.mampa.utils;

 import java.util.concurrent.ThreadFactory;
 import java.util.concurrent.atomic.AtomicInteger;

 /**
  * Same as {@link java.util.concurrent.Executors#defaultThreadFactory()} except that you can set you thread prefix.
  *
  * @author leo
  */
 public class NamedThreadFactory implements ThreadFactory {
     final ThreadGroup group;
     final AtomicInteger threadNumber = new AtomicInteger(1);
     final String prefix;
     final int priority;


     public NamedThreadFactory(String name) {
         this(name, Thread.NORM_PRIORITY);
     }

     public NamedThreadFactory(String name, int priority) {
        SecurityManager s = System.getSecurityManager();
        group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
        this.prefix = name + "-";
        this.priority = priority;
    }

    public Thread newThread(Runnable r) {
        Thread t = new Thread(group, r, prefix + threadNumber.getAndIncrement(), 0);
        if (t.isDaemon()) {
            t.setDaemon(false);
        }
        if (t.getPriority() != priority) {
            t.setPriority(priority);
        }
        return t;
    }
}
