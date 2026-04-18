package io.github.abstruse_scientia.custos.core.model;

import lombok.Getter;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SlidingWindowState {

    @Getter
    private final Queue<Long> timestamps = new ConcurrentLinkedQueue<>();



    @Override
    public String toString() {
        return "SlidingWindowState{" + "timestamps=" + timestamps + '}';
    }

}
