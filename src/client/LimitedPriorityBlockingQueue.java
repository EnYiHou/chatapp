package client;

import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

public class LimitedPriorityBlockingQueue<T> extends PriorityBlockingQueue<T> {
    private final int capacity;
    
    public LimitedPriorityBlockingQueue(int capacity) {
        super(capacity);
        
        this.capacity = capacity;
    }
    
    public LimitedPriorityBlockingQueue(
        int capacity,
        Comparator<T> comparator
    ) {
        super(capacity, comparator);
        
        this.capacity = capacity;
    }
    
    private void makePlaceForNewItem() {
        while (this.size() >= this.capacity)
            this.remove();
    }

    @Override
    public boolean add(T e) {
        makePlaceForNewItem();
        return super.add(e);
    }

    @Override
    public boolean offer(T e, long timeout, TimeUnit unit) {
        makePlaceForNewItem();
        return super.offer(e, timeout, unit);
    }

    @Override
    public void put(T e) {
        makePlaceForNewItem();
        super.put(e);
    }

    @Override
    public boolean offer(T e) {
        makePlaceForNewItem();
        
        return super.offer(e);
    }
    
    @Override
    public int hashCode() {
        return this.stream().mapToInt(m -> m.hashCode()).sum();
    }
}
