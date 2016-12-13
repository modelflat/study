import multiprocessing as mp
import struct
import threading as thd
import time
import os
from time import perf_counter as pc

import numpy as np

CPU_COUNT = mp.cpu_count()


def element_wise_xor(first, second):
    assert len(first) == len(second)
    return [np.bitwise_xor(f, s) for f, s in zip(first, second)]


def segmentize(size, thread_count, routine, seg_size=None):
    if size < thread_count ** 2 or (seg_size and size < seg_size * thread_count):
        return [thd.Thread(target=routine, args=(0, size, 0))]
    segment = size // thread_count
    return [thd.Thread(target=routine,
                       args=(i * segment, (i + 1) * segment if i < thread_count - 1 else size, i))
            for i in range(thread_count)]


def execute(threads):
    st = time.perf_counter()
    for thread in threads:
        thread.start()
    for thread in threads:
        thread.join()
    return time.perf_counter() - st


def cleanup_result(result):
    result_ = []
    result.sort(key=lambda e: e[0])
    for l in result:
        result_.extend(l[1])
    return result_


def generate_IV(fmt, nptype):
    return [nptype(x) for x in struct.unpack(fmt, os.urandom(struct.calcsize(fmt)))]


def generate_key(length):
    return os.urandom(length)


def _ECB(block_fn, data_blocks, key, **kwargs):
    result_lock = thd.Lock()
    result = []
    thread_count = kwargs.get("thread_count")

    def routine(begin, end, tag):
        #db = data_blocks.copy()
        #key_ = key.copy()
        #time = pc()
        t_result = [block_fn(data_blocks[i], key) for i in range(begin, end)]
        #print(int((pc() - time) * 1000))
        with result_lock:
            result.append((tag, t_result))

    threads = segmentize(len(data_blocks), thread_count, routine)
    execute(threads)
    return cleanup_result(result)


def _ECB_encrypt(enc_block_fn, data_blocks, key, **kwargs):
    return _ECB(enc_block_fn, data_blocks, key, **kwargs)


def _ECB_decrypt(dec_block_fn, data_blocks, key, **kwargs):
    return _ECB(dec_block_fn, data_blocks, key, **kwargs)


def _CBC_encrypt(enc_block_fn, data_blocks, key, **kwargs):
    IV = kwargs.get("IV")
    result = [IV]
    for block in data_blocks:
        result.append(enc_block_fn(element_wise_xor(result[-1], block), key))
    return result[1:]


def _CBC_decrypt(dec_block_fn, data_blocks, key, **kwargs):
    IV = kwargs.get("IV")
    result = []
    result_lock = thd.Lock()
    thread_count = kwargs.get("thread_count")

    def routine(begin, end, tag):
        if tag == 0:
            # handle first element
            first = [element_wise_xor(dec_block_fn(data_blocks[0], key), IV)]
            begin += 1
        else:
            first = []
        t_result = first + [element_wise_xor(dec_block_fn(data_blocks[i], key), data_blocks[i - 1])
                            for i in range(begin, end)]
        with result_lock:
            result.append((tag, t_result))

    threads = segmentize(len(data_blocks), thread_count, routine)
    execute(threads)
    return cleanup_result(result)


def _PCBC_encrypt(enc_block_fn, data_blocks, key, **kwargs):
    prev = kwargs.get("IV")
    result = []
    for block in data_blocks:
        tr = enc_block_fn(element_wise_xor(block, prev), key)
        result.append(tr[::])
        prev = element_wise_xor(block, tr)
    return result


def _PCBC_decrypt(dec_block_fn, data_blocks, key, **kwargs):
    prev = kwargs.get("IV")
    result = []
    for block in data_blocks:
        tr = element_wise_xor(dec_block_fn(block, key), prev)
        result.append(tr[::])
        prev = element_wise_xor(block, tr)
    return result


def _CFB_encrypt(enc_block_fn, data_blocks, key, **kwargs):
    IV = kwargs.get("IV")
    result = [IV]
    for block in data_blocks:
        result.append(element_wise_xor(enc_block_fn(result[-1], key), block))
    return result[1:]


def _CFB_decrypt(enc_block_fn, data_blocks, key, **kwargs):
    IV = kwargs.get("IV")
    result = []
    result_lock = thd.Lock()
    thread_count = kwargs.get("thread_count")

    def routine(begin, end, tag):
        if tag == 0:
            # handle first element
            first = [element_wise_xor(enc_block_fn(IV, key), data_blocks[0])]
            begin += 1
        else:
            first = []
        t_result = first + [element_wise_xor(enc_block_fn(data_blocks[i - 1], key), data_blocks[i])
                            for i in range(begin, end)]
        with result_lock:
            result.append((tag, t_result))

    threads = segmentize(len(data_blocks), thread_count, routine)
    execute(threads)
    return cleanup_result(result)


_OPMODES = {"ECB": (_ECB_encrypt, _ECB_decrypt),
            "CBC": (_CBC_encrypt, _CBC_decrypt),
            "PCBC": (_PCBC_encrypt, _PCBC_decrypt),
            "CFB": (_CFB_encrypt, _CFB_decrypt),
            # "OFB":(_OFB_encrypt, _OFB_decrypt),
            # "CTR":(_CTR_encrypt, _CTR_decrypt),
            }


def _raw(func, block_algo, block_format, data, dtype, key, IV, **kwargs):
    thread_count = kwargs.get("thread_count")

    if type(data) == bytes:
        data = blockify(data, block_format, dtype, thread_count)

    return func(block_algo, data, key, IV=IV, thread_count=thread_count)


def raw_decrypt(dec_block_algo: tuple, opmode, data, key, IV, thread_count=CPU_COUNT):
    """
    :param dec_block_algo: (decrypt_block_function, block_format)
    :param opmode: operation mode of decrypt
    :param data: data to decrypt
    :param key: key to be used in decrypt
    :param IV: Initialization vector
    :param thread_count: thread count for parallelizable algorhitms. CPU count will be used if omitted.
    :return: decrypted data (blocks format)
    """
    decrypt_func = _OPMODES.get(opmode, None)
    assert decrypt_func is not None
    decrypt_func = decrypt_func[1]
    return _raw(decrypt_func, dec_block_algo[0], dec_block_algo[1], data, dec_block_algo[2], key, IV, thread_count=thread_count)


def raw_encrypt(enc_block_algo: tuple, opmode, data, key, IV, thread_count=CPU_COUNT):
    """
    :param enc_block_algo: (encrypt_block_function, block_format)
    :param opmode: operation mode of encrypt
    :param data: data to encrypt
    :param key: key to be used in encrypt
    :param thread_count: thread count for parallelizable algorhitms. CPU count will be used if omitted.
    :return: encrypted data (blocks format)
    """
    encrypt_func = _OPMODES.get(opmode, None)
    assert encrypt_func is not None
    encrypt_func = encrypt_func[0]
    return _raw(encrypt_func, enc_block_algo[0], enc_block_algo[1], data, enc_block_algo[2], key, IV, thread_count=thread_count)


def blockify(data, block_format, dtype, thread_count=CPU_COUNT):
    result = []
    result_lock = thd.Lock()

    size = struct.calcsize(block_format)
    if len(data) % size != 0:
        data += b"\0" * (size - (len(data) % size))

    count = len(data) // size

    def routine(begin, end, tag):
        segment = data[begin*size:end*size]
        t_result = []
        for i in range(end - begin):
            t_result.append([dtype(x) for x in struct.unpack(block_format, segment[i*size: (i + 1)*size])])
        with result_lock:
            result.append((tag, t_result))

    threads = segmentize(count, thread_count, routine, seg_size=size)
    execute(threads)
    return cleanup_result(result)


def deblockify(blocks, block_format, thread_count=CPU_COUNT):
    result = []
    result_lock = thd.Lock()

    size = struct.calcsize(block_format)

    def routine(begin, end, tag):
        t_result = []
        for block in blocks[begin:end]:
            t_result.append(struct.pack("2I", *block))
        with result_lock:
            result.append((tag, t_result))

    threads = segmentize(len(blocks), thread_count, routine)
    execute(threads)
    return b"".join(cleanup_result(result))


class FileInputByteStream:
    def __init__(self, filename, block_format):
        self.raw = open(filename, "rb")
        self.block_size = struct.calcsize(block_format)
        self.block_format = block_format
        self._eof = False

    def eof(self):
        return self._eof

    def get_block(self):
        buf = self.raw.read(self.block_size)
        if len(buf) == self.block_size:
            return struct.unpack(self.block_format, buf)
        elif len(buf) > 0:
            self._eof = True
            return struct.unpack(self.block_format, buf + b"\0" * (self.block_size - len(buf)))

    def close(self):
        self.raw.close()

    def __getitem__(self, i):
        self.raw.seek(i * self.block_size)
        return self.get_block()

    def __iter__(self):
        while not self.eof():
            yield self.get_block()


class FileOutputByteStream:
    def __init__(self, filename, block_format, min_blocks_to_put=0):
        self.raw = open(filename, "rb")
        self.block_size = struct.calcsize(block_format)
        self.block_format = block_format
        self.min_put_size = min_blocks_to_put + 1
        self._buffer = []

    def put_block(self, block):
        self._buffer.append(block)
        if len(self._buffer) >= self.min_put_size:
            self.flush()

    def flush(self):
        for b in self._buffer:
            self.raw.write(struct.pack(self.block_format, *b))
        self._buffer = []

    def close(self):
        self.flush()
        self.raw.close()


if __name__ == "__main__":
    pass