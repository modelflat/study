import numpy as np

Q = np.uint32(0x9E3779B9)
P = np.uint32(0xB7E15163)


def rotl32(x, y):
    return np.uint32(np.bitwise_or(np.left_shift(x, (np.bitwise_and(y, 31))),
                                   np.right_shift(x, np.subtract(32, np.bitwise_and(y, 31)))))


def rotr32(x, y):
    return np.uint32(np.bitwise_or(np.right_shift(x, (np.bitwise_and(y, 31))),
                                   np.left_shift(x, np.subtract(32, np.bitwise_and(y, 31)))))


if __name__ == '__main__':
    print(rotl32(np.uint32(0x80000000), 1))
    print(rotr32(np.uint32(0x80000000), 1))