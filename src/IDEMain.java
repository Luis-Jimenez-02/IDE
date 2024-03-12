import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import javax.swing.undo.UndoManager;

public class IDEMain extends JFrame {
    private JTextArea textArea;
    private JTextArea lineNumbersTA;
    private UndoManager undoManager;
    private JTree fileTree;

    public IDEMain() {
        setTitle("IDE");
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        textArea = new JTextArea();
        JScrollPane scrollPane = new JScrollPane(textArea);

        //Estilos para los numeros de linea
        lineNumbersTA = new JTextArea();
        lineNumbersTA.setEditable(false);
        lineNumbersTA.setBackground(Color.LIGHT_GRAY);
        lineNumbersTA.setFont(textArea.getFont());


        // Creamos el JTree y lo inicializamos con la estructura de archivos de la carpeta actual de trabajo
        File currentDirectory = new File(System.getProperty("user.dir"));
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(currentDirectory.getName());
        addFilesToNode(currentDirectory, rootNode);
        fileTree = new JTree(rootNode);

        JScrollPane treeScrollPane = new JScrollPane(fileTree);

        //Declaramos el SplitPAne para el arbol de archivos
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScrollPane, scrollPane);
        splitPane.setResizeWeight(0.2);

        //Creamos un Scroll Pane para los numeros de linea
        JScrollPane lineNumbersSP = new JScrollPane(lineNumbersTA);
        lineNumbersSP.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

        splitPane.setLeftComponent(treeScrollPane);
        splitPane.setRightComponent(scrollPane);

        scrollPane.setRowHeaderView(lineNumbersSP);

        add(splitPane, BorderLayout.CENTER);

        undoManager = new UndoManager();

        textArea.getDocument().addUndoableEditListener(new UndoableEditListener() {
            @Override
            public void undoableEditHappened(UndoableEditEvent e) {
                undoManager.addEdit(e.getEdit());
            }
        });

        textArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e){
                updateLineNumbers();
            }

            @Override
            public void removeUpdate(DocumentEvent e){
                updateLineNumbers();
            }

            @Override
            public void changedUpdate(DocumentEvent e){
                updateLineNumbers();
            }
        });


        //Titulos de la barra menu superior
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenu editMenu = new JMenu("Edit");
        JMenu viewMenu = new JMenu("View");


        //Elementos de File
        JMenuItem saveItem = new JMenuItem("Save");
        JMenuItem newItem = new JMenuItem("New");
        JMenuItem openItem = new JMenuItem("Open");

        //Elementos de Edit
        JMenuItem undoItem = new JMenuItem("Undo");
        JMenuItem redoItem = new JMenuItem("Redo");

        //Elementos de view
        JMenuItem modeItem = new JMenuItem("toogle light/dark mode");
        JMenuItem fontItem = new JMenuItem("Select Font Size");
        JMenuItem appearanceItem = new JMenuItem("Appearance");

        //Funcionalidades de los elementos de File
        saveItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveFile();
            }
        });

        newItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                newFile();
            }
        });

        openItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openFile();
            }
        });

        //Funcionalidad de los elementos de Edit
        undoItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                undo();
            }
        });

        redoItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                redo();
            }
        });

        //Funcionalidades de los elementos de view menu
        modeItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e){
                if(textArea.getBackground().equals(Color.WHITE)){
                    //Cambiar a dark
                    textArea.setBackground(Color.BLACK);
                    textArea.setForeground(Color.WHITE);
                    fileTree.setBackground(Color.BLACK);
                    fileTree.setForeground(Color.WHITE);
                }else{
                    //Cambiar a white
                    textArea.setBackground(Color.WHITE);
                    textArea.setForeground(Color.BLACK);
                    fileTree.setBackground(Color.WHITE);
                    fileTree.setForeground(Color.BLACK);
                }
            }
        });

        //Mostarr los titulos y elementos
        fileMenu.add(newItem);
        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        editMenu.add(undoItem);
        editMenu.add(redoItem);
        viewMenu.add(modeItem);
        viewMenu.add(fontItem);
        viewMenu.add(appearanceItem);
        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(viewMenu);
        setJMenuBar(menuBar);
    }

    private void saveFile() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(textArea.getText());
                JOptionPane.showMessageDialog(this, "File saved successfully!");
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error saving file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void newFile() {
        IDEMain newIDE = new IDEMain();
        newIDE.setVisible(true);
    }

    private void openFile() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (FileReader reader = new FileReader(file)) {
                char[] buffer = new char[(int) file.length()];
                reader.read(buffer);
                textArea.setText(new String(buffer));
                JOptionPane.showMessageDialog(this, "File opened successfully!");
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error opening file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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

    // Método para agregar archivos y directorios a un nodo del árbol
    private void addFilesToNode(File file, DefaultMutableTreeNode node) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(f.getName());
                    node.add(childNode);
                    addFilesToNode(f, childNode);
                }
            }
        }
    }

    private void updateLineNumbers(){
        int totalLines = textArea.getLineCount();
        StringBuilder numbersText = new StringBuilder();
        for (int i = 1; i <= totalLines; i++){
            numbersText.append(i).append("\n");
        }

        lineNumbersTA.setText(numbersText.toString());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                IDEMain ide = new IDEMain();
                ide.setVisible(true);
            }
        });
    }
}

