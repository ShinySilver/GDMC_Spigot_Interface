#!/bin/bash

mkdir -p BuildTools
cd BuildTools
echo "This script will download and run the latest version of BuildTools in order to build Spigot. BuildTools is the official Spigot distribution method - see https://www.spigotmc.org/wiki/buildtools/ for more details. Long story short, the reason why we have to build spigot ourself is minecraft's license - I'm not a jurist, but from what I understand you can't distribute minecraft's code, but you can decompile its jar, patch it and use it in situ, and that's what BuildTools does. Do you wish to continue?"
select yn in "Yes" "No"; do
    case $yn in
        Yes ) break;;
        No ) exit;;
    esac
done

echo ""
echo "Downloading BuildTools from the official repo (https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar)..."
curl -o BuildTools.jar https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar

echo ""
echo "Select the version of minecraft that you want to use. For example, you can type '1.16.5', without the comma. Or you can type 'latest' if you don't care. I'm not sanitizing the output, so type things well please ;)"
echo "Note: Building spigot will start next. It can take a while. It needs to clone repositories, decompile things, patch stuff, etc. It's gonna flood this terminal and take, like, 5-10 minutes?"
read -p "Version? " version
java -jar BuildTools.jar --rev $version --generate-source

echo ""
echo "####### End of spigot build #######"
echo ""
echo "If you don't see any error above, building Spigot was probably a success! "

echo ""
echo "Deleting the old server jar (if any) and copying the newly built one in its place."
cd ..
rm *.jar
cp BuildTools/spigot-$version.jar ./

echo ""
echo "Done! If you are just a regular GDMC participant, you've finished. But if you are a spigot framework dev, don't forget to import the server jar into your build path. You can also choose to additionally import Buildtools/Spigot/Spigot-API/target/spigot-something.jar if you want a clutter-free jar of the API to explore in your IDE. In this case, you will its source in BuildTools/Spigot/Spigot-API/src/main/java if you want some documentation in your IDE."

echo ""
read -p "Press enter to exit" ex
