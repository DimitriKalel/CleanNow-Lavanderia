package br.com.cleannow.repositorio;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public class AdaptadorPolimorfico<T> implements JsonSerializer<T>, JsonDeserializer<T> {
    private static final String CAMPO = "tipo";

    private final Map<String, Class<? extends T>> classesPorTipo = new LinkedHashMap<>();
    private final Function<T, String> obterTipo;

    public AdaptadorPolimorfico(Function<T, String> obterTipo) {
        this.obterTipo = obterTipo;
    }

    public AdaptadorPolimorfico<T> registrar(String tipo, Class<? extends T> classe) {
        classesPorTipo.put(tipo, classe);
        return this;
    }

    @Override
    public JsonElement serialize(T origem, Type tipo, JsonSerializationContext ctx) {
        JsonObject obj = ctx.serialize(origem, origem.getClass()).getAsJsonObject();
        obj.addProperty(CAMPO, obterTipo.apply(origem));
        return obj;
    }

    @Override
    public T deserialize(JsonElement json, Type tipo, JsonDeserializationContext ctx) {
        JsonObject obj = json.getAsJsonObject();
        JsonElement campo = obj.get(CAMPO);
        if (campo == null) {
            throw new JsonParseException("Registro sem o campo 'tipo': " + obj);
        }
        Class<? extends T> classe = classesPorTipo.get(campo.getAsString());
        if (classe == null) {
            throw new JsonParseException("Tipo desconhecido: " + campo.getAsString());
        }
        return ctx.deserialize(obj, classe);
    }
}
