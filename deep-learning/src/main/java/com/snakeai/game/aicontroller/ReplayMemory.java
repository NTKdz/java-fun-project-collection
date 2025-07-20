package com.snakeai.game.aicontroller;

import org.nd4j.linalg.api.ndarray.INDArray;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class ReplayMemory {

    private final int capacity;
    private final List<Experience> memory = new ArrayList<>();
    private final Random random = new Random();

    public ReplayMemory(int capacity) {
        this.capacity = capacity;
    }

    public void push(INDArray state, int[] action, int reward, INDArray nextState, boolean done) {
        if (memory.size() >= capacity) {
            memory.remove(0);
        }
        memory.add(new Experience(state, action, reward, nextState, done));
    }

    public List<Experience> sample(int batchSize) {
        int sampleSize = Math.min(batchSize, memory.size());
        List<Experience> batch = new ArrayList<>(memory);
        Collections.shuffle(batch, random);
        return batch.subList(0, sampleSize);
    }

    public int size() {
        return memory.size();
    }

    public record Experience(INDArray state, int[] action, int reward, INDArray nextState, boolean done) {}
}