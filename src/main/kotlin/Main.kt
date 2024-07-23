import kotlinx.coroutines.*
import mu.KotlinLogging
import java.awt.BorderLayout
import java.awt.GridLayout
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Paths
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*
import javax.swing.*
import javazoom.jl.decoder.JavaLayerException
import javazoom.jl.player.Player
import com.mpatric.mp3agic.Mp3File

private val logger = KotlinLogging.logger {}

fun main() {
    SwingUtilities.invokeLater {
        val frame = JFrame("MP3 Player")
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.setSize(600, 600)

        val textArea = JTextArea()
        textArea.isEditable = false
        val scrollPane = JScrollPane(textArea)

        val startButton = JButton("Start")
        val stopButton = JButton("Stop")
        val settingsButton = JButton("Settings")

        val buttonPanel = JPanel()
        buttonPanel.add(startButton)
        buttonPanel.add(stopButton)
        buttonPanel.add(settingsButton)

        frame.layout = BorderLayout()
        frame.add(scrollPane, BorderLayout.CENTER)
        frame.add(buttonPanel, BorderLayout.SOUTH)

        frame.isVisible = true

        val player = Mp3Player(textArea)

        startButton.addActionListener {
            player.start()
        }

        stopButton.addActionListener {
            player.stop()
        }

        settingsButton.addActionListener {
            SettingsDialog(frame, player).isVisible = true
        }
    }
}

class Mp3Player(private val textArea: JTextArea) : CoroutineScope {
    private val job = Job()
    override val coroutineContext = Dispatchers.Main + job
    private var playerJob: Job? = null
    private var currentPlayer: Player? = null
    private var config = Properties()
    private val configFilePath = Paths.get(System.getProperty("user.dir"), "config.properties").toString()

    init {
        loadConfig()
    }

    fun start() {
        if (playerJob == null || playerJob?.isActive == false) {
            playerJob = launch(Dispatchers.IO) {
                val startTime = LocalTime.parse(config.getProperty("startTime", "10:00"))
                val endTime = LocalTime.parse(config.getProperty("endTime", "22:45"))
                val minTracks = config.getProperty("minTracks", "3").toInt()
                val maxTracks = config.getProperty("maxTracks", "5").toInt()
                val minWaitTime = config.getProperty("minWaitTime", "10").toInt()
                val maxWaitTime = config.getProperty("maxWaitTime", "15").toInt()
                val minPlays = config.getProperty("minPlays", "1").toInt()
                val maxPlays = config.getProperty("maxPlays", "3").toInt()
                val maxIntervalBetweenFiles = config.getProperty("maxIntervalBetweenFiles", "30").toInt()

                log("Application started. Start time: $startTime, End time: $endTime")

                val defaultMp3Path = Paths.get(System.getProperty("user.dir"), "mp3").toString()
                val mp3DirectoryPath = config.getProperty("mp3Directory", defaultMp3Path)
                log("MP3 directory path: $mp3DirectoryPath")

                val mp3Directory = File(mp3DirectoryPath)
                val mp3Files = mp3Directory.listFiles { _, name -> name.endsWith(".mp3") }?.toList() ?: emptyList()

                if (mp3Files.isEmpty()) {
                    log("No MP3 files found in the directory: ${mp3Directory.absolutePath}.")
                    return@launch
                }
                log("MP3 files found in the directory: ${mp3Directory.absolutePath}. Number of files: ${mp3Files.size}")

                while (LocalTime.now().isBefore(endTime)) {
                    val currentTime = LocalTime.now()
                    if (currentTime.isAfter(startTime)) {
                        val remainingTime = java.time.Duration.between(currentTime, endTime).toMinutes()
                        log("Current time: $currentTime, Remaining time: $remainingTime minutes")

                        if (remainingTime < 10) {
                            log("Less than 10 minutes remaining to play another set of MP3 files. Stopping playback.")
                            break
                        }

                        playRandomFiles(mp3Files, remainingTime, minTracks, maxTracks, minPlays, maxPlays, maxIntervalBetweenFiles)
                    } else {
                        val waitTime = java.time.Duration.between(LocalTime.now(), startTime).toMillis()
                        log("Waiting ${waitTime / 1000 / 60} minutes to start the playback")
                        delay(waitTime)
                    }

                    val waitDuration = Random().nextInt(maxWaitTime - minWaitTime + 1) + minWaitTime
                    val currentTimeAfterPlay = LocalTime.now()
                    val remainingTimeAfterPlay = java.time.Duration.between(currentTimeAfterPlay, endTime).toMinutes()
                    log("Wait duration: $waitDuration minutes, Current time: $currentTimeAfterPlay, Remaining time after playing: $remainingTimeAfterPlay minutes")

                    if (remainingTimeAfterPlay < waitDuration) {
                        log("Not enough time remaining to wait for $waitDuration minutes. Stopping playback.")
                        break
                    }

                    log("Waiting $waitDuration minutes before next playback")
                    delay(waitDuration * 60 * 1000L)
                }
                log("Application finished at ${LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))}")
            }
        }
    }

    fun stop() {
        playerJob?.cancel()
        currentPlayer?.close()
        currentPlayer = null
        log("Playback stopped")
    }

    private suspend fun playRandomFiles(
        mp3Files: List<File>,
        remainingTime: Long,
        minTracks: Int,
        maxTracks: Int,
        minPlays: Int,
        maxPlays: Int,
        maxIntervalBetweenFiles: Int
    ) {
        val selectedFiles = mp3Files.shuffled().take((minTracks..maxTracks).random())
        log("Selected ${selectedFiles.size} random MP3 files to play at ${LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))}")

        val filePlayCounts = selectedFiles.associateWith { (minPlays..maxPlays).random() }.toMutableMap()
        log("Play counts for files: $filePlayCounts")

        var remainingTimeVar = remainingTime

        while (remainingTimeVar > 0 && filePlayCounts.isNotEmpty() && playerJob?.isActive == true) {
            val file = filePlayCounts.keys.random()
            val fileDuration = getFileDuration(file)
            val playCount = filePlayCounts[file] ?: 0

            if (playCount > 0) {
                playMp3(file)
                remainingTimeVar -= fileDuration

                filePlayCounts[file] = playCount - 1

                if (filePlayCounts[file] == 0) {
                    filePlayCounts.remove(file)
                    log("File ${file.name} has reached its play limit and has been removed from the list.")
                }

                if (remainingTimeVar > 0 && Random().nextBoolean()) {
                    val interval = Random().nextInt(maxIntervalBetweenFiles + 1)
                    log("Waiting $interval seconds before playing next file")
                    delay(interval * 1000L)
                }
            }
        }

        log("Playback finished. Remaining time: $remainingTimeVar minutes")
    }

    private suspend fun playMp3(file: File) {
        withContext(Dispatchers.IO) {
            try {
                FileInputStream(file).use { fis ->
                    val player = Player(fis)
                    currentPlayer = player
                    log("Playing: ${file.name} at ${LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))}")
                    player.play()
                }
            } catch (e: JavaLayerException) {
                log("Failed to play MP3 file: ${file.absolutePath}")
            } finally {
                currentPlayer = null
            }
        }
    }

    private fun getFileDuration(file: File): Long {
        return try {
            val mp3File = Mp3File(file)
            val durationInSeconds = mp3File.lengthInSeconds
            val durationInMinutes = durationInSeconds / 60
            log("Calculated duration for file ${file.name}: $durationInMinutes minutes ($durationInSeconds seconds)")
            durationInMinutes
        } catch (e: Exception) {
            log("Failed to get duration for file: ${file.absolutePath}")
            0
        }
    }

    fun loadConfig() {
        try {
            FileInputStream(configFilePath).use { inputStream ->
                config.load(inputStream)
                log("Loaded config from $configFilePath at ${LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))}")
            }
        } catch (e: Exception) {
            log("Failed to load config file: $configFilePath. Using default settings.")
        }
    }

    fun saveConfig() {
        try {
            FileOutputStream(configFilePath).use { outputStream ->
                config.store(outputStream, "MP3 Player Configuration")
                log("Saved config to $configFilePath at ${LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))}")
            }
        } catch (e: Exception) {
            log("Failed to save config file: $configFilePath")
        }
    }

    private fun log(message: String) {
        SwingUtilities.invokeLater {
            textArea.append(message + "\n")
        }
        logger.info { message }
    }

    fun getConfig(): Properties {
        return config
    }
}

class SettingsDialog(owner: JFrame, private val player: Mp3Player) : JDialog(owner, "Settings", true) {
    private val config = player.getConfig()
    private val startTimeField = JTextField(config.getProperty("startTime", "10:00"))
    private val endTimeField = JTextField(config.getProperty("endTime", "22:45"))
    private val minTracksField = JTextField(config.getProperty("minTracks", "3"))
    private val maxTracksField = JTextField(config.getProperty("maxTracks", "5"))
    private val minWaitTimeField = JTextField(config.getProperty("minWaitTime", "10"))
    private val maxWaitTimeField = JTextField(config.getProperty("maxWaitTime", "15"))
    private val minPlaysField = JTextField(config.getProperty("minPlays", "1"))
    private val maxPlaysField = JTextField(config.getProperty("maxPlays", "3"))
    private val maxIntervalBetweenFilesField = JTextField(config.getProperty("maxIntervalBetweenFiles", "30"))
    private val mp3DirectoryField = JTextField(config.getProperty("mp3Directory", Paths.get(System.getProperty("user.dir"), "mp3").toString()))

    init {
        layout = BorderLayout()
        val panel = JPanel(GridLayout(10, 2))

        panel.add(JLabel("Start Time:"))
        panel.add(startTimeField)
        panel.add(JLabel("End Time:"))
        panel.add(endTimeField)
        panel.add(JLabel("Min Tracks:"))
        panel.add(minTracksField)
        panel.add(JLabel("Max Tracks:"))
        panel.add(maxTracksField)
        panel.add(JLabel("Min Wait Time (minutes):"))
        panel.add(minWaitTimeField)
        panel.add(JLabel("Max Wait Time (minutes):"))
        panel.add(maxWaitTimeField)
        panel.add(JLabel("Min Plays:"))
        panel.add(minPlaysField)
        panel.add(JLabel("Max Plays:"))
        panel.add(maxPlaysField)
        panel.add(JLabel("Max Interval Between Files (seconds):"))
        panel.add(maxIntervalBetweenFilesField)
        panel.add(JLabel("MP3 Directory:"))
        panel.add(mp3DirectoryField)

        val saveButton = JButton("Save")
        saveButton.addActionListener {
            config.setProperty("startTime", startTimeField.text)
            config.setProperty("endTime", endTimeField.text)
            config.setProperty("minTracks", minTracksField.text)
            config.setProperty("maxTracks", maxTracksField.text)
            config.setProperty("minWaitTime", minWaitTimeField.text)
            config.setProperty("maxWaitTime", maxWaitTimeField.text)
            config.setProperty("minPlays", minPlaysField.text)
            config.setProperty("maxPlays", maxPlaysField.text)
            config.setProperty("maxIntervalBetweenFiles", maxIntervalBetweenFilesField.text)
            config.setProperty("mp3Directory", mp3DirectoryField.text)

            player.saveConfig()
            isVisible = false
        }

        add(panel, BorderLayout.CENTER)
        add(saveButton, BorderLayout.SOUTH)
        pack()
    }
}
