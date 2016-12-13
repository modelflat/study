import ciphers._ciphers as c
import ciphers.rc6
import numpy as np

ALGOS = {
    "RC6" : (
        (rc6.encrypt, "2I", np.uint32),
        (rc6.decrypt, "2I", np.uint32),
        rc6.expand_key,
        16
    )
}

_ENC_ONLY_OPMODES = ["OFB", "CFB", "CTR"]


class Crypto:

    def __init__(self, algo, opmode, key=None):
        self.algo = ALGOS[algo]
        self.opmode = opmode
        self.raw_key = c.generate_key(self.algo[3]) if not key else key
        self.key = self.algo[2](self.raw_key)
        self.IV = c.generate_IV(self.algo[0][1], self.algo[0][2]) if self.opmode != "ECB" else None

    def encrypt(self, string):
        blocks = c.raw_encrypt(self.algo[0], self.opmode, string.encode(), self.key, self.IV)
        return c.deblockify(blocks, self.algo[0][1])

    def decrypt(self, byte_string):
        blocks = c.raw_decrypt(self.algo[0] if self.opmode in _ENC_ONLY_OPMODES else self.algo[1],
                               self.opmode, byte_string, self.key, self.IV)
        return c.deblockify(blocks, self.algo[1][1]).rstrip(b"\0").decode()
