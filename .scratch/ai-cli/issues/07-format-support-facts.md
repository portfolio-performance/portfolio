# Which client file formats can core read and write headlessly, and what does tracking upstream format changes cost?

Type: research

## Question

For each client file format core supports (plain XML, compressed XML, binary/protobuf, encrypted in its AES variants), can `ClientFactory` load and save it headlessly on the plain classpath from ticket 01, with the format preserved on save? How does core detect the format and file version, where is the supported-version ceiling defined, and how does it react to a newer-than-supported file? How are encrypted-file passwords supplied to core, and what happens on a wrong password? Finally, how often has upstream changed the file format or version over the last few years (from git history of `ClientFactory`, the protobuf schema and the version constant), as input for the cost of keeping this fork current?
