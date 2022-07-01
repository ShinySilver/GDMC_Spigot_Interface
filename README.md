# GDMC_Framework_Dev

## What can we find in this repo?

In the `GDMC HTTP Interface Plugin/` folder, you'll find the source code of the GDMC HTTP Interface plugin for Spigot.

In the `GDMC Server/` folder, you'll find scripts to setup Spigot, a modified minecraft server, with our interface.

In `GDMC Simple Python Interface/gdmc_interface.py`, you'll find a very basic python API to connect to the minecraft
server through the HTTP interface. We used this API in `GDMC Simple Python Interface/unit_test.py` to implement unit
tests.

## Why should I use this framework?

It's fast. Very fast. You can place up to 500k block per second without even slowing the target server. I'll do a
detailed evaluation here if I'm bored.

## What version of minecraft does this framework support?

Currently, 1.16.5. I'll make a version compatible with multiple version of minecraft "Soon" using reflection. Or I'll
just update it to the latest version of MC, shouldn't take more than half an hour work. I'll see later.

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
entering "localhost" as the server IP.

Note: if you want to be able to connect to your GDMC server in offline mode, you'll have to edit the
`server.properties` file generated at the 1st full server startup and replace `online-mode=true` with
`online-mode=false`. Note that this will enable cracked versions of the game to connect to your server under the
username they want. Be wary! But it's ok as long as you are indeed offline.

### As a framework dev
If you want to contribute, you'll have to open the `GDMC HTTP Interface Plugin/` as a new java project in your IDE &
import into your project's java build path `GDMC Server/spigot-X.X.X.jar` (replace X.X.X with the MC version you want
to support). This is the server jar, all the libs and classes you see there can be used in the plugin.
 
However this jar is very cluttered - if you want something easier to navigate into, you should in addition to the
previous one import `GDMC Server/BuildTools/Spigot/Spigot-API/target/spigot-api-X.X.X-RX.X-SNAPSHOT.jar`. It's the
spigot API that the plugin is using & it's supposed to stay stable between minecraft version! Moreover its source can be
imported to be used as & documentation, and can be found in `GDMC Server/BuildTools/Spigot/Spigot-API/src/main/java/`.

In a best case scenario, we would create our plugin using only spigot API calls in order to have a plugin working on all
MC versions, but we cannot - some low level things cannot be done using it, such as serializing chunks as json or
parsing block data str for example. Soooo I have to use some ugly code from the full server jar, and you can't just
import the spigot API