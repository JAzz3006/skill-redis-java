package com.skillbox.redisdemo;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.Scanner;

import static java.lang.System.out;

public class RedisTest {

    // Запуск докер-контейнера:
    // docker run --rm --name skill-redis -p 127.0.0.1:6379:6379/tcp -d redis

    // Для теста будем считать неактивными пользователей, которые не заходили 2 секунды
    private static final int DELETE_SECONDS_AGO = 2;

    // Допустим пользователи делают 500 запросов к сайту в секунду
    private static final int RPS = 5;

    // И всего на сайт заходило 1000 различных пользователей
    private static final int USERS = 1000;

    // Также мы добавим задержку между посещениями
    private static final int SLEEP = 50; // 1 миллисекунда

    private static final SimpleDateFormat DF = new SimpleDateFormat("HH:mm:ss");
    private static final int MAX_REGS_COUNT = 20;
    private static volatile boolean running = true;
    private static final int PURCHASE_FREQ = 10;

    private static void log(int UsersOnline) {
        String log = String.format("[%s] Пользователей онлайн: %d", DF.format(new Date()), UsersOnline);
        out.println(log);
    }

    public static void main(String[] args) throws InterruptedException {
        RedisStorage redis = new RedisStorage();
        redis.init();
        //эмулируем регистрацию заданного количества случайных пользователей
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

        //запускаем доп. поток, прерывающий цикл while
        Thread inputThread = new Thread(() -> {
            try (Scanner sc = new Scanner(System.in)) {
                sc.nextLine();
                running = false;
            } catch (Exception ignored) {}
        });
        inputThread.start();

        //механизм случайного выбора
        Random random = new Random();


        //запуск цикла показов
        out.println("Для выходя из программы нажмите Enter");
        while (running) {
            int whoBuy = 0;
            int whenBuy = 0;

            for (int i = 0;
                 i < redis.getRegisteredUsers().count(Double.NEGATIVE_INFINITY, true, Double.POSITIVE_INFINITY, true);
                 i++){
                if (!running) break;
                if (i % PURCHASE_FREQ == 0){
                    whoBuy = i + random.nextInt(PURCHASE_FREQ);
                    whenBuy = i + random.nextInt(PURCHASE_FREQ);
                }
                if (i == whenBuy){

                    out.printf("Gjrfpsdftv ", );
                }
                String current = redis.getRegisteredUsers().valueRange(i,i).iterator().next();
                out.printf("Показываем пользователя %S\n", current);

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    out.println("Поток прерван");
                    Thread.currentThread().interrupt();
                }

            }

//            for (String s : redis.getRegisteredUsers()){
//                if (!running) break;
//                luckyOne = random.nextInt(PURCHASE_FREQ);
//                if (purchase == luckyOne){
//                    out.println("покупка!");
//                }
//                purchase++;
//
//                out.printf("Показываем пользователя %s\n", s);
//                try {
//                    Thread.sleep(500);
//                } catch (InterruptedException e) {
//                    out.println("Поток прерван");
//                    Thread.currentThread().interrupt();
//                }
//                if (purchase == PURCHASE_FREQ){
//
//                }
//            }
        }
        System.out.println("Остановлено!");
        System.exit(0);
    }

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
}
