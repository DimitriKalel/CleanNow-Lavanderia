package br.com.cleannow.repositorio;

import br.com.cleannow.modelo.Identificavel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RepositorioMemoria<T extends Identificavel> implements Repositorio<T> {
    protected final Map<String, T> dados = new LinkedHashMap<>();

    @Override
    public void salvar(T entidade) {
        dados.put(entidade.getId(), entidade);
        aposAlteracao();
    }

    @Override
    public Optional<T> buscarPorId(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(dados.get(id.trim().toUpperCase()))
                .or(() -> Optional.ofNullable(dados.get(id.trim())));
    }

    @Override
    public List<T> listarTodos() {
        return new ArrayList<>(dados.values());
    }

    @Override
    public boolean remover(String id) {
        boolean removido = buscarPorId(id).map(e -> dados.remove(e.getId()) != null).orElse(false);
        if (removido) {
            aposAlteracao();
        }
        return removido;
    }

    protected void aposAlteracao() {
    }
}
