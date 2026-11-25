package br.com.cleannow.repositorio;

import br.com.cleannow.excecao.PersistenciaException;
import br.com.cleannow.modelo.Identificavel;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class RepositorioJson<T extends Identificavel> extends RepositorioMemoria<T> {
    private final Path arquivo;
    private final Gson gson;
    private final Type tipoLista;
    private final List<String> avisos = new ArrayList<>();

    public RepositorioJson(Path arquivo, Gson gson, Type tipoLista) {
        this.arquivo = arquivo;
        this.gson = gson;
        this.tipoLista = tipoLista;
        carregar();
    }

    private void carregar() {
        if (!Files.exists(arquivo)) {
            return;
        }
        try (Reader r = Files.newBufferedReader(arquivo, StandardCharsets.UTF_8)) {
            List<T> lista = gson.fromJson(r, tipoLista);
            if (lista != null) {
                for (T item : lista) {
                    if (item != null && item.getId() != null) {
                        dados.put(item.getId(), item);
                    }
                }
            }
        } catch (IOException e) {
            throw new PersistenciaException("Não foi possível ler " + arquivo + ": " + e.getMessage(), e);
        } catch (RuntimeException e) {
            isolarArquivoCorrompido(e);
        }
    }

    private void isolarArquivoCorrompido(Exception causa) {
        dados.clear();
        String sufixo = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path copia = arquivo.resolveSibling(arquivo.getFileName() + ".corrompido-" + sufixo);
        try {
            Files.move(arquivo, copia, StandardCopyOption.REPLACE_EXISTING);
            avisos.add("Arquivo " + arquivo.getFileName() + " estava corrompido e foi movido para "
                    + copia.getFileName() + ". Os dados desse arquivo começaram vazios. (" + causa.getMessage() + ")");
        } catch (IOException e) {
            avisos.add("Arquivo " + arquivo.getFileName() + " está corrompido e não pôde ser movido: " + e.getMessage());
        }
    }

    @Override
    protected void aposAlteracao() {
        gravar();
    }

    public void gravar() {
        try {
            Path pasta = arquivo.toAbsolutePath().getParent();
            if (pasta != null) {
                Files.createDirectories(pasta);
            }
            Path temporario = arquivo.resolveSibling(arquivo.getFileName() + ".tmp");
            try (Writer w = Files.newBufferedWriter(temporario, StandardCharsets.UTF_8)) {
                gson.toJson(new ArrayList<>(dados.values()), tipoLista, w);
            }
            Files.move(temporario, arquivo, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new PersistenciaException("Não foi possível gravar " + arquivo + ": " + e.getMessage(), e);
        }
    }

    public List<String> getAvisos() {
        return avisos;
    }

    public Path getArquivo() {
        return arquivo;
    }
}
