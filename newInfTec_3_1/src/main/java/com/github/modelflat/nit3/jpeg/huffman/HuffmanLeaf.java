package com.github.modelflat.nit3.jpeg.huffman;

class HuffmanLeaf extends HuffmanTree {
    private final int value;

    HuffmanLeaf(int freq, int val) {
        super(freq);
        value = val;
    }

    int getValue() {
        return value;
    }
}