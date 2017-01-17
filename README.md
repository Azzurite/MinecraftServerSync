# MinecraftServerSync
A simple wrapper around a Spigot Minecraft server so no dedicated server is necessary.

The problem for us was: we were a group of friends, wanted to play together but did not have a dedicated Minecraft server.  
Renting a server would have costed money.  
The solution was, of course, to run the server locally. But this way only one person has the map files.

MinecraftServerSync solves the problem of only one person having the server files.

It is connected to a FTP server, where it uploads/downloads the server files to/from. Whenever someone starts MinecraftServerSync, it will try to look for the server files on the FTP and download them. Then it will start the server, and publish the public IP to the FTP.  
Now, when another user starts MinecraftServerSync, it will detect that a server is already online, add the Server to the Minecraft server list and start Minecraft itself, so the user can join instantly.
When the temporary server host stops his server, all server files will automatically be uploaded to the FTP. 

Now the process can begin anew and somebody else can be the server host.
