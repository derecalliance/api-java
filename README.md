# DeRec-tests

## DeRec-as-a-Service Prototype

A prototype implementation of [DeRec over HTTP](https://github.com/jorabin/derec-tests/tree/master/src/main/java/com/thebuildingblocks/derec/v0_9) called version 0.9.

## Cryptography

Tests of [Shamir Secret Sharing and various prototypes of encryption](https://github.com/jorabin/derec-tests/tree/master/src/main/java/com/thebuildingblocks/derec/crypto) for DeRec:

- crude performance evaluation of Shamir Secret Sharing
- initial proposal AES-GCM based pairing and encryption
- custom encoding of a sign then encrypt key transfer
- RFC 5652 CMS Key Transfer with signature
- RFC 5652 CM Key Agreement 

## Some Reference material

Neil Madden [Ephemeral elliptic curve Diffie-Hellman key agreement in Java](https://neilmadden.blog/2016/05/20/ephemeral-elliptic-curve-diffie-hellman-key-agreement-in-java/)  
Mykong [Java AES encryption and decryption](https://mkyong.com/java/java-aes-encryption-and-decryption/)  
ProAndroidDev
Patrick Favre-Bulle [Security Best Practices: Symmetric Encryption with AES in Java and Android](https://proandroiddev.com/security-best-practices-symmetric-encryption-with-aes-in-java-7616beaaade9)   
IETF [RFC 5652](https://datatracker.ietf.org/doc/html/rfc5652)  
[100 Examples of using Bouncy Castle](https://www.bouncycastle.org/fips-java/BCFipsIn100.pdf)

Things to look at

[Bouncy Castle GPG](https://neuhalje.github.io/bouncy-gpg/)
