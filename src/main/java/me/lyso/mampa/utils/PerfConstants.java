/**
 * PerfConstants.java
 * [CopyRight]
 * @author leo [leoyonn@gmail.com]
 * @date Mar 18, 2014 3:14:16 PM
 */
package me.lyso.mampa.utils;

/**
 * Constants for perf-counter.
 *
 * @author leo
 */
public interface PerfConstants {
    String PrefixMampa = "mampa~";

    String PrefixTransition = PrefixMampa + "transition~";
    String PrefixTransitionOk = PrefixTransition + "~ok~";
    String PrefixTransitionFail = PrefixTransition + "~fail~";
    String PrefixTransitionTimeoutMismatch = PrefixTransition + "~timeout~mismatch~";

    String PrefixTell = PrefixMampa + "tell";
    String PrefixTellFail = PrefixTell + "~fail";
    String PrefixTellAfterFail = PrefixTell + "~after~fail~";
    String PrefixTellPublishOk = PrefixTell + "~publish~ok~";
    String PrefixTellSyncOk = PrefixTell + "~sync~ok~";
    String PrefixTellOk = PrefixTell + "~ok~";
    String PrefixTellActorIndex = PrefixTell + "~actor~";

    String PrefixEvent = PrefixMampa + "event";
    String PrefixEventOk = PrefixEvent + "~ok~";
    String PrefixEventAllFsmOk = PrefixEvent + "~allfsm~ok~";
    String PrefixEventBatchFsmOk = PrefixEvent + "~batchfsm~ok~";
    String PrefixEventInQueueTime = PrefixEvent + "~inqueue~time~";
    String PrefixEventExeption = PrefixEvent + "~exception~";

    String PrefixFsm = PrefixMampa + "fsm";
    String PrefixFsmDismiss = PrefixFsm + "~dismiss~";
    String PrefixFsmLifeTime = PrefixFsm + "~lifetime~";
    String PrefixFsmReturnsFail = PrefixFsm + "~pool~returns~fail~";
    String PrefixFsmEnrollFail = PrefixFsm + "~enroll~fail~";
    String PrefixFsmEnrollOk = PrefixFsm + "~enroll~ok~";
    String PrefixFsmInitStop = PrefixFsm + "~init~stop~";
    String PrefixFsmInitFail = PrefixFsm + "~init~fail~";
    String PrefixFsmCountExceed = PrefixFsm + "~count~exceed~";
    String PrefixFsmBorrowFail = PrefixFsm + "~pool~borrow~fail~";
    String PrefixFsmMapSize = PrefixFsm + "~map~size~";
    String PrefixFsmPoolIdleSize = PrefixFsm + "~pool~idle~size~";
    String PrefixMailboxOccupiedSize = "mailbox~occupied~size~";
}
