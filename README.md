# Blindsend Java code examples

This project offers an example of [blindsend](https://github.com/blindnet-io/blindsend) Java library for private, end-to-end encrypted file exchange in only a few lines of code. Blindsend is an open source tool for private file exchanging between a Sender and a Receiver.

[Blindsend](https://github.com/blindnet-io/blindsend) works by having a file requesting party (which is also a file receiver) providing a password to generate a file exchange link via blindsend, and transmitting the link to the file Sender. The Sender then uses the link to upload the file, which is first encrypted before uploading it to blindsend. After successful upload, the Receiver uses the password and the same link to download the encrypted file. Once downloaded, the file is decrypted locally on Receiver's machine. A demo with a web client is avalable [here](https://blindsend.xyz).

## Quick start

You can try out blindsend Java library by executing `BlindsendFileSendingExample` and `BlindsendFileReceivingExample` classes' main methods. You can do so by running the following commands:
```bash
git clone git@github.com:blindnet-io/blindsend-examples-java.git
cd blindsend-examples-java
mvn clean install
mvn exec:java -Dexec.mainClass=examples.BlindsendFileSendingExample -Dexec.args="<path_to_file>"
mvn exec:java -Dexec.mainClass=examples.BlindsendFileReceivingExample -Dexec.args="<blindsend_link>"
```
The first exec:java command uses `FileReceiver` to request blindsend file exchange link, prints the link, and then passes it to `FileSender` which encrypts a file given in `<path_to_file>` and sends the encrypted file to blindsend (`-Dexec.args="<path_to_file>"` can be omiited if you wish to use an example file from `resources` folder).

The second exec:java requires `-Dexec.args=` flag to specify the previously generated link, and uses `FileReceiver` to download and decrypt the previously uploaded encrypted file. Decrypted file is saved in your local `home` folder.

The above file exchange example uses our test API which is already specified in the run classes' main methods. You can also [run your own instance](https://github.com/blindnet-io/blindsend-be#installation-instructions) of blindsend API locally. Note that in this example the password for generating file exchange link is specified in the code in the main methods.

## Dependencies

This project uses [Bouncy Castle](https://www.bouncycastle.org/) cryptographic library. Therefore, when using it in your own project make sure to add `BouncyCastleProvider` to the security provides in your main method
```Java
Security.addProvider(new BouncyCastleProvider());
```  

## Current status
This project has been started by [blindnet.io](https://blindnet.io/) and is currently under development.

