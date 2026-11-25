package br.com.cleannow.repositorio;

import br.com.cleannow.modelo.Identificavel;

import java.util.List;
import java.util.Optional;

public interface Repositorio<T extends Identificavel> {
    void salvar(T entidade);

    Optional<T> buscarPorId(String id);

    List<T> listarTodos();

    boolean remover(String id);

    default boolean existe(String id) {
        return buscarPorId(id).isPresent();
    }

    default int quantidade() {
        return listarTodos().size();
    }
}
