package com.github.modelflat.nit3.jpeg.huffman;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

public class Huffman {

    private HuffmanTree tree;
    private Map<Integer, Integer> alphabet;
    private Map<Integer, Integer> sizes;

    public Huffman(Map<Integer, Integer> alphabet) {
        PriorityQueue<HuffmanTree> trees = new PriorityQueue<>();

        for (int key : alphabet.keySet()) {
            trees.offer(new HuffmanLeaf(alphabet.get(key), key));
        }

        while (trees.size() > 1) {
            trees.offer(new HuffmanNode(trees.poll(), trees.poll()));
        }
        this.alphabet = alphabet;
        sizes = new HashMap<>(alphabet.keySet().size());
        tree = trees.poll();
    }

    public int getSizeFor(int symbol) {
        if (!alphabet.containsKey(symbol) || alphabet.get(symbol) == 0) {
            throw new RuntimeException("Non-existing symbol requested: " + symbol);
        }

        if (sizes.containsKey(symbol)) {
            return sizes.get(symbol);
        }
        int value = doGetSizeFor(symbol, tree, 0);
        sizes.put(symbol, value);
        return value;
    }

    private int doGetSizeFor(int symbol, HuffmanTree next, int count) {
        if (next instanceof HuffmanLeaf) {
            return ((HuffmanLeaf) next).getValue() == symbol ? count : count - 1;
        } else {
            int resultLeft = doGetSizeFor(symbol, ((HuffmanNode) next).getLeft(), count + 1);
            if (resultLeft > count) {
                return resultLeft;
            }
            int resultRight = doGetSizeFor(symbol, ((HuffmanNode) next).getRight(), count + 1);
            if (resultRight > count) {
                return resultRight;
            }
        }
        return count - 1;
}

    public void printSizes() {
        for (int i : alphabet.keySet()) {
            if (alphabet.get(i) > 0) {
                System.out.printf("%d: %d (frequency: %d)\n", i, getSizeFor(i), alphabet.get(i));
            }
        }
    }
}