package io.github.abstruse_scientia.custos.core.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SlidingWindowCounterState {
    long windowStartMinute;
    long previousWindowCount;
    long currentWindowCount;

    public SlidingWindowCounterState(long windowStartMinute) {
        this.windowStartMinute = windowStartMinute;
        this.previousWindowCount = 0;
        this.currentWindowCount = 0;
    }

}
