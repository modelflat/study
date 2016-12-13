package com.github.modelflat.nit3.jpeg.huffman;

class HuffmanNode extends HuffmanTree {
    private final HuffmanTree left;
    private final HuffmanTree right;

    HuffmanNode(HuffmanTree l, HuffmanTree r) {
        super(l.frequency + r.frequency);
        left = l;
        right = r;
    }

    HuffmanTree getLeft() {
        return left;
    }

    HuffmanTree getRight() {
        return right;
    }
}