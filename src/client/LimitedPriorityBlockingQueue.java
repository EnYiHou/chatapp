package client;

import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

public class LimitedPriorityBlockingQueue<T> extends PriorityBlockingQueue<T> {
    private final int capacity;
    private final Runnable notifier;
    
    public LimitedPriorityBlockingQueue(int capacity) {
        super(capacity);
        
        this.capacity = capacity;
        this.notifier = null;
    }
    
    public LimitedPriorityBlockingQueue(
        int capacity,
        Comparator<T> comparator
    ) {
        super(capacity, comparator);
        
        this.capacity = capacity;
        this.notifier = null;
    }
    
    public LimitedPriorityBlockingQueue(
        int capacity,
        Comparator<T> comparator,
        Runnable notifier
    ) {
        super(capacity, comparator);
        
        this.capacity = capacity;
        this.notifier = notifier;
    }
    
    private void prepareAdd() {
        while (this.size() >= this.capacity)
            this.remove();
        
        if (this.notifier != null)
            this.notifier.run();
    }

    @Override
    public boolean add(T e) {
        prepareAdd();
        return super.add(e);
    }

    @Override
    public boolean offer(T e, long timeout, TimeUnit unit) {
        prepareAdd();
        return super.offer(e, timeout, unit);
    }

    @Override
    public void put(T e) {
        prepareAdd();
        super.put(e);
    }

    @Override
    public boolean offer(T e) {
        prepareAdd();
        
        return super.offer(e);
    }
}
