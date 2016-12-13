from util import *
import numpy as np
import struct


def expand_key(key):
    # TODO: add len check

    if type(key) == str:
        key = key.encode()

    c = 4
    r = 20
    L = np.array(struct.unpack(str(len(key) // 4) + "I", key), dtype=np.uint32)
    S = [P]
    t = 2 * (r + 1)
    for i in range(1, t):
        S.append(np.add(S[-1], Q))

    i = j = 0
    A = B = np.uint32(0)
    for k in range(3 * t):
        A = S[i] = rotl32(np.add(S[i], np.add(A, B)), 3)
        B = L[j] = rotl32(np.add(L[j], np.add(A, B)), int(np.add(A, B)))
        i = (i + 1) % t
        j = (j + 1) % c
    return S


def encrypt(block, S: np.array):

    A = np.add(block[0], S[0])
    B = np.add(block[1], S[1])
    for i in range(20):
        A = (np.add(rotl32(np.bitwise_xor(A, B), int(B)), S[2*i]))
        B = (np.add(rotl32(np.bitwise_xor(B, A), int(A)), S[2*i + 1]))
    return A, B


def decrypt(block, S: np.array):

    A = block[0]
    B = block[1]
    for i in range(20)[::-1]:
        B = np.bitwise_xor(rotr32(np.subtract(B, S[2*i + 1]), int(A)), A)
        A = np.bitwise_xor(rotr32(np.subtract(A, S[2*i]), int(B)), B)
    return np.subtract(A, S[0]), np.subtract(B, S[1])


















