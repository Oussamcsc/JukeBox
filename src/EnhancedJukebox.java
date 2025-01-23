import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.util.*;
import javax.sound.sampled.*;

public class EnhancedJukebox {
    private Clip audioClip;
    private JLabel albumCoverLabel;
    private JPanel visualizerPanel;
    private String[] audioFiles;
    private String[] coverFiles;
    private int currentTrackIndex = -1;  // Start with no track selected
    private final String defaultImage = "default.jpg"; // Default image filename
    private JukeBoxConfig config;
    private final String configFile = "JBconfig.ser";

    public EnhancedJukebox() {
        config = loadConfig();
        loadMediaFiles(config.getMediaFolder());
        createAndShowGUI();
    }

    /**
     * Loads configuration from the serialized config file or creates a default one.
     */
    private JukeBoxConfig loadConfig() {
        File file = new File(configFile);
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                return (JukeBoxConfig) ois.readObject();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Error loading config. Using defaults.");
            }
        }
        return new JukeBoxConfig("Songs and cover images");
    }

    /**
     * Saves the current configuration to a serialized file.
     */
    private void saveConfig() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(configFile))) {
            oos.writeObject(config);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Error saving config: " + e.getMessage());
        }
    }

    /**
     * Load audio and cover files from the specified directory.
     */
    private void loadMediaFiles(String directoryPath) {
        File dir = new File(directoryPath);
        if (!dir.exists() || !dir.isDirectory()) {
            JOptionPane.showMessageDialog(null, "Media directory not found!");
            System.exit(1);
        }

        // Load and sort audio files
        File[] audioFileArray = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".wav"));
        if (audioFileArray == null || audioFileArray.length == 0) {
            JOptionPane.showMessageDialog(null, "No .wav files found in the media directory!");
            System.exit(1);
        }
        Arrays.sort(audioFileArray); // Sort alphabetically
        audioFiles = Arrays.stream(audioFileArray).map(File::getAbsolutePath).toArray(String[]::new);

        // Load and sort cover files
        File[] coverFileArray = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".jpg"));
        Arrays.sort(coverFileArray); // Sort alphabetically
        coverFiles = coverFileArray == null ? new String[0] : Arrays.stream(coverFileArray).map(File::getAbsolutePath).toArray(String[]::new);
    }

    /**
     * Plays the current audio track.
     */
    private void playAudio() {
        try {
            if (audioClip != null && audioClip.isRunning()) {
                audioClip.stop();
            }

            File audioFile = new File(audioFiles[currentTrackIndex]);
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
            audioClip = AudioSystem.getClip();
            audioClip.open(audioStream);
            audioClip.start();

            // Update the visualizer while the music is playing
            javax.swing.Timer visualizerTimer = new javax.swing.Timer(100, (ActionEvent e) -> visualizerPanel.repaint());
            visualizerTimer.start();

            audioClip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    visualizerTimer.stop();
                }
            });

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Error playing audio: " + ex.getMessage());
        }
    }

    /**
     * Creates and displays the Jukebox GUI.
     */
    private void createAndShowGUI() {
        JFrame frame = new JFrame("Enhanced Jukebox");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);

        // Control Buttons
        JButton playButton = new JButton("Play");
        JButton stopButton = new JButton("Stop");
        JButton rewindButton = new JButton("Rewind");
        JButton nextTrackButton = new JButton("Next Track");
        JButton chooseJamButton = new JButton("Choose a Jam!");

        // Album Cover Panel
        albumCoverLabel = new JLabel();
        albumCoverLabel.setHorizontalAlignment(JLabel.CENTER);
        albumCoverLabel.setPreferredSize(new Dimension(300, 300)); // Set fixed size for the album cover
        updateAlbumCover();  // Show default image at start

        // Visualizer Panel
        visualizerPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawRandomShapes(g);
            }
        };
        visualizerPanel.setBackground(new Color(30, 30, 30)); // Dark background for visualizer panel

        // Set up button actions using lambdas
        playButton.addActionListener(e -> playAudio());
        stopButton.addActionListener(e -> stopAudio());
        rewindButton.addActionListener(e -> rewindAudio());
        nextTrackButton.addActionListener(e -> nextTrack());
        chooseJamButton.addActionListener(e -> chooseJam());

        // Layout
        JPanel controlPanel = new JPanel();
        controlPanel.setBackground(new Color(50, 50, 50)); // Dark gray for control panel
        controlPanel.add(playButton);
        controlPanel.add(stopButton);
        controlPanel.add(rewindButton);
        controlPanel.add(nextTrackButton);
        controlPanel.add(chooseJamButton);

        frame.setLayout(new BorderLayout());
        frame.add(controlPanel, BorderLayout.SOUTH);
        frame.add(albumCoverLabel, BorderLayout.NORTH);
        frame.add(visualizerPanel, BorderLayout.CENTER);

        frame.setVisible(true);
    }

    private void stopAudio() {
        if (audioClip != null && audioClip.isRunning()) {
            audioClip.stop();
        }
    }

    /**
     * Rewinds the current audio track to the beginning.
     */
    private void rewindAudio() {
        if (audioClip != null) {
            audioClip.setFramePosition(0);
            audioClip.start();
        }
    }

    /**
     * Plays the next track in the playlist.
     */
    private void nextTrack() {
        currentTrackIndex = (currentTrackIndex + 1) % audioFiles.length;
        playAudio();
        updateAlbumCover();
    }

    /**
     * Allows the user to choose a song from the list.
     */
    private void chooseJam() {
        String[] songNames = Arrays.stream(audioFiles).map(file -> new File(file).getName()).toArray(String[]::new);
        String selectedSong = (String) JOptionPane.showInputDialog(null, "Choose a Jam!", "Song Selector",
                JOptionPane.PLAIN_MESSAGE, null, songNames, songNames[0]);

        if (selectedSong != null) {
            for (int i = 0; i < audioFiles.length; i++) {
                if (audioFiles[i].endsWith(selectedSong)) {
                    currentTrackIndex = i;
                    break;
                }
            }
            playAudio();
            updateAlbumCover();
        }
    }

    /**
     * Updates the album cover based on the current track.
     */
    private void updateAlbumCover() {
        String mediaFolder = config.getMediaFolder();
        File defaultImageFile = new File(mediaFolder, defaultImage);

        if (currentTrackIndex >= 0 && currentTrackIndex < coverFiles.length) {
            albumCoverLabel.setIcon(new ImageIcon(new ImageIcon(coverFiles[currentTrackIndex]).getImage().getScaledInstance(300, 300, Image.SCALE_SMOOTH)));
        } else if (defaultImageFile.exists()) {
            albumCoverLabel.setIcon(new ImageIcon(defaultImageFile.getAbsolutePath()));
        } else {
            albumCoverLabel.setIcon(null); // Clear if default image is missing
        }
    }

    /**
     * Draws random shapes as a visualizer.
     */
    private void drawRandomShapes(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        int width = visualizerPanel.getWidth();
        int height = visualizerPanel.getHeight();

        // Use fewer shapes but increase their size and color variety
        for (int i = 0; i < 5; i++) {
            g2d.setColor(new Color((int) (Math.random() * 0xFFFFFF)));
            int x = (int) (Math.random() * width);
            int y = (int) (Math.random() * height);
            int size = (int) (Math.random() * 100); // Larger shapes
            g2d.fillOval(x, y, size, size);  // Draw the larger, random-shaped ovals
        }
    }


    /**
     * Main method to launch the Jukebox application.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(EnhancedJukebox::new);
    }
}

