package com.skillbox.redisdemo;
import org.redisson.Redisson;
import org.redisson.api.RKeys;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisConnectionException;
import org.redisson.config.Config;
import java.util.Date;
import static java.lang.System.out;

public class RedisStorage {

    private RedissonClient redisson;
    private RKeys rKeys;
    private RScoredSortedSet<String> onlineUsers;
    private final static String KEY = "ONLINE_USERS";
    private RScoredSortedSet<String> registeredUsers;
    private final static String KEY_1 = "REGISTERED_USERS";

    private double getTs() {
        return new Date().getTime();
    }

    void init() {
        Config config = new Config();
        config.setCodec(new org.redisson.client.codec.StringCodec()); // вместо FST
        config.useSingleServer().setAddress("redis://127.0.0.1:6379");
        try {
            redisson = Redisson.create(config);
        } catch (RedisConnectionException Exc) {
            out.println("Не удалось подключиться к Redis");
            out.println(Exc.getMessage());
        }
        rKeys = redisson.getKeys();
        onlineUsers = redisson.getScoredSortedSet(KEY);
        registeredUsers = redisson.getScoredSortedSet(KEY_1);
        registeredUsers.clear();
        rKeys.delete(KEY);
    }

    void shutdown() {
        redisson.shutdown();
    }

    void logRegistration(int user_id)
    {
        registeredUsers.add(getTs(), String.join("-","user",String.valueOf(user_id)));
    }

    int calculateRegisteredUsersNumber()
    {
        return registeredUsers.count(Double.NEGATIVE_INFINITY, true, Double.POSITIVE_INFINITY, true);
    }

    RScoredSortedSet<String> getRegisteredUsers(){
        return registeredUsers;
    }



    // Пример вывода всех ключей - старая логика
    public void listKeys() {
        Iterable<String> keys = rKeys.getKeys();
        for(String key: keys) {
            out.println("KEY: " + key + ", type:" + rKeys.getType(key));
        }
    }

    // Фиксирует посещение пользователем страницы - старая логика
    void logPageVisit(int user_id)
    {
        //ZADD ONLINE_USERS
        onlineUsers.add(getTs(), String.valueOf(user_id));
    }

    // Удаляет - старая логика
    void deleteOldEntries(int secondsAgo)
    {
        //ZREVRANGEBYSCORE ONLINE_USERS 0 <time_5_seconds_ago>
        onlineUsers.removeRangeByScore(0, true, getTs() - secondsAgo, true);
    }

    // Считает - старая логика
    int calculateUsersNumber()
    {
        //ZCOUNT ONLINE_USERS
        return onlineUsers.count(Double.NEGATIVE_INFINITY, true, Double.POSITIVE_INFINITY, true);
    }

}
