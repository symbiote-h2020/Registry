package eu.h2020.symbiote.utils;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import eu.h2020.symbiote.model.Platform;

/**
 * Created by mateuszl on 11.01.2017.
 */
public class JsonConverter {

    public static Platform getPlatformFromJson(String platformJson) {
        try {
            Gson gson = new Gson();
            return gson.fromJson(platformJson, Platform.class);
        } catch (JsonSyntaxException e) {
            return null;
        }
    }
}
