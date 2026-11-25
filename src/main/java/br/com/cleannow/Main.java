package br.com.cleannow;

import br.com.cleannow.excecao.CleanNowException;
import br.com.cleannow.ui.ConsoleApp;
import br.com.cleannow.ui.Entrada;
import br.com.cleannow.util.RelogioAjustavel;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        String pasta = args.length > 0 ? args[0] : "dados";
        PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        Path pastaDados = Path.of(pasta);
        RelogioAjustavel relogio = RelogioAjustavel.sistema();
        Sistema sistema;
        try {
            sistema = Sistema.comArquivos(pastaDados, Path.of("relatorios"), relogio);
        } catch (CleanNowException e) {
            out.println("Não foi possível iniciar o CleanNow: " + e.getMessage());
            out.println("Verifique se a pasta '" + pastaDados.toAbsolutePath() + "' pode ser lida e gravada.");
            return;
        }
        Entrada entrada = new Entrada(new Scanner(System.in, StandardCharsets.UTF_8), out, relogio);
        new ConsoleApp(sistema, entrada, out).iniciar();
    }
}
