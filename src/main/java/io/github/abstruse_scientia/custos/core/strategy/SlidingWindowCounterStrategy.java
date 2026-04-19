package io.github.abstruse_scientia.custos.core.strategy;

import io.github.abstruse_scientia.custos.core.config.RateLimitConfig;
import io.github.abstruse_scientia.custos.core.model.Algorithm;
import io.github.abstruse_scientia.custos.core.model.RateLimitDecision;
import io.github.abstruse_scientia.custos.core.model.SlidingWindowCounterState;
import io.github.abstruse_scientia.custos.core.store.RateLimitStore;

import static java.lang.Math.ceil;

public class SlidingWindowCounterStrategy implements RateLimiterStrategy {


    @Override
    public Algorithm getAlgorithm() {
        return Algorithm.SLIDING_WINDOW_COUNTER;
    }

    @Override
    public RateLimitDecision allow(String key, RateLimitConfig config, RateLimitStore store) {
        final RateLimitDecision[] decisionHolder = new RateLimitDecision[1];
        long currentMinute = System.currentTimeMillis() / 60000;
        store.atomicCompute(key, (k, currentState) -> {

            SlidingWindowCounterState state = currentState instanceof  SlidingWindowCounterState ?
                    (SlidingWindowCounterState) currentState : new SlidingWindowCounterState(currentMinute);

            if (state.getWindowStartMinute() != currentMinute) {
                //If current minute - 1 = previous window's minute
                //Then it can be said that current window has passed.
                if (state.getWindowStartMinute() == currentMinute - 1) {
                    state.setPreviousWindowCount(state.getCurrentWindowCount());
                }else {
                    // User went quite for more than a minute
                    state.setPreviousWindowCount(0);
                }

                state.setWindowStartMinute(currentMinute);
                state.setCurrentWindowCount(0);
            }

            long currentSecond = (System.currentTimeMillis() / 1000) % 60;
            double previousWindowWeight = (60 - currentSecond) / 60.0;
            long totalRequestEstimate = (long)(state.getPreviousWindowCount() * previousWindowWeight) +
                    state.getCurrentWindowCount();


            if (totalRequestEstimate >= config.getCapacity()) {
                long retryAfterSeconds;
                if (state.getCurrentWindowCount() >= config.getCapacity()) {
                    retryAfterSeconds = 60 - currentSecond;
                }else {
                    double requiredWeight = (double)
                            (config.getCapacity() - state.getCurrentWindowCount()) /
                            state.getPreviousWindowCount();

                    double targetSecond = 60 - (requiredWeight * 60);
                    retryAfterSeconds = (long) ceil(targetSecond - currentSecond) + 1;
                }
                decisionHolder[0] = new RateLimitDecision(false, retryAfterSeconds);
            }else {
                state.setCurrentWindowCount(state.getCurrentWindowCount() + 1);
                decisionHolder[0] = new RateLimitDecision(true, 0);
            }
            return state;
        });

        return decisionHolder[0];

    }

}
