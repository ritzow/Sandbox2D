package ritzow.solomon.engine.util;

public final class MutableInteger {
    private int value;
    
    public MutableInteger(int value) {
        this.value = value;
    }
    
    public void set(int value) {
        this.value = value;
    }
    
    public int intValue() {
        return value;
    }
}