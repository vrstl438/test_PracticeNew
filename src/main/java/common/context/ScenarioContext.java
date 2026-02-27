package common.context;

import common.datakeys.Key;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ScenarioContext {
    private static final Map<Key, Object> storage = new ConcurrentHashMap<>();

    public void saveData(Key key, Object value){
        storage.put(key, value);
    }

    public Optional<Object> getData(Key key){
        return Optional.ofNullable(storage.get(key));
    }

    public <T> Optional<T> getOptionalData(Key key, Class<T> clazz){
        if (clazz.isInstance(storage.get(key)) || storage.get(key) == null){
            return Optional.ofNullable((T) storage.get(key));
        }
        throw new IllegalStateException(
                "Value in context for key '" + key + "' has type " + storage.get(key).getClass().getName()
                        + ", expected " + clazz.getName()
        );
    }

    public <T> T getData (Key key, Class<T> clazz){
        return getOptionalData(key, clazz).orElseThrow(() ->
                new IllegalStateException(
                        "Required value is missing in context for key '" + key + "' (expected " + clazz.getName() + ")"
                )
        );
    }

    public void resetContext(){
        storage.clear();
    }
}
