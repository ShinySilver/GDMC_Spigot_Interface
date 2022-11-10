# GDMC_Framework_Dev

A framework to modify the map on a spigot minecraft server from an HTTP interface / Python. It uses spark to do most of
the network-related stuff, and FastAsyncWorldEdit to actually place the blocks with high performance.

## What can we find in this repo?

In the `GDMC HTTP Interface Plugin/` folder, you'll find the source code of the GDMC HTTP Interface plugin for Spigot.

In the `GDMC Server/` folder, you'll find scripts to setup Spigot, a modified minecraft server, with our interface.

In `GDMC Simple Python Interface/gdmc_interface.py`, you'll find a very basic python API to connect to the minecraft
server through the HTTP interface. We used this API in `GDMC Simple Python Interface/unit_test.py` to implement unit
tests.

## Why should I use this framework?

~~It's fast. Very fast. You can place up to 500k block per second without even slowing the target server. I'll do a
detailed evaluation here if I'm bored.~~ I recoded everything from the ground up a few month ago, I expect it to be even faster, but I've done neither testing nor implemented batching, sooooo idk really.

## What version of minecraft does this framework support?

Currently, 1.19.X. But as long as you swap the Fast Async World Edit in the server plugin folder with another version, the HTTP interface plugin (and its python interface) should work as long as its API is not too different.

## I want pictures!

Maybe later?

## Where do I start?
### As a GDMC participant
If you want to try using our framework for your submission, you should first clone our repo. Then, you should open the
`GDMC Server/` folder, run the `1_build_server_jar.sh` script in order to download BuildTools & build Spigot, then
`2_fetch_latest_interface_build.sh` in order to download the latest interface version for the MC version you selected.
Don't forget to tell my script to build Spigot for 1.16.5 when it asks you!

At last, you'll only have to run `3_start_server.sh` in order to start the server. The first time you'll execute it, the
server will shut down after generating a license agreement file. Go read it & replace `eula=false` with `eula=true`
after agreeing. Restart the server, it's gonna start up! When fully started, you'll be able to connect from Minecraft by
entering "localhost" as the server IP, & try running some unit test from python.

Note: if you want to be able to connect to your GDMC server in offline mode, you'll have to edit the
`server.properties` file generated at the 1st full server startup and replace `online-mode=true` with
`online-mode=false`. Note that this will enable cracked versions of the game to connect to your server under the
username they want. Be wary! But it's ok as long as you are indeed offline.

### As a framework dev
If you want to contribute, you'll have to open the `GDMC HTTP Interface Plugin/` as a new java project in your IDE &
import into your project's java build path `GDMC Server/spigot-X.X.X.jar` (replace X.X.X with the MC version you want
to support). This is the server jar, all the libs and classes you see there can be used in the plugin. I left some of
my IDE config file in the folder, if you are using Eclipse, you might get lucky!
 
However this jar is cluttered with all the lib used by minecraft - if you want something easier to navigate into, you should in addition to the
previous one import `GDMC Server/BuildTools/Spigot/Spigot-API/target/spigot-api-X.X.X-RX.X-SNAPSHOT.jar`. It's the
spigot API that the plugin is using & it's supposed to stay 99.99% stable between minecraft version! Moreover its source can be
imported to be used as a documentation, and can be found in `GDMC Server/BuildTools/Spigot/Spigot-API/src/main/java/`.
