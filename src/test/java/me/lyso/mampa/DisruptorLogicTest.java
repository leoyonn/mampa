/**
 *
 * DisruptorLogicTest.java
 * @date 14-7-10 下午1:44
 * @author leo [leoyonn@gmail.com]
 * [CopyRight] All Rights Reserved.
 */

package me.lyso.mampa;

import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.EventTranslator;
import com.lmax.disruptor.dsl.Disruptor;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.Executors;

/**
 * @author leo
 */
@Ignore
public class DisruptorLogicTest {
    class Event {

    }
    @Test
    public void test() throws InterruptedException {
        final Disruptor<Event> d = new Disruptor<Event>(new EventFactory<Event>() {
            @Override
            public Event newInstance() {
                return new Event();
            }
        }, 8, Executors.newSingleThreadExecutor());
        d.handleEventsWith(new EventHandler<Event>() {
            @Override
            public void onEvent(Event event, long sequence, boolean endOfBatch) throws Exception {
                System.out.println(String.format("Sequence-consume: %d, Remains: %d", sequence, d.getRingBuffer().remainingCapacity()));
                Thread.sleep(20);
            }
        });
        d.start();
        for (int i = 0; i < 20; i ++) {
            d.publishEvent(new EventTranslator<Event>() {
                @Override
                public void translateTo(Event event, long sequence) {
                    System.out.println(String.format("Sequence-produce: %d, Remains: %d", sequence, d.getRingBuffer().remainingCapacity()));
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        Thread.sleep(3000);
    }
}
