package io.github.abstruse_scientia.custos.core.model;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SlidingWindowState {

    private final Queue<Long> timestamps = new ConcurrentLinkedQueue<>();

    public Queue<Long> getTimestamps() {
        return timestamps;
    }

    @Override
    public String toString() {
        return "SlidingWindowState{" + "timestamps=" + timestamps + '}';
    }

}
