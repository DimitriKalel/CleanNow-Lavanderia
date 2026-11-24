package br.com.cleannow.excecao;

public class PersistenciaException extends CleanNowException {
    public PersistenciaException(String mensagem) {
        super(mensagem);
    }

    public PersistenciaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
