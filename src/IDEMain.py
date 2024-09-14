import os
import tkinter as tk
from tkinter import filedialog, messagebox, simpledialog
from tkinter.scrolledtext import ScrolledText
from tkinter import ttk


class IDEMain(tk.Tk):
    def __init__(self):
        super().__init__()

        self.title("IDE")
        self.geometry("800x600")

        # Crear el área de texto
        self.textArea = ScrolledText(self, undo=True)
        self.textArea.pack(side=tk.RIGHT, fill=tk.BOTH, expand=True)

        # Crear el área para los números de línea
        self.lineNumbersTA = tk.Text(self, width=4, background="lightgray", state=tk.DISABLED)
        self.lineNumbersTA.pack(side=tk.LEFT, fill=tk.Y)

        # Crear el árbol de archivos
        self.fileTree = ttk.Treeview(self)
        self.fileTree.pack(side=tk.LEFT, fill=tk.Y)

        self.update_file_tree()

        # Crear un menú
        menubar = tk.Menu(self)
        self.config(menu=menubar)

        # Menú de Archivo
        fileMenu = tk.Menu(menubar, tearoff=0)
        fileMenu.add_command(label="Nuevo", command=self.new_file)
        fileMenu.add_command(label="Abrir", command=self.open_file)
        fileMenu.add_command(label="Guardar", command=self.save_file)
        menubar.add_cascade(label="Archivo", menu=fileMenu)

        # Menú de Edición
        editMenu = tk.Menu(menubar, tearoff=0)
        editMenu.add_command(label="Deshacer", command=self.undo)
        editMenu.add_command(label="Rehacer", command=self.redo)
        menubar.add_cascade(label="Edición", menu=editMenu)

        # Menú de Ver
        viewMenu = tk.Menu(menubar, tearoff=0)
        viewMenu.add_command(label="Cambiar tema", command=self.toggle_mode)
        menubar.add_cascade(label="Ver", menu=viewMenu)

        # Eventos de edición
        self.textArea.bind("<KeyRelease>", self.update_line_numbers)

    def update_file_tree(self):
        """ Actualiza el árbol de archivos basado en el directorio actual. """
        current_directory = os.getcwd()
        root_node = self.fileTree.insert("", "end", text=current_directory, open=True)
        self.add_files_to_node(current_directory, root_node)

    def add_files_to_node(self, directory, node):
        """ Agrega archivos y directorios al árbol de archivos. """
        for item in os.listdir(directory):
            path = os.path.join(directory, item)
            child_node = self.fileTree.insert(node, "end", text=item, open=False)
            if os.path.isdir(path):
                self.add_files_to_node(path, child_node)

    def update_line_numbers(self, event=None):
        """ Actualiza los números de línea del área de texto. """
        lines = int(self.textArea.index('end').split('.')[0]) - 1
        line_numbers = "\n".join(str(i) for i in range(1, lines + 1))
        self.lineNumbersTA.config(state=tk.NORMAL)
        self.lineNumbersTA.delete(1.0, tk.END)
        self.lineNumbersTA.insert(tk.END, line_numbers)
        self.lineNumbersTA.config(state=tk.DISABLED)

    def new_file(self):
        """ Crea un nuevo archivo (básicamente borra el área de texto). """
        self.textArea.delete(1.0, tk.END)

    def open_file(self):
        """ Abre un archivo y lo muestra en el área de texto. """
        file_path = filedialog.askopenfilename()
        if file_path:
            with open(file_path, 'r') as file:
                content = file.read()
            self.textArea.delete(1.0, tk.END)
            self.textArea.insert(tk.END, content)
            self.update_line_numbers()

    def save_file(self):
        """ Guarda el archivo actual. """
        file_path = filedialog.asksaveasfilename(defaultextension=".txt", filetypes=[("Text files", "*.txt"), ("All files", "*.*")])
        if file_path:
            with open(file_path, 'w') as file:
                file.write(self.textArea.get(1.0, tk.END))
            messagebox.showinfo("Éxito", "Archivo guardado con éxito")

    def undo(self):
        """ Deshace la última acción. """
        self.textArea.edit_undo()

    def redo(self):
        """ Rehace la última acción deshecha. """
        self.textArea.edit_redo()

    def toggle_mode(self):
        """ Cambia entre modo oscuro y modo claro. """
        if self.textArea["background"] == "black":
            self.textArea.config(bg="white", fg="black")
        else:
            self.textArea.config(bg="black", fg="white")


if __name__ == "__main__":
    app = IDEMain()
    app.mainloop()
