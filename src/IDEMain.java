import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.*;
import javax.swing.tree.*;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Arrays;

public class IDEMain extends JFrame {
    private JTextPane textPane;
    private JTextArea lineNumbersTA;
    private UndoManager undoManager;
    private JTree fileTree;
    private JTextArea errorTextArea;
    private JTextArea resultTextArea;
    private JTextArea sintacticoTextArea;
    private JTextArea semanticoTextArea;
    private JTextArea intermedioTextArea;
    private File currentFile;
    private JTable lexicoTable;
    private boolean lexicoExecuted = false;

    private static final String TITLE = "IDE";
    private static final String FILE_MENU_TITLE = "File";
    private static final String EDIT_MENU_TITLE = "Edit";
    private static final String VIEW_MENU_TITLE = "View";
    private static final String COMPILAR_MENU_TITLE = "Compile";

    public IDEMain() {
        setTitle(TITLE);
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initializeComponents();
        setupMenu();
        setupListeners();
    }

    private void initializeComponents() {
        textPane = new JTextPane();
        lineNumbersTA = new JTextArea();
        lineNumbersTA.setEditable(false);
        lineNumbersTA.setBackground(Color.LIGHT_GRAY);
        lineNumbersTA.setFont(textPane.getFont());

        JScrollPane scrollPane = new JScrollPane(textPane);
        JScrollPane lineNumbersSP = new JScrollPane(lineNumbersTA);
        lineNumbersSP.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.setRowHeaderView(lineNumbersSP);

        fileTree = createFileTree();
        JScrollPane treeScrollPane = new JScrollPane(fileTree);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScrollPane, scrollPane);
        splitPane.setResizeWeight(0.1);

        undoManager = new UndoManager();
        textPane.getDocument().addUndoableEditListener(e -> undoManager.addEdit(e.getEdit()));
        textPane.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                updateLineNumbers();
                highlightSyntax();
            }
            public void removeUpdate(DocumentEvent e) {
                updateLineNumbers();
                highlightSyntax();
            }
            public void changedUpdate(DocumentEvent e) {
                updateLineNumbers();
                highlightSyntax();
            }
        });

        errorTextArea = new JTextArea();
        resultTextArea = new JTextArea();
        sintacticoTextArea = new JTextArea();
        semanticoTextArea = new JTextArea();
        intermedioTextArea = new JTextArea();

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Errores", new JScrollPane(errorTextArea));
        tabbedPane.addTab("Resultados", new JScrollPane(resultTextArea));

        String[] columnNames = {"Clave", "Lexema", "Fila", "Columna"};
        lexicoTable = new JTable(new DefaultTableModel(columnNames, 0));

        JTabbedPane tabbedPane2 = new JTabbedPane();
        tabbedPane2.addTab("Lexico", new JScrollPane(lexicoTable));
        tabbedPane2.addTab("Sintactico", new JScrollPane(sintacticoTextArea));
        tabbedPane2.addTab("Semantico", new JScrollPane(semanticoTextArea));
        tabbedPane2.addTab("Codigo intermedio", new JScrollPane(intermedioTextArea));

        JSplitPane splitPane2 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, splitPane, tabbedPane2);
        splitPane2.setResizeWeight(0.8);

        add(splitPane2, BorderLayout.CENTER);
    }

    private JTree createFileTree() {
        File currentDirectory = new File(System.getProperty("user.dir"));
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(currentDirectory.getName());
        addFilesToNode(currentDirectory, rootNode);
        return new JTree(rootNode);
    }

    private void setupMenu() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = createMenu(FILE_MENU_TITLE, new String[]{"New", "Open", "Save"}, new ActionListener[]{this::newFile, this::openFile, this::saveFile});
        JMenu editMenu = createMenu(EDIT_MENU_TITLE, new String[]{"Undo", "Redo"}, new ActionListener[]{e -> undo(), e -> redo()});
        JMenu viewMenu = createMenu(VIEW_MENU_TITLE, new String[]{"toggle light/dark mode"}, new ActionListener[]{e -> toggleLightDarkMode()});
        JMenu compilarMenu = createMenu(COMPILAR_MENU_TITLE, new String[]{"Lexico", "Sintactico"}, new ActionListener[]{e -> runLexicalAnalyzer(), e -> runSintacticAnalyzer()});

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(viewMenu);
        menuBar.add(compilarMenu);

        setJMenuBar(menuBar);
    }

    private JMenu createMenu(String title, String[] itemNames, ActionListener[] actions) {
        JMenu menu = new JMenu(title);
        for (int i = 0; i < itemNames.length; i++) {
            JMenuItem item = new JMenuItem(itemNames[i]);
            item.addActionListener(actions[i]);
            menu.add(item);
        }
        return menu;
    }

    private void setupListeners() {
        // Add specific listeners here if needed
    }

    private void saveFile(ActionEvent e) {
        if (currentFile == null) {
            JFileChooser fileChooser = new JFileChooser();
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                currentFile = fileChooser.getSelectedFile();
            }
        }
        if (currentFile != null) {
            try (FileWriter writer = new FileWriter(currentFile)) {
                writer.write(textPane.getText());
                JOptionPane.showMessageDialog(this, "File saved successfully!");
            } catch (IOException ex) {
                showErrorDialog("Error saving file: " + ex.getMessage());
            }
        }
    }

    private void newFile(ActionEvent e) {
        new IDEMain().setVisible(true);
    }

    private void openFile(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            currentFile = fileChooser.getSelectedFile();
            try (BufferedReader reader = new BufferedReader(new FileReader(currentFile))) {
                textPane.read(reader, null);
                updateLineNumbers();
                highlightSyntax();
            } catch (IOException ex) {
                showErrorDialog("Error opening file: " + ex.getMessage());
            }
        }
    }

    private void runLexicalAnalyzer(ActionEvent e) {
        if (currentFile == null || !isFileContentSame()) {
            int choice = JOptionPane.showConfirmDialog(this, "El archivo no está guardado. ¿Desea guardarlo antes de compilar?", "Guardar archivo", JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) {
                saveFile(null);
            } else {
                return;
            }
        }
        runLexicalAnalyzer();
    }

    private void runSintacticAnalyzer(ActionEvent e) {
        if (!lexicoExecuted) {
            JOptionPane.showMessageDialog(this, "Debe ejecutar el análisis léxico antes del análisis sintáctico.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        runSintacticAnalyzer();
    }

    private boolean isFileContentSame() {
        if (currentFile == null) return false;
        try (BufferedReader reader = new BufferedReader(new FileReader(currentFile))) {
            StringBuilder fileContent = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                fileContent.append(line).append("\n");
            }
            return textPane.getText().equals(fileContent.toString().trim());
        } catch (IOException e) {
            return false;
        }
    }

    private void runLexicalAnalyzer() {
        if (currentFile == null) return;
        try {
            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "lexico", currentFile.getAbsolutePath());
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            DefaultTableModel model = (DefaultTableModel) lexicoTable.getModel();
            model.setRowCount(0);

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(": |\\(Row: |, Column: |\\)");
                if (parts.length >= 4) {
                    model.addRow(new Object[]{parts[0].trim(), parts[1].trim(), parts[2].trim(), parts[3].trim()});
                }
            }
            process.waitFor();
            lexicoExecuted = true;
        } catch (Exception e) {
            showErrorDialog("Error running lexical analyzer: " + e.getMessage());
        }
    }

    private void runSintacticAnalyzer() {
        if (currentFile == null) return;
        try {
            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "sintactico", currentFile.getAbsolutePath());
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            sintacticoTextArea.setText("");

            String line;
            while ((line = reader.readLine()) != null) {
                sintacticoTextArea.append(line + "\n");
            }
            process.waitFor();
        } catch (Exception e) {
            showErrorDialog("Error running sintactic analyzer: " + e.getMessage());
        }
    }

    private void updateLineNumbers() {
        int totalLines = textPane.getDocument().getDefaultRootElement().getElementCount();
        StringBuilder lineNumbers = new StringBuilder();
        for (int i = 1; i <= totalLines; i++) {
            lineNumbers.append(i).append("\n");
        }
        lineNumbersTA.setText(lineNumbers.toString());
    }

    private void highlightSyntax() {
        SwingUtilities.invokeLater(() -> {
            StyledDocument doc = textPane.getStyledDocument();
            StyleContext sc = StyleContext.getDefaultStyleContext();
            AttributeSet defaultStyle = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, Color.BLACK);
            AttributeSet keywordStyle = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, Color.BLUE);
            AttributeSet numberStyle = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, Color.MAGENTA);

            doc.setCharacterAttributes(0, doc.getLength(), defaultStyle, true);

            String text = textPane.getText();
            String[] keywords = {"if", "else", "for", "while", "return", "int", "float", "double", "char", "boolean"};
            for (String keyword : keywords) {
                int pos = 0;
                while ((pos = text.indexOf(keyword, pos)) >= 0) {
                    doc.setCharacterAttributes(pos, keyword.length(), keywordStyle, true);
                    pos += keyword.length();
                }
            }

            for (int i = 0; i < text.length(); i++) {
                if (Character.isDigit(text.charAt(i))) {
                    int j = i;
                    while (j < text.length() && Character.isDigit(text.charAt(j))) {
                        j++;
                    }
                    doc.setCharacterAttributes(i, j - i, numberStyle, true);
                    i = j - 1;
                }
            }
        });
    }

    private void addFilesToNode(File file, DefaultMutableTreeNode node) {
        File[] files = file.listFiles();
        if (files != null) {
            for (File f : files) {
                DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(f.getName());
                node.add(childNode);
                if (f.isDirectory()) {
                    addFilesToNode(f, childNode);
                }
            }
        }
    }

    private void undo() {
        if (undoManager.canUndo()) {
            undoManager.undo();
        }
    }

    private void redo() {
        if (undoManager.canRedo()) {
            undoManager.redo();
        }
    }

    private void toggleLightDarkMode() {
        if (textPane.getBackground().equals(Color.WHITE)) {
            textPane.setBackground(Color.BLACK);
            textPane.setForeground(Color.WHITE);
            fileTree.setBackground(Color.BLACK);
            fileTree.setForeground(Color.WHITE);
        } else {
            textPane.setBackground(Color.WHITE);
            textPane.setForeground(Color.BLACK);
            fileTree.setBackground(Color.WHITE);
            fileTree.setForeground(Color.BLACK);
        }
    }

    private void showErrorDialog(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new IDEMain().setVisible(true));
    }
}
