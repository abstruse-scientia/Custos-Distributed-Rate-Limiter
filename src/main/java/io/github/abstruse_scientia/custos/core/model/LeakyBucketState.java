package io.github.abstruse_scientia.custos.core.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LeakyBucketState {

    private double tokens;
    private long lastLeakTime;

    public LeakyBucketState() {
        this.tokens = 0;
        this.lastLeakTime = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return "LeakBucketSate{ " +
                "tokens=" + tokens + ", " +
                "lastLeakTime=" + lastLeakTime +
                "}";
    }
}
