package br.com.cleannow.excecao;

public class AgendaLotadaException extends CleanNowException {
    public AgendaLotadaException(String mensagem) {
        super(mensagem);
    }

    public AgendaLotadaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
