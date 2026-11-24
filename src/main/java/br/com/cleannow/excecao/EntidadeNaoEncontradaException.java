package br.com.cleannow.excecao;

public class EntidadeNaoEncontradaException extends CleanNowException {
    public EntidadeNaoEncontradaException(String mensagem) {
        super(mensagem);
    }

    public EntidadeNaoEncontradaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
