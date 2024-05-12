package com.example.googol.backend;

import java.util.LinkedList;
import java.util.Queue;

class URLQueue {

    private Queue<String> queue;

    public URLQueue() {
        this.queue = new LinkedList<>();
    }

    public synchronized void enqueue(String message) {
        queue.offer(message);
        notify();
    }

    public synchronized String dequeue() throws InterruptedException {
        while (queue.isEmpty()) {
            wait();
        }
        return queue.poll();
    }

    public synchronized boolean isEmpty() {
        return queue.isEmpty();
    }
}