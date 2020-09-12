# Blindsend Java code examples

This project offers Java examples for private, end-to-end encrypted file exchange in only a few lines of code.
To exchange files privately between a sender and a receiver, this example relies on [blindsend](https://github.com/blindnet-io/blindsend), an open source tool for private file exchanging.

[Blindsend](https://github.com/blindnet-io/blindsend) works by having a file requesting party (which is also a file receiver) generating a file exchange link via blindsend, and transmitting the link to the file sender. The sender then uses the link to upload the file, which is first encrypted before uploading it to blindsend. After successful upload, the receiver can use the same link to download the encrypted file.
Once downloaded, the file is decrypted locally on receiver's machine.

To setup and run an example of blindsend file sharing workflow, you will first need blindsend API endpoint. You can use test API provided in this example, or you can run your own instance of blindsend locally.

Class `BlindsendFileSendingExample` contains a main method that can be run to execute blindsend file requesting and sending. It is executed as follows:
1. Request a file sharing link from blindsend
2. Encrypt the example file provided in the `resources` folder (or your own file if wished)
3. Sending locally encrypted file to blindsend via generated link

Class `BlindsendFileReceivingExample` contains a main method that can be run to receiving previously sent file. To run successfully, it needs blindsend link obtained after executing `BlindsendFileSendingExample`. It is executed as follows:
1. Downloading the encrypted file from blindsend via generated link
2. Decryption of downloaded file on your local machine

This project uses [Bouncy Castle](https://www.bouncycastle.org/) cryptographic library. Therefore, when using it in your own project make sure to add `BouncyCastleProvider` to the security provides in your main method
```Java
Security.addProvider(new BouncyCastleProvider());
```  

## Current status
This project is in ongoing development.

