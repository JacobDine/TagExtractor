import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

public class TagExtractorApp extends JFrame {

    private JLabel sourceFileLabel;
    private JTextArea resultArea;
    private JButton chooseTextFileBtn;
    private JButton chooseStopWordFileBtn;
    private JButton extractBtn;
    private JButton saveBtn;
    private JLabel stopWordFileLabel;
    private JLabel statusLabel;

    private File selectedTextFile;
    private File selectedStopWordFile;

    private TreeMap<String, Integer> tagFrequencyMap;

    public TagExtractorApp() {
        setTitle("Tag / Keyword Extractor");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(750, 600);
        setMinimumSize(new Dimension(600, 480));
        setLocationRelativeTo(null);

        initComponents();
        layoutComponents();
    }

    private void initComponents() {
        sourceFileLabel   = new JLabel("No text file selected");
        stopWordFileLabel = new JLabel("No stop word file selected");
        statusLabel       = new JLabel(" ");

        resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));

        chooseTextFileBtn  = new JButton("Choose Text File…");
        chooseStopWordFileBtn = new JButton("Choose Stop Word File…");
        extractBtn         = new JButton("Extract Tags");
        saveBtn            = new JButton("Save Tags to File…");

        extractBtn.setEnabled(false);
        saveBtn.setEnabled(false);

        chooseTextFileBtn.addActionListener(e -> chooseTextFile());
        chooseStopWordFileBtn.addActionListener(e -> chooseStopWordFile());
        extractBtn.addActionListener(e -> extractTags());
        saveBtn.addActionListener(e -> saveTags());
    }

    private void layoutComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(14, 14, 14, 14));

        JPanel topPanel = new JPanel(new GridBagLayout());
        topPanel.setBorder(BorderFactory.createTitledBorder("File Selection"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        topPanel.add(chooseTextFileBtn, gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        sourceFileLabel.setFont(sourceFileLabel.getFont().deriveFont(Font.ITALIC));
        topPanel.add(sourceFileLabel, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        topPanel.add(chooseStopWordFileBtn, gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        stopWordFileLabel.setFont(stopWordFileLabel.getFont().deriveFont(Font.ITALIC));
        topPanel.add(stopWordFileLabel, gbc);

        JScrollPane scrollPane = new JScrollPane(resultArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Extracted Tags  (word  →  frequency)"));

        JPanel bottomPanel = new JPanel(new BorderLayout(8, 0));
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttonRow.add(extractBtn);
        buttonRow.add(saveBtn);
        bottomPanel.add(buttonRow, BorderLayout.WEST);
        statusLabel.setForeground(Color.DARK_GRAY);
        bottomPanel.add(statusLabel, BorderLayout.CENTER);

        mainPanel.add(topPanel,    BorderLayout.NORTH);
        mainPanel.add(scrollPane,  BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private void chooseTextFile() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Select a Text File to Analyse");
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Text files (*.txt)", "txt"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            selectedTextFile = fc.getSelectedFile();
            sourceFileLabel.setText(selectedTextFile.getName());
            updateExtractButton();
        }
    }

    private void chooseStopWordFile() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Select a Stop Word File");
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Text files (*.txt)", "txt"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            selectedStopWordFile = fc.getSelectedFile();
            stopWordFileLabel.setText(selectedStopWordFile.getName());
            updateExtractButton();
        }
    }

    private void updateExtractButton() {
        extractBtn.setEnabled(selectedTextFile != null && selectedStopWordFile != null);
    }

    private void extractTags() {
        TreeSet<String> stopWords = loadStopWords(selectedStopWordFile);
        if (stopWords == null) return;   // error already shown

        tagFrequencyMap = new TreeMap<>();

        try (Scanner scanner = new Scanner(new BufferedReader(new FileReader(selectedTextFile)))) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                // Split on whitespace
                String[] tokens = line.split("\\s+");
                for (String token : tokens) {
                    // Remove every non-letter character, force lowercase
                    String word = token.replaceAll("[^a-zA-Z]", "").toLowerCase();
                    if (word.isEmpty()) continue;
                    if (stopWords.contains(word)) continue;
                    tagFrequencyMap.merge(word, 1, Integer::sum);
                }
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Error reading text file:\n" + ex.getMessage(),
                    "File Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        displayTags();
        saveBtn.setEnabled(!tagFrequencyMap.isEmpty());
        statusLabel.setText("Found " + tagFrequencyMap.size() + " unique tags.");
    }

    private TreeSet<String> loadStopWords(File stopWordFile) {
        TreeSet<String> stopWords = new TreeSet<>();
        try (Scanner scanner = new Scanner(new BufferedReader(new FileReader(stopWordFile)))) {
            while (scanner.hasNextLine()) {
                String word = scanner.nextLine().trim().toLowerCase();
                if (!word.isEmpty()) stopWords.add(word);
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Error reading stop word file:\n" + ex.getMessage(),
                    "File Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        return stopWords;
    }

    private void displayTags() {
        if (tagFrequencyMap.isEmpty()) {
            resultArea.setText("No tags found (all words were stop words or the file is empty).");
            return;
        }

        // Sort by frequency descending for easier reading
        java.util.List<Map.Entry<String, Integer>> entries = new ArrayList<>(tagFrequencyMap.entrySet());
        entries.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Source file : %s%n", selectedTextFile.getName()));
        sb.append(String.format("Total unique tags : %d%n%n", tagFrequencyMap.size()));
        sb.append(String.format("%-30s %s%n", "WORD", "FREQUENCY"));
        sb.append("-".repeat(42)).append("\n");
        for (Map.Entry<String, Integer> entry : entries) {
            sb.append(String.format("%-30s %d%n", entry.getKey(), entry.getValue()));
        }

        resultArea.setText(sb.toString());
        resultArea.setCaretPosition(0);
    }

    private void saveTags() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save Tags To…");
        fc.setSelectedFile(new File("tags_output.txt"));
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Text files (*.txt)", "txt"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File saveFile = fc.getSelectedFile();
        if (!saveFile.getName().endsWith(".txt")) {
            saveFile = new File(saveFile.getAbsolutePath() + ".txt");
        }

        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(saveFile)))) {
            pw.printf("Source file : %s%n", selectedTextFile.getName());
            pw.printf("Total unique tags : %d%n%n", tagFrequencyMap.size());
            pw.printf("%-30s %s%n", "WORD", "FREQUENCY");
            pw.println("-".repeat(42));

            java.util.List<Map.Entry<String, Integer>> entries = new ArrayList<>(tagFrequencyMap.entrySet());
            entries.sort((a, b) -> b.getValue().compareTo(a.getValue()));
            for (Map.Entry<String, Integer> entry : entries) {
                pw.printf("%-30s %d%n", entry.getKey(), entry.getValue());
            }
            statusLabel.setText("Saved to: " + saveFile.getName());
            JOptionPane.showMessageDialog(this, "Tags saved to:\n" + saveFile.getAbsolutePath(),
                    "Saved", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Error saving file:\n" + ex.getMessage(),
                    "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) {}
            new TagExtractorApp().setVisible(true);
        });
    }
}
