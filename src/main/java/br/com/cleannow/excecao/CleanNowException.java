package br.com.cleannow.excecao;

public class CleanNowException extends RuntimeException {
    public CleanNowException(String mensagem) {
        super(mensagem);
    }

    public CleanNowException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
