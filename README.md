# NeighborHarmony

NeighborHarmony is a Kotlin-based desktop application designed to handle noisy neighbors by playing random MP3 files from a specified directory at configurable times and intervals. The application provides a user interface to start and stop playback, and a settings dialog to configure playback parameters, which are saved to a properties file.

## Important Disclaimer

Please consider using this approach only as a last resort. Before resorting to this solution, try all other possible approaches:
- Talking to your neighbors
- Calling the police
- Discussing the issue with other neighbors

Be prepared to talk to your neighbors if they become upset by the noise. Make sure to use the configuration that fits your needs and complies with your local laws.


![img_3.png](img_3.png)
> "The path of the righteous man is beset on all sides by the inequities of the selfish and the tyranny of evil men. Blessed is he who, in the name of charity and good will, shepherds the weak through the valley of the darkness. For he is truly his brother’s keeper and the finder of lost children. And I will strike down upon thee with great vengeance and furious anger those who attempt to poison and destroy my brothers. And you will know I am the Lord when I lay my vengeance upon you." — Pulp Fiction

## Features

- Plays random MP3 files from a specified directory.
- Configurable playback start and end times.
- Configurable number of tracks to play, play count, and intervals between tracks.
- User interface with start, stop, and settings buttons.
- Settings are saved to a properties file and can be modified within the application.

## Prerequisites
   
- Java 17+ must be installed on your system.

## Setup

1. **Install Java:**

    - **Windows:**
      Download and install the latest JDK from the [Oracle JDK website](https://www.oracle.com/java/technologies/javase-jdk17-downloads.html).

    - **Linux:**
      Open a terminal and run the following command:
      ```
      sudo apt update
      sudo apt install openjdk-17-jdk
      ```

    - **macOS:**
      Install Homebrew if you haven't already:
      ```
      /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
      ```
      Then install Java 17:
      ```
      brew install openjdk@17
      ```

2. **Download the NeighborHarmony Application:**

   Download the ZIP file containing the application JAR, MP3 folder, and run scripts. Unzip it to your desired location.

   The ZIP file structure should look like this:
   ```
   NeighborHarmony/
   ├── mp3/
   │   ├── sample1.mp3
   │   ├── sample2.mp3
   │   └── ...
   ├── NeighborHarmony-all.jar
   ├── run.sh
   └── run.bat
   ```

## Running the Application

### Windows

1. Open a command prompt.
2. Navigate to the directory where you unzipped the application.
3. Run the application using the provided batch script:
   ```
   run.bat
   ```

### Linux / macOS

1. Open a terminal.
2. Navigate to the directory where you unzipped the application.
3. Make the shell script executable:
   ```
   chmod +x run.sh
   ```
4. Run the application using the provided shell script:
   ```
   ./run.sh
   ```

## Configuration

The application uses a `config.properties` file to store configuration parameters. These parameters can be modified within the application using the settings dialog. The available parameters are:

- `startTime`: The time when playback starts (default: `10:00`).
- `endTime`: The time when playback ends (default: `22:45`).
- `minTracks`: The minimum number of tracks to play in one session (default: `3`).
- `maxTracks`: The maximum number of tracks to play in one session (default: `5`).
- `minWaitTime`: The minimum wait time between playback sessions in minutes (default: `10`).
- `maxWaitTime`: The maximum wait time between playback sessions in minutes (default: `15`).
- `minPlays`: The minimum number of times to play each track in one session (default: `1`).
- `maxPlays`: The maximum number of times to play each track in one session (default: `3`).
- `maxIntervalBetweenFiles`: The maximum interval between tracks in seconds (default: `30`).
- `mp3Directory`: The directory containing the MP3 files (default: `./mp3`).

The `config.properties` file is automatically created and updated in the application's directory.

## Hardware Setup

As for hardware setup, I strongly recommend vibrospeakers (bone-conducting technology) to target only the wall/ceiling/floor with the annoying neighbor to minimize the impact on other neighbors. Consider devices with either Bluetooth or AUX input (for the latter, you might require a long AUX cable). If the setup requires the usage of multiple speakers, use AUX splitters and Voicemeeter (Banana or Potato versions) to be able to mix output to multiple audio outputs from your computer.

### Recommendation
#### XINYI Sini Audio
![img.png](img.png)

#### Voicemeeter:
- [Voicemeeter Banana](https://vb-audio.com/Voicemeeter/banana.htm)
- [Voicemeeter Potato](https://vb-audio.com/Voicemeeter/potato.htm)

#### AUX Cables

![img_1.png](img_1.png)

#### AUX Splitters

![img_2.png](img_2.png)

Note: Try not to buy cheap ones, as they can lead to a lot of noise, which will annoy you and make the speakers heat up quickly. The ones in the photos are just some random cables and splitters for demonstration purposes, not for recommendation.
## Building the Application

If you need to build the application yourself, follow these steps:

1. **Clone the Repository:**
   ```
   git clone &lt;https://github.com/davittatenkohayrapetyan/NeighborHarmony&gt;
   cd NeighborHarmony
   ```

2. **Build the JAR:**
   Ensure you have Gradle installed. Open a terminal in the project directory and run:
   ```
   ./gradlew shadowJar
   ```

3. **Package the Application:**
   After the build completes, the JAR file will be located in the `build/libs` directory.

## Contact

For any issues or questions, please contact davidhayrapetyan93@gmail.com.

---

## Shell Script (run.sh)

```
#!/bin/bash

java -jar NeighborHarmony-all.jar
```

## Batch Script (run.bat)

```
@echo off

java -jar NeighborHarmony-all.jar
pause
```

## Directory Structure

Ensure your directory structure looks like this:

```
NeighborHarmony/
├── mp3/
│   ├── sample1.mp3
│   ├── sample2.mp3
│   └── ...
├── NeighborHarmony-all.jar
├── run.sh
└── run.bat
```