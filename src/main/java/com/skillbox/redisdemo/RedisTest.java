package com.skillbox.redisdemo;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.Scanner;

import static java.lang.System.out;

public class RedisTest {

    // Для теста будем считать неактивными пользователей, которые не заходили 2 секунды
    private static final int DELETE_SECONDS_AGO = 2;

    // Допустим пользователи делают 500 запросов к сайту в секунду
    private static final int RPS = 5;

    // И всего на сайт заходило 1000 различных пользователей
    private static final int USERS = 1000;

    // Также мы добавим задержку между посещениями
    private static final int SLEEP = 50; // 50 миллисекунд

    private static final SimpleDateFormat DF = new SimpleDateFormat("HH:mm:ss");
    private static final int MAX_REGS_COUNT = 20;
    private static volatile boolean running = true;
    private static final int PURCHASE_FREQ = 10;

    public static void main(String[] args) throws InterruptedException {
        RedisStorage redis = new RedisStorage();
        redis.init();
        emulateUsersReg(redis);
        exitProvider();

        Random random = new Random();
        int registeredUsersCount = redis.getRegisteredUsers().count(Double.NEGATIVE_INFINITY, true, Double.POSITIVE_INFINITY, true);
        int whenBuy = 0;
        int whoBuy = 0;

        while (running) {
            for (int i = 0; i < registeredUsersCount; i++){
                if (!running) break;
                if (i % PURCHASE_FREQ == 0){
                    whenBuy = i + random.nextInt(PURCHASE_FREQ);
                    whoBuy = random.nextInt(registeredUsersCount);
                }
                if (i == whenBuy){
                    purchase(redis, whenBuy, whoBuy);
                }
                String current = redis.getRegisteredUsers().valueRange(i,i).iterator().next();
                out.printf("Показываем пользователя %S\n", current);
                pause(500);
            }
        }
        System.out.println("Остановлено!");
        redis.shutdown();
        System.exit(0);
    }

    public static void pause(int millis){
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            out.println("Поток прерван");
            Thread.currentThread().interrupt();
        }
    }

    public static void purchase (RedisStorage redis, int whenBuy, int whoBuy){
        String buyer = redis.getRegisteredUsers().valueRange(whoBuy,whoBuy).iterator().next();
        String waiter = redis.getRegisteredUsers().valueRange(whenBuy,whenBuy).iterator().next();
        double waiterScore = redis.getRegisteredUsers().getScore(waiter);
        redis.getRegisteredUsers().add(waiterScore - 1, buyer);
        out.printf(">Пользователь %s оплатил внеочередной показ\n", buyer);
        if (whoBuy < whenBuy){
            out.printf("Показываем пользователя %S\n", buyer);
        }
    }

    public static void exitProvider(){
        out.println("Для выходя из программы нажмите Enter");
        Thread inputThread = new Thread(() -> {
            try (Scanner sc = new Scanner(System.in)) {
                sc.nextLine();
                running = false;
            } catch (Exception ignored) {}
        });
        inputThread.start();
    }

    public static void emulateUsersReg(RedisStorage redis){
        for (int i = 0; i < MAX_REGS_COUNT; ++i){
            redis.logRegistration(i + 1);
            try{
                Thread.sleep(SLEEP);
            } catch (InterruptedException e) {
                out.println("Что-то пошло не так " + e.getMessage());
                Thread.currentThread().interrupt();
            }
        }
        if (redis.calculateRegisteredUsersNumber() == 20){
            out.printf("Создана база данных в количестве %d пользователей\n", redis.calculateRegisteredUsersNumber());
        }else out.printf("Что-то пошло не так, создана база из %d пользователей\n", redis.calculateRegisteredUsersNumber());
    }

    //предыдущую логику сохраняю
    private static void onlineUsersCount(){
        RedisStorage redis = new RedisStorage();
        redis.init();
        Random random = new Random();
        // Эмулируем 10 секунд работы сайта
        for(int seconds=0; seconds <= 10; seconds++) {
            // Выполним 500 запросов
            for(int request = 0; request <= RPS; request++) {
                int user_id = random.nextInt(USERS);
                redis.logPageVisit(user_id);
                try{
                    Thread.sleep(SLEEP);
                } catch (InterruptedException e) {
                    out.println("Что-то пошло не так " + e.getMessage());
                    Thread.currentThread().interrupt();
                }
            }
            redis.deleteOldEntries(DELETE_SECONDS_AGO);
            int usersOnline = redis.calculateUsersNumber();
            log(usersOnline);
        }
        redis.shutdown();
    }

    //предыдущую логику сохраняю
    private static void log(int UsersOnline) {
        String log = String.format("[%s] Пользователей онлайн: %d", DF.format(new Date()), UsersOnline);
        out.println(log);
    }
}
