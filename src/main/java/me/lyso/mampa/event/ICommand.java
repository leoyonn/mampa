/**
 *
 * Command.java
 * @date 14-11-4 下午7:33
 * @author leo [leoyonn@gmail.com]
 * [CopyRight] All Rights Reserved.
 */
package me.lyso.mampa.event;

import java.util.concurrent.*;

/**
 * Command sent to Mampa-Actor to execute and return result to caller.
 *
 * @author leo
 */
public interface ICommand<V> extends RunnableFuture<V> {
    ICommand<V> with(Object... args);

    public abstract static class Command<R> implements ICommand<R> {
        protected final IEventType type;
        protected final long uuid;
        protected final String res;
        protected final Exec<R> exec;
        protected final FutureTask<R> task;

        public Command(long uuid, String res, Exec<R> exec, IEventType type) {
            this.uuid = uuid;
            this.res = res;
            this.exec = exec;
            this.type = type;
            this.task = new FutureTask<R>(exec);
        }

        public long uuid() {
            return uuid;
        }

        public String res() {
            return res;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return task.cancel(mayInterruptIfRunning);
        }

        @Override
        public boolean isCancelled() {
            return task.isCancelled();
        }

        @Override
        public boolean isDone() {
            return task.isDone();
        }

        @Override
        public R get() throws InterruptedException, ExecutionException {
            return task.get();
        }

        @Override
        public R get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return task.get(timeout, unit);
        }

        @Override
        public void run() {
            task.run();
        }


        @Override
        public Command<R> with(Object... args) {
            exec.with(args);
            return this;
        }

        public IEventType type() {
            return type;
        }
    }

    public abstract static class Exec<R> implements Callable<R> {
        private Object[] args;
        private transient boolean argsSet = false;

        public Exec<R> with(Object... args) {
            this.args = args;
            this.argsSet = true;
            return this;
        }

        @Override
        public R call() throws Exception {
            if (!argsSet) {
                throw new IllegalStateException("Should call args(...) before run!");
            }
            return exec(args);
        }

        public abstract R exec(Object... args);
    }
}
