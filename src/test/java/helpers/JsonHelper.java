package helpers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class JsonHelper {
    public static String pojoToJson(Object o) {
        Gson gson = new GsonBuilder().create();

        return gson.toJson(o);
    }
}
