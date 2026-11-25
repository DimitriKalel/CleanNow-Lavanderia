package br.com.cleannow.repositorio;

import br.com.cleannow.modelo.cliente.Cliente;
import br.com.cleannow.modelo.cliente.ClienteComum;
import br.com.cleannow.modelo.cliente.ClienteVIP;
import br.com.cleannow.modelo.servico.Lavagem;
import br.com.cleannow.modelo.servico.PassarRoupa;
import br.com.cleannow.modelo.servico.Secagem;
import br.com.cleannow.modelo.servico.Servico;
import br.com.cleannow.modelo.servico.ServicoPorKg;
import br.com.cleannow.modelo.servico.ServicoPorPeca;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.function.Function;

public final class GsonFactory {
    private GsonFactory() {
    }

    public static Gson criar() {
        AdaptadorPolimorfico<Cliente> clientes = new AdaptadorPolimorfico<>(Cliente::getTipo)
                .registrar(ClienteComum.TIPO, ClienteComum.class)
                .registrar(ClienteVIP.TIPO, ClienteVIP.class);

        AdaptadorPolimorfico<Servico> servicos = new AdaptadorPolimorfico<>(Servico::getTipo)
                .registrar(Lavagem.TIPO, Lavagem.class)
                .registrar(Secagem.TIPO, Secagem.class)
                .registrar(PassarRoupa.TIPO, PassarRoupa.class)
                .registrar(ServicoPorKg.TIPO, ServicoPorKg.class)
                .registrar(ServicoPorPeca.TIPO, ServicoPorPeca.class);

        return new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Cliente.class, clientes)
                .registerTypeAdapter(Servico.class, servicos)
                .registerTypeAdapter(LocalDate.class, texto(LocalDate::toString, LocalDate::parse))
                .registerTypeAdapter(LocalDateTime.class, texto(LocalDateTime::toString, LocalDateTime::parse))
                .registerTypeAdapter(YearMonth.class, texto(YearMonth::toString, YearMonth::parse))
                .create();
    }

    private static <V> TypeAdapter<V> texto(Function<V, String> escrever, Function<String, V> ler) {
        return new TypeAdapter<V>() {
            @Override
            public void write(JsonWriter out, V valor) throws IOException {
                if (valor == null) {
                    out.nullValue();
                } else {
                    out.value(escrever.apply(valor));
                }
            }

            @Override
            public V read(JsonReader in) throws IOException {
                if (in.peek() == JsonToken.NULL) {
                    in.nextNull();
                    return null;
                }
                return ler.apply(in.nextString());
            }
        };
    }
}
