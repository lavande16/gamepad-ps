#!/usr/bin/env python3
"""Synthetic ISO metadata and card containers; no copyrighted game data or executable code."""
from pathlib import Path
import struct


def both32(n): return struct.pack('<I', n) + struct.pack('>I', n)
def both16(n): return struct.pack('<H', n) + struct.pack('>H', n)
def record(name, sector, size, directory=False):
    data = bytearray(33 + len(name) + (len(name) % 2 == 0))
    data[0] = len(data); data[2:10] = both32(sector); data[10:18] = both32(size)
    data[25] = 2 if directory else 0; data[28:32] = both16(1)
    data[32] = len(name); data[33:33+len(name)] = name
    return data


def iso(path, boot):
    data = bytearray(64 * 2048)
    pvd = memoryview(data)[16*2048:17*2048]
    pvd[0:7] = b'\x01CD001\x01'; pvd[80:88] = both32(64); pvd[128:132] = both16(2048)
    pvd[156:190] = record(b'\0',20,2048,True)
    system = f'BOOT2 = cdrom0:\\{boot};1\nVER = 1.00\nVMODE = NTSC\n'.encode()
    directory = record(b'\0',20,2048,True)+record(b'\1',20,2048,True)+record(b'SYSTEM.CNF;1',21,len(system))
    data[20*2048:20*2048+len(directory)] = directory
    data[21*2048:21*2048+len(system)] = system
    path.write_bytes(data)


def card(path, sentinel):
    with path.open('wb') as stream:
        stream.write(b'Sony PS2 Memory Card Format')
        stream.seek(1024); stream.write(bytes([sentinel]))
        stream.truncate(8*1024*1024)


if __name__ == '__main__':
    folder = Path(__file__).resolve().parents[1]/'stage3/fixtures'; folder.mkdir(exist_ok=True)
    iso(folder/'import.iso','SLUS_999.98'); iso(folder/'preserve.iso','SLUS_999.99')
    card(folder/'source-card.ps2',3); card(folder/'current-card.ps2',7)
    (folder/'invalid.iso').write_text('Not an ISO despite its file extension')
    print('Synthetic metadata fixtures created:',folder)
