package com.github.modelflat.nit3.jpeg.huffman;

abstract class HuffmanTree implements Comparable<HuffmanTree> {
    final int frequency;

    HuffmanTree(int freq) {
        frequency = freq;
    }

    public int compareTo(HuffmanTree tree) {
        return frequency - tree.frequency;
    }
}
