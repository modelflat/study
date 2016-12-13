package com.github.modelflat.nit3.jpeg;

import java.util.Iterator;

public class SerpentIterator implements Iterator<Float>, Iterable<Float> {

    private final float[][] array;
    private int linearCoord = 1;
    private int localI = 0;
    private int currentI = 0;
    private int localJ = 0;
    private int currentJ = 0;
    private int iterations = 1;

    private boolean asc = true;

    SerpentIterator(float[][] array) {
        this.array = array;
    }

    @Override
    public boolean hasNext() {
        return linearCoord < 65;
    }

    @Override
    public Float next() {
        if (hasNext()) {
            float value = asc ? array[localJ][localI] : array[localI][localJ];
            localJ--;
            localI++;
            linearCoord++;
            if (localI >= iterations) {
                endLine();
            }
            return value;
        }
        return null;
    }

    private void endLine() {
        if (currentI < 7) {
            currentI++;
            iterations++;
        }
        else {
            currentJ++;
        }
        asc = !asc;
        localJ = currentI;
        localI = currentJ;
    }

    @Override
    public Iterator<Float> iterator() {
        return this;
    }
}
