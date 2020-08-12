#### DMRDecode - DMR Radio Protocol Decoder
Original Java program written by Ian Wraith, and I have imported that into Netbeans 8 and produced this Java SE 8 Repository with an executable JAR file.
```
java -jar DMRDecode.jar
```
The aim of the project was to provide a user friendly DMR data decoder for hobbyists. It is Java based so should run under Windows, Apple, or Linux PC's. The only hardware needed is a radio scanner with an FM discriminator output.

The core of the program was based around the open source DSD program. That program was written in C and ran under Linux only, so it was converted to Java and sections removed that didn't relate to DMR.

#### Networking
The program listens for connections on TCP/IP Port 17887. When a client connects (this can be simulated with the command "telnet 127.0.0.1 17887") the program responds with "OK". Up to 10 clients can be connected.

Following that, on receiving a voice frame the program sends the following information to each connected client ..

"#" - The # character is sent to indicate the start of a voice frame

C - A integer 1 or a 2 is sent to indicate which channel this frame is from

Followed by 27 integers which contain the 216 bits that make up a voice frame. Each integer contains 8 bits.

#### Decode
DMR data is arranged into frames each consisting of 264 bits. There are three types of frame ..

Voice frame. As it suggests, this contains digitized voice data in two 108 bit sections separated by a 48 bit synchronization sequence which identifies as the frame as being a voice one. As yet the program does nothing with voice frames other than displaying their presence.

Data frame. These contain signalling information which is required for call set up. The frames consist of two 98 bit sections which contain the main signalling payload (which the program doesn't yet decode) plus a 48 bit synchronization sequence which identifies the frame and a 20 bit SLOT TYPE section. The SLOT TYPE section of the frame is decoded by the program. A decoded example would be ..

#### Slot Type : Color Code 2 CSBK

Where you see the systems color code (actually a number between 0 and 15) and what kind of information is being sent in the main signalling payload of the frame (in this case CSBK data).

Embedded frame. These frames don't contain any synchronization bits but instead carry embedded signalling in its place (which isn't currently decoded) they also contain either voice or signalling data in their main payload. The signalling data variety contain a SLOT TYPE section which is decoded (see point 2) while the voice variety contains a EMB field which is decoded as shown below ..

#### EMB : Color Code 2 : First fragment of LC

Again we see the systems color code and what type of information is contained in the embedded field (in this case the first fragment of an LC).

In between each frame of data DMR systems transmit a short 24 bit burst called the CACH which the program decodes. Each CACH contains a 7 bit section known as the TACT which says what kind of data is seen in the rest of the CACH as seen below ..

#### CACH : TACT AT=1 Ch 1 Last fragment of LC

This shows the CACH was sent in timeslot 1 (DMR is a 2 slot TDMA system) and that the rest of the data is the last fragment of an LC. The data sent in every four CACHs is added together and forms what is known as a SHORT LC. Most kinds of SHORT LC are decoded by the program and look like this ..

#### Short LC : Act_Updt - Slot 1 Active with Voice Group Call Hashed Addr 149 & Slot 2 Not Active

This example tells us that slot 1 us active with a voice group call which has a shortened address of 149 while slot 2 is inactive. You may also see SHORT LCs that look like this ..

#### Short LC : Unknown SLCO=15 000100000000011100000000

These none standard Short LC messages are believed to be sent by Motorola's Capacity Plus DMR systems.
