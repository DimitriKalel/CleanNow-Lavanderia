package br.com.cleannow.excecao;

public class TransicaoStatusInvalidaException extends CleanNowException {
    public TransicaoStatusInvalidaException(String mensagem) {
        super(mensagem);
    }

    public TransicaoStatusInvalidaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
