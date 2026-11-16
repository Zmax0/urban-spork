## Protocol interoperability

**passed: 66/66 passing** (60 forwarding cases + 6 wrong-credential cases).

Branch: `dev/protocol-interop-ci` · Commit: `b58f40a9e88b299d49025330c4a4ced0bda06728` · [Run](https://github.com/Zmax0/urban-spork/actions/runs/43824032680)

| Protocol | Passed / expected | Failures | Errors | Skipped | Flaky | Status |
| --- | ---: | ---: | ---: | ---: | ---: | --- |
| shadowsocks | 26/26 | 0 | 0 | 0 | 0 | passed |
| vmess | 34/34 | 0 | 0 | 0 | 0 | passed |
| trojan | 6/6 | 0 | 0 | 0 | 0 | passed |

| Protocol | Configuration | Passed / expected | Status |
| --- | --- | ---: | --- |
| shadowsocks | aes-128-gcm | 6/6 | passed |
| shadowsocks | aes-256-gcm | 4/4 | passed |
| shadowsocks | chacha20-poly1305 | 4/4 | passed |
| shadowsocks | 2022-blake3-aes-128-gcm | 4/4 | passed |
| shadowsocks | 2022-blake3-aes-256-gcm | 4/4 | passed |
| shadowsocks | 2022-blake3-chacha20-poly1305 | 4/4 | passed |
| vmess | aes-128-gcm-tcp | 6/6 | passed |
| vmess | aes-128-gcm-tls | 4/4 | passed |
| vmess | aes-128-gcm-ws | 4/4 | passed |
| vmess | aes-128-gcm-wss | 4/4 | passed |
| vmess | chacha20-poly1305-tcp | 4/4 | passed |
| vmess | chacha20-poly1305-tls | 4/4 | passed |
| vmess | chacha20-poly1305-ws | 4/4 | passed |
| vmess | chacha20-poly1305-wss | 4/4 | passed |
| trojan | tls | 6/6 | passed |

VMess reference UDP covers 64/1024 bytes and consecutive datagrams; 4096-byte VMess UDP is covered by the regular urban-spork regression tests.

