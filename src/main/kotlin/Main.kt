import kotlinx.coroutines.*
import mu.KotlinLogging
import java.io.File
import java.io.InputStream
import java.nio.file.Paths
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*
import javazoom.jl.decoder.JavaLayerException
import javazoom.jl.player.Player
import com.mpatric.mp3agic.Mp3File
import java.io.FileInputStream

private val logger = KotlinLogging.logger {}

fun main() = runBlocking {
    val startTime = LocalTime.of(10, 30)
    val endTime = LocalTime.of(22, 30)
    logger.info { "Application started. Start time: $startTime, End time: $endTime" }

    val config = loadConfig("config.properties")
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

            if (remainingTime < 30) {
                logger.info { "Not enough time remaining to play another set of MP3 files. Stopping playback." }
                break
            }

            playRandomFiles(mp3Files, remainingTime)
        } else {
            val waitTime = java.time.Duration.between(LocalTime.now(), startTime).toMillis()
            logger.info { "Waiting ${waitTime / 1000 / 60} minutes to start the playback" }
            delay(waitTime)
        }

        val waitDuration = Random().nextInt(16) + 15 
        val currentTimeAfterPlay = LocalTime.now()
        val remainingTimeAfterPlay = java.time.Duration.between(currentTimeAfterPlay, endTime).toMinutes()
        logger.info { "Wait duration: $waitDuration minutes, Time after playing: $currentTimeAfterPlay, Remaining time after playing: $remainingTimeAfterPlay minutes" }

        if (remainingTimeAfterPlay < waitDuration) {
            logger.info { "Not enough time remaining to wait for $waitDuration minutes. Stopping playback." }
            break
        }

        logger.info { "Waiting $waitDuration minutes before next playback" }
        delay(waitDuration * 60 * 1000L)
    }
    logger.info { "Application finished" }
}

suspend fun playRandomFiles(mp3Files: List<File>, remainingTime: Long) {
    val randomFiles = mp3Files.shuffled().take((1..2).random())
    logger.info { "Selected ${randomFiles.size} random MP3 files to play" }

    for (file in randomFiles) {
        val fileDuration = getFileDuration(file)
        logger.info { "File: ${file.name}, Duration: $fileDuration minutes, Remaining time: $remainingTime minutes" }

        if (remainingTime - fileDuration > 0) {
            playMp3(file)
        } else {
            logger.info { "Not enough time remaining to play ${file.name}. Stopping playback." }
            break
        }
    }
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

fun loadConfig(fileName: String): Properties {
    val properties = Properties()
    val classLoader = Thread.currentThread().contextClassLoader
    val inputStream: InputStream? = classLoader.getResourceAsStream(fileName)
    if (inputStream != null) {
        properties.load(inputStream)
        logger.info { "Loaded config from $fileName" }
    } else {
        logger.warn { "Config file not found in resources. Using default settings." }
    }
    return properties
}
