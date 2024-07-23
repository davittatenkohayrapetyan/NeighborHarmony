import kotlinx.coroutines.*
import mu.KotlinLogging
import java.io.File
import java.io.FileInputStream
import java.nio.file.Paths
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*
import javazoom.jl.decoder.JavaLayerException
import javazoom.jl.player.Player
import com.mpatric.mp3agic.Mp3File

private val logger = KotlinLogging.logger {}

fun main() = runBlocking {
    val configFilePath = Paths.get(System.getProperty("user.dir"),  "config.properties").toString()
    val config = loadConfig(configFilePath)

    val startTime = LocalTime.parse(config.getProperty("startTime", "10:00"))
    val endTime = LocalTime.parse(config.getProperty("endTime", "22:45"))
    val minTracks = config.getProperty("minTracks", "3").toInt()
    val maxTracks = config.getProperty("maxTracks", "5").toInt()
    val minWaitTime = config.getProperty("minWaitTime", "10").toInt()
    val maxWaitTime = config.getProperty("maxWaitTime", "15").toInt()
    val minPlays = config.getProperty("minPlays", "1").toInt()
    val maxPlays = config.getProperty("maxPlays", "3").toInt()
    val maxIntervalBetweenFiles = config.getProperty("maxIntervalBetweenFiles", "30").toInt()

    logger.info { "Application started at ${LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))}. Start time: $startTime, End time: $endTime" }

    val defaultMp3Path = Paths.get(System.getProperty("user.dir"), "mp3").toString()
    val mp3DirectoryPath = config.getProperty("mp3Directory", defaultMp3Path)
    logger.info { "MP3 directory path: $mp3DirectoryPath" }

    val mp3Directory = File(mp3DirectoryPath)
    val mp3Files = mp3Directory.listFiles { _, name -> name.endsWith(".mp3") }?.toList() ?: emptyList()

    if (mp3Files.isEmpty()) {
        logger.error { "No MP3 files found in the directory: ${mp3Directory.absolutePath}." }
        return@runBlocking
    }
    logger.info { "MP3 files found in the directory: ${mp3Directory.absolutePath}. Number of files: ${mp3Files.size}" }

    while (LocalTime.now().isBefore(endTime)) {
        val currentTime = LocalTime.now()
        if (currentTime.isAfter(startTime)) {
            val remainingTime = java.time.Duration.between(currentTime, endTime).toMinutes()
            logger.info { "Current time: $currentTime, Remaining time: $remainingTime minutes" }

            if (remainingTime < 10) {
                logger.info { "Less than 10 minutes remaining to play another set of MP3 files. Stopping playback." }
                break
            }

            playRandomFiles(mp3Files, remainingTime, minTracks, maxTracks, minPlays, maxPlays, maxIntervalBetweenFiles)
        } else {
            val waitTime = java.time.Duration.between(LocalTime.now(), startTime).toMillis()
            logger.info { "Waiting ${waitTime / 1000 / 60} minutes to start the playback" }
            delay(waitTime)
        }

        val waitDuration = Random().nextInt(maxWaitTime - minWaitTime + 1) + minWaitTime
        val currentTimeAfterPlay = LocalTime.now()
        val remainingTimeAfterPlay = java.time.Duration.between(currentTimeAfterPlay, endTime).toMinutes()
        logger.info { "Wait duration: $waitDuration minutes, Current time: $currentTimeAfterPlay, Remaining time after playing: $remainingTimeAfterPlay minutes" }

        if (remainingTimeAfterPlay < waitDuration) {
            logger.info { "Not enough time remaining to wait for $waitDuration minutes. Stopping playback." }
            break
        }

        logger.info { "Waiting $waitDuration minutes before next playback" }
        delay(waitDuration * 60 * 1000L)
    }
    logger.info { "Application finished at ${LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))}" }
}

suspend fun playRandomFiles(mp3Files: List<File>, remainingTime: Long, minTracks: Int, maxTracks: Int, minPlays: Int, maxPlays: Int, maxIntervalBetweenFiles: Int) {
    val selectedFiles = mp3Files.shuffled().take((minTracks..maxTracks).random())
    logger.info { "Selected ${selectedFiles.size} random MP3 files to play at ${LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))}" }

    // Initialize play counts for each file
    val filePlayCounts = selectedFiles.associateWith { (minPlays..maxPlays).random() }.toMutableMap()
    logger.info { "Play counts for files: $filePlayCounts" }

    var remainingTimeVar = remainingTime

    while (remainingTimeVar > 0 && filePlayCounts.isNotEmpty()) {
        val file = filePlayCounts.keys.random()
        val fileDuration = getFileDuration(file)
        val playCount = filePlayCounts[file] ?: 0

        if (playCount > 0) {
            // Play the file
            playMp3(file)
            remainingTimeVar -= fileDuration

            // Update the play count
            filePlayCounts[file] = playCount - 1

            // Remove the file if it has been played the maximum number of times
            if (filePlayCounts[file] == 0) {
                filePlayCounts.remove(file)
                logger.info { "File ${file.name} has reached its play limit and has been removed from the list." }
            }

            // Apply interval between files randomly
            if (remainingTimeVar > 0 && Random().nextBoolean()) {
                val interval = Random().nextInt(maxIntervalBetweenFiles + 1)
                logger.info { "Waiting $interval seconds before playing next file" }
                delay(interval * 1000L)
            }
        }
    }

    logger.info { "Playback finished. Remaining time: $remainingTimeVar minutes" }
}



suspend fun playMp3(file: File) {
    withContext(Dispatchers.IO) {
        try {
            FileInputStream(file).use { fis ->
                val player = Player(fis)
                logger.info { "Playing: ${file.name} at ${LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))}" }
                player.play()
            }
        } catch (e: JavaLayerException) {
            logger.error(e) { "Failed to play MP3 file: ${file.absolutePath}" }
        }
    }
}

fun getFileDuration(file: File): Long {
    return try {
        val mp3File = Mp3File(file)
        val durationInSeconds = mp3File.lengthInSeconds
        val durationInMinutes = durationInSeconds / 60
        logger.info { "Calculated duration for file ${file.name}: $durationInMinutes minutes ($durationInSeconds seconds)" }
        durationInMinutes
    } catch (e: Exception) {
        logger.error(e) { "Failed to get duration for file: ${file.absolutePath}" }
        0
    }
}

fun loadConfig(filePath: String): Properties {
    val properties = Properties()
    try {
        FileInputStream(filePath).use { inputStream ->
            properties.load(inputStream)
            logger.info { "Loaded config from $filePath at ${LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))}" }
        }
    } catch (e: Exception) {
        logger.error(e) { "Failed to load config file: $filePath. Using default settings." }
    }
    return properties
}
