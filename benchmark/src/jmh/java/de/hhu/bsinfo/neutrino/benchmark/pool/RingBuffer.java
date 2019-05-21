package de.hhu.bsinfo.neutrino.benchmark.pool;

import java.lang.reflect.Array;

public class RingBuffer<T> {

    public static final int REMOVE_SIGN = 0x7FFFFFFF;

    private final T[] buffer;

    private int posBack;
    private int posFront;

    private static int getUnsignedInt(int number) {
        return number & REMOVE_SIGN;
    }

    public RingBuffer(final int size, Class<T> clazz) {
        buffer = (T[]) Array.newInstance(clazz, size);
    }

    public void push(final T object) {
        int front = getUnsignedInt(posFront);

        if(front != posBack + buffer.length) {
            buffer[front % buffer.length] = object;
            posFront++;
        }
    }

    public T pop() {
        int back = getUnsignedInt(posBack);

        if(back == posFront) {
            return null;
        }

        T ret = buffer[back % buffer.length];
        posBack++;

        return ret;
    }
}
