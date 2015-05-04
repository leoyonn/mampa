/**
 *
 * IMampaFuture.java
 * @date 4/1/15 15:09
 * @author leo [leoyonn@gmail.com]
 * [CopyRight] All Rights Reserved.
 */

package me.lyso.mampa.event;

import org.apache.commons.lang3.tuple.Pair;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Interface for simple future for mampa result return.
 *
 * @author leo
 */
public interface IMampaFuture<T> {
    /**
     * Get result from mampa, wait until result is ready.
     *
     * @return
     * @throws InterruptedException
     */
    Pair<T, String> get() throws InterruptedException;

    /**
     * Get result from mampa, wait until result is ready for at most #timeout #unit.
     * If timeout exceeds, just return a result with null value and "Timeout" error message.
     *
     * @param timeout
     * @param unit
     * @return
     * @throws InterruptedException
     */
    Pair<T, String> get(long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * Simple future for mampa result return.
     *
     * @param <T>
     * @author leo
     */
    public static class MampaFuture<T> implements IMampaFuture<T> {
        private static final Pair<?, String> TimeoutResult = Pair.of(null, "Timeout");
        private final Semaphore s = new Semaphore(0);
        private T result;
        private String error;

        /**
         * Call release in mampa thread when result is ready.
         *
         * @param result
         */
        protected void release(T result) {
            this.result = result;
            s.release();
        }

        /**
         * Call release in mampa thread when you know this operation fails.
         *
         * @param message
         */
        protected void release(String message) {
            this.error = message;
            s.release();
        }

        @Override
        public Pair<T, String> get() throws InterruptedException {
            s.acquire();
            return Pair.of(result, error);
        }

        @Override
        public Pair<T, String> get(long timeout, TimeUnit unit) throws InterruptedException {
            if (!s.tryAcquire(timeout, unit)) {
                //noinspection unchecked
                return (Pair<T, String>) TimeoutResult;
            }
            return Pair.of(result, error);
        }
    }
}
