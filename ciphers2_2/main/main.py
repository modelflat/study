from ciphers import *
from time import perf_counter as pc


def test_rc6(data, opmode):
    cr = Crypto("RC6", opmode)
    time = pc()
    encrypted = cr.encrypt(data)
    time = int((pc() - time) * 1000)
    print("Encrypted data (%d ms):" % time, encrypted)  # ((len(encrypted) * "{:02X}").format(*list(encrypted))))
    time = pc()
    decrypted = cr.decrypt(encrypted)
    time = int((pc() - time) * 1000)
    print("Decrypted data (%d ms):" % time, decrypted)
    if decrypted == data:
        print("test passed")
    else:
        print("test failed! %s != %s" % (decrypted, data))

opmodes = ["ECB", "CBC", "PCBC", "CFB"]

if __name__ == "__main__":
    data = "hello привет, юникод рулит: 汉语 漢語 中文 "
    print(data.encode(), len(data.encode()))
    for mode in opmodes:
        print("MODE:", mode)
        test_rc6(data, mode)
