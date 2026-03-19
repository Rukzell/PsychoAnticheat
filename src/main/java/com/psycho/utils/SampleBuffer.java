package com.psycho.utils;

import java.util.ArrayDeque;
import java.util.Deque;

public class SampleBuffer {
    private final Deque<Float> values = new ArrayDeque<>();
    private final int maxSize;

    public SampleBuffer(int maxSize) {
        this.maxSize = maxSize;
    }

    public void add(float value) {
        if (values.size() >= maxSize) {
            values.pollFirst();
        }
        values.addLast(value);
    }

    public Deque<Float> getValues() {
        return values;
    }

    public boolean isFull() {
        return values.size() >= maxSize;
    }
}