# Blindsend Java library

This project is [blindsend](https://github.com/blindnet-io/blindsend) Java library for private, end-to-end encrypted file exchange in only a few lines of code. Blindsend is an open source tool for private file exchanging between a Sender and a Receiver.

[Blindsend](https://github.com/blindnet-io/blindsend) supports two use cases:
1. Uploading a file and obtaining a link for file download (*send* use case). In this case, file Sender initiates the file exchange and provides the download link to file Receiver.
2. Requesting a file and obtaining a link which is transmited to the Sender to use for uploading the requested file (*request* use case). In this case, the Receiver initiates file exchange and provides the upload link to the Sender (think of a doctor, a Receiver, requesting a blood analysis results from a patient, the Sender). The same link is used by the Receiver to download the file.

A demo with a web client is avalable [here](https://blindsend.xyz). To learn more about blindsend and how it works, read our documentation [here](https://developer.blindnet.io/blindsend/).

## Quick start

A runnable example is available per each of the two use case scenarios (packages `examples.request` and `examples.send`), and can be run by executing `FileSendingExample` and `FileReceivingExample` classes' main methods. For the *request* use case use the following commands:
```bash
git clone git@github.com:blindnet-io/blindsend-java-lib.git
cd blindsend-java-lib
mvn clean install
mvn exec:java -Dexec.mainClass=examples.request.FileSendingExample -Dexec.args="<path_to_file>"
mvn exec:java -Dexec.mainClass=examples.request.FileReceivingExample -Dexec.args="<blindsend_link>"
```

The first exec:java command above uses `FileReceiver` to request blindsend file exchange link, prints the link, and then passes it to `FileSender` which encrypts a file given in `<path_to_file>` and sends the encrypted file to blindsend (`-Dexec.args="<path_to_file>"` can be omiited if you wish to use an example file from `resources` folder).

The second exec:java requires `-Dexec.args=` flag to specify the previously generated link, and uses `FileReceiver` to download and decrypt the previously uploaded encrypted file. Decrypted file is saved in your local `home` folder.

The above file exchange example uses our test API which is already specified in the run classes' main methods. You can also [run your own instance](https://github.com/blindnet-io/blindsend-be#installation-instructions) of blindsend API locally. Note that in this example the password for generating file exchange link is specified in the code in the main methods.

To run an example of the *send* use case, just replace the name of the package in the last two commands.

## Dependencies

This project uses [Bouncy Castle](https://www.bouncycastle.org/) cryptographic library. Therefore, when using it in your own project make sure to add `BouncyCastleProvider` to the security provides in your main method
```Java
Security.addProvider(new BouncyCastleProvider());
```  

## Current status
This project has been started by [blindnet.io](https://blindnet.io/) and is currently under development.

