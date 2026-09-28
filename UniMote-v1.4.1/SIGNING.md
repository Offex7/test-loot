# Signing instructions

The original certificate is recorded in BASELINE.md, but the original private key was not supplied.

The working artifact was signed with a newly generated PKCS#12 keystore using jarsigner. The local keystore is not committed to this public branch.

Preferred Android build-machine flow when build-tools are available:

1. Keep the same applicationId and version values.
2. Use the intended private key/keystore.
3. Run zipalign.
4. Sign with apksigner using V2/V3 schemes.
5. Run apksigner verify --verbose --print-certs.

For the exact keystore used for the supplied artifact, use the separately supplied local file and password. Do not publish that private key to a public repository.
