package br.com.cleannow.excecao;

public class PagamentoException extends CleanNowException {
    public PagamentoException(String mensagem) {
        super(mensagem);
    }

    public PagamentoException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
