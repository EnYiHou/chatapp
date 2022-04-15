package protocol;

public class Deserialized<T> {
    private final int size;
    private final T value;
    
    public Deserialized(T value, int size) {
        this.size = size;
        this.value = value;
    }

    public int getSize() {
        return this.size;
    }

    public T getValue() {
        return this.value;
    }
}
