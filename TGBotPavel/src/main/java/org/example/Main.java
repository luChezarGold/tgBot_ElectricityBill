package org.example;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class Main {
    public static void main(String[] args) {
        try {
            // Использование DefaultBotSession для long polling
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            // Регистрация бота
            telegramBotsApi.registerBot(new ElectricBillBot());
        } catch (TelegramApiException e) {
            // Обработка возможных ошибок
            e.printStackTrace();
        }
    }
}
