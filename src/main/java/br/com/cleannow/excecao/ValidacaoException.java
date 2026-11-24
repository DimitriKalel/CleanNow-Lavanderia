package br.com.cleannow.excecao;

public class ValidacaoException extends CleanNowException {
    public ValidacaoException(String mensagem) {
        super(mensagem);
    }

    public ValidacaoException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
