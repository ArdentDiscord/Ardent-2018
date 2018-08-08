
# Ardent  
Created and maintained by Adam Ratzman.  
This readme will teach you how to deploy Ardent on your local machine and learn how it works.

## Running the (test) bot  
### Clone the repository
Clone the project using `https://github.com/ArdentDiscord/Ardent-2018.git` or `git@github.com:ArdentDiscord/Ardent-2018.git`
### Get Docker
First, you need the following technologies: Docker and Docker Compose. If you're using Windows (but not 10 Pro), use [this link](https://docs.docker.com/toolbox/) to install the Docker Toolbox, which will install both for you. Use the Docker quickstart terminal. Otherwise, install the version of Docker CE (Community edition) and Docker Compose that are available for your machine [here](https://docs.docker.com/install/) and [here (for compose)](https://docs.docker.com/compose/install/). 

### Configuring the bot
Navigate to the Ardent project root directory. You'll need to create a file called `test-config` (no extension) to put your API keys. Ardent will load its config from this. 

Inside, you need the following structure:  
```
test :: true  
token :: DISCORD_BOT_TOKEN_HERE  
giphy :: GIPHY_TOKEN_HERE
google :: GOOGLE_TOKEN_HERE
client_secret :: DISCORD_BOT_CLIENT_SECRET_HERE  
spotify_client_id :: SPOTIFY_CLIENT_ID
spotify_client_secret :: SPOTIFY_CLIENT_SECRET
```
There are five straightforward steps to being able to run Ardent, which we'll go over now.
 1. Create a Discord bot [here](https://discordapp.com/developers/applications/). You'll find the client secret (client_secret in the config) under `General Information` on the right side. 
 2. Go to the `Bot` tab. Create a bot if one isn't already created, then replace `token` inside the config with your bot token.
 3. Go to the [Giphy API](https://developers.giphy.com/dashboard/?create=true) site and create an application. Put the API key provided under the `giphy` key in the config.
 4. Go to the Spotify [developer dashboard](https://developer.spotify.com/dashboard/) and create an application. Put the client id and secret it gives you under their respective keys in the config.
 5. This step is a bit complicated and involves integrating the Google Sheets API into the bot, necessary for trivia (and soon, other stuff!).
 First, sign into GCP (Google Cloud Platform) with your Google account [here](https://console.cloud.google.com). Create a project when it prompts you. It'll take up to a minute to instantiate, so be patient. 
 Next, click on the 3 menu bars on the top left and go to `APIs & Services`. Click on `Enable APIs and Services` and search for the `Google Sheets API`.
 Click `Enable`, then click on `Credentials` on the **left** side of the screen.
 On the dropdown menu, select to create an `API Key`. Copy the created key and put it as the value of the `google` key.

Cool, you're done with the setup! Ardent takes care of setting up the database on first run, which is stored in a docker volume named `rethinkdb` by default (you can change this!).

### Running the bot
You need to run this via the Docker Quickstart Terminal if you're using Windows. Otherwise, the command line will work. If you're on a UNIX machine, make sure that docker is running and docker-compose is installed.

Inside the root directory of the project, type
```
cd compose
sh deploy.sh
```
to change your working directory and then deploy the bot.
 **Read the deployment script carefully. Though there's nothing in it that would harm your computer, never trust scripts over the internet!**
 
 If all goes well, the bot will start up! Note that on the first time you run it, docker needs to pull the `rethinkdb:latest` and `openjdk:8-jre-slim` images. 

## Getting started for developers
Ardent uses a centralized command system that takes advantage of reflection to instantiate commands at runtime. I recommend you begin with the [Ardent register](https://github.com/ArdentDiscord/Ardent-2018/tree/master/src/main/kotlin/com/ardentbot/core/ArdentRegister.kt) to see what components are available. Then, look through the [commands directory](https://github.com/ArdentDiscord/Ardent-2018/tree/master/src/main/kotlin/com/ardentbot/core/commands) to understand how commands are defined, processed, and how help is sent. Afterwards, look at a few simple commands (like Ping.kt) to check your understand. Talk to Adam#9261 on our [Discord server](https://ardentbot.com/support) if you have questions, comments, or are ready to submit a pull request or work on a feature!

### Note about Translations
Since you don't have API access to Ardent's [crowdin](https://crowdin.com/project/ardent), you'll use a local zip file containing translations. As they're not updated in real time, they may get out of date. Make sure to pull regularly.

### Note about OpenJDK and Docker
Even though you can substitute OpenJDK with the Oracle JRE (though good luck with the legality of that) or run Ardent on your local machine, we use OpenJDK and Docker to ensure portabilit. Though you can change the version in Dockerfile.ardent.test, make sure it's not an **alpine-based image**, or else playing music will crash the JVM.