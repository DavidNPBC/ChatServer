Concurrent Chat Server Project

This is a project I did whilst I was at <Code for All_>'s Full-Stack bootcamp.
Our challenge was to implement a chat that would be able to serve multiple Clients at the same time.
We had to make sure the chat would be able to effectively handle several requests from Clients at the same time.
Thus, our chat had to deal with concurrency from multiple Clients connecting.
I used synchronized locks to deal with the Lists as Client could enter and quit the chat at the same time and could ask for the list of present CLients in the chat.

The design of the program is that there is a server that is constantly listening in for new Clients at port 999. As soon as a new Client gets in, the server dispatches the socket to a Server Handler in a different thread.
To manage the multiple threads a Cached Thread Pool was used.
The Server Helper is used to the Server can be listening in for new Clients whilst the Server Helper provides the chat functionality for the Client.
The same thing happens in the Client as, as soon as it is connected it uses a Single Thread Pool to connect to the Server.
The Client then has a Client Helper as well so that it is able to receive messages, whilst it sends messages.
Thus, the threads and the helpers here are used in the Server to be able to have several connecting clients and in the CLients to be able to send and receive messages at the same time.

Our chat would have to have a list of commands, which were that the Client would need to be able to:
-Check who else was in the chat ("/list")
-Send a private message to someone ("/pm + available Nickname")
-Change its nickname ("/change + nick")
-Quit the chat ("/quit")

I added some features, namely:
-A command to receive the list of commands available ("/commands")
-A command to send a risky text to someone randomly in the chat, without the Client knowing who it is. However, the receiving person knows who sent it ("/risky + message")
-3 commands to sent ASCII art of pikachu, kakarot or a random pokemon (there are 6 different ones) to the whole chat.

In addition, I added some defensive programming so that the commands would work as expected.
