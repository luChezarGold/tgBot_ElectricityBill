package org.example;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ElectricBillBot extends TelegramLongPollingBot {




////
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/tgbot";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "kotic";
    @Override
    public String getBotUsername() {
        return "electric_bill_bot";
    }

    @Override
    public String getBotToken() {
        return "8135770117:AAFdLotN0YCTb0LUU76EKLlCJO795_YkzTA";
    }
////





    private Map<Long, UserState> userStates = new HashMap<>();
    private Map<Long, Map<String, String>> registrationData = new HashMap<>();

    private enum UserState {
        NOT_LOGGED_IN,
        CHOOSING_PAYMENT_METHOD,
        REGISTERING_CARD_NUMBER,
        TOPPING_UP_ACCOUNT,
        REGISTERING_CARD_CVV,
        REGISTERING_CARD_EXPIRATION,
        REGISTERING_CARDHOLDER_NAME,
        CONFIRMING_PAYMENT,
        SCHEDULING_BANK_VISIT_DATE,
        SCHEDULING_BANK_VISIT_TIME,
        LOGGED_IN,
        REGISTERING_LOGIN,
        UPDATING_METER_READING,
        REGISTERING_PASSWORD,
        REGISTERING_NAME,
        REGISTERING_ADDRESS,
        REGISTERING_RAION,
        REGISTERING_LS,
        REGISTERING_EMAIL,
        REGISTERING_PHONE,
        LOGIN_LOGIN,  // Новое состояние для логина
        LOGIN_PASSWORD // Новое состояние для пароля
    }



    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            UserState state = userStates.getOrDefault(chatId, UserState.NOT_LOGGED_IN);

            // Обрабатываем нажатие на кнопку "ОТКАТ"
            if ("ОТКАТ".equals(messageText)) {
                // Возвращаем на главное меню
                sendMessage(chatId, "Вы вернулись в главное меню.");
                sendLoggedInMenu(chatId);
                userStates.put(chatId, UserState.LOGGED_IN);
                return;
            }

            switch (state) {
                case NOT_LOGGED_IN:
                    handleNotLoggedInState(chatId, messageText);
                    break;
                case LOGGED_IN:
                    handleLoggedInState(chatId, messageText);
                    break;

                case TOPPING_UP_ACCOUNT:
                    handleTopUpAmount(chatId, messageText);
                    break;
                case REGISTERING_LOGIN:
                    handleRegistrationState(chatId, messageText, state);
                    break;
                case REGISTERING_PASSWORD:
                    handleRegistrationState(chatId, messageText, state);
                    break;
                case REGISTERING_EMAIL:
                    handleRegistrationState(chatId, messageText, state);
                    break;
                case REGISTERING_NAME:
                    handleRegistrationState(chatId, messageText, state);
                    break;
                case REGISTERING_LS:
                    handleRegistrationState(chatId, messageText, state);
                    break;
                case REGISTERING_RAION:
                    handleRegistrationState(chatId, messageText, state);
                    break;
                case REGISTERING_ADDRESS:
                    handleRegistrationState(chatId, messageText, state);
                    break;
                case REGISTERING_PHONE:
                    handleRegistrationState(chatId, messageText, state);
                    break;
                case UPDATING_METER_READING:
                    handleMeterReadingUpdate(chatId, messageText);
                    break;
                case LOGIN_LOGIN:
                case LOGIN_PASSWORD:
                    handleLoginState(chatId, messageText, state); // Обрабатываем логин
                    break;
                case REGISTERING_CARD_NUMBER:
                    handleCardNumberRegistration(chatId, messageText);
                    break;
                case REGISTERING_CARD_CVV:
                    handleCardCVVRegistration(chatId, messageText);
                    break;
                case REGISTERING_CARD_EXPIRATION:
                    handleCardExpirationRegistration(chatId, messageText);
                    break;
                case REGISTERING_CARDHOLDER_NAME:
                    handleCardholderNameRegistration(chatId, messageText);
                    break;
                case CONFIRMING_PAYMENT:
                    handlePaymentConfirmation(chatId, messageText);
                    break;
                case CHOOSING_PAYMENT_METHOD:
                    handlePaymentMethodChoice(chatId, messageText);
                    break;
                case SCHEDULING_BANK_VISIT_DATE:
                    handleBankVisitDateInput(chatId, messageText);
                    break;
                case SCHEDULING_BANK_VISIT_TIME:
                    handleBankVisitTimeInput(chatId, messageText);
                    break;
            }
        }
    }


    private void handleMeterReadingUpdate(long chatId, String messageText) {
        try {
            BigDecimal newReading = new BigDecimal(messageText);
            if (newReading.compareTo(BigDecimal.ZERO) < 0) {
                throw new NumberFormatException();
            }
            updateMeterReadingInDatabase(chatId, newReading);
        } catch (NumberFormatException e) {
            sendMessage(chatId, "Пожалуйста, введите корректное положительное число для показаний счетчика.");
        }
    }

    private void handleCardNumberRegistration(long chatId, String cardNumber) {
        if (cardNumber.matches("\\d{16}")) {
            registrationData.computeIfAbsent(chatId, k -> new HashMap<>()).put("cardNumber", cardNumber);
            sendMessage(chatId, "Введите CVV код (3 или 4 цифры):");
            userStates.put(chatId, UserState.REGISTERING_CARD_CVV);
        } else {
            sendMessage(chatId, "Неверный формат номера карты. Пожалуйста, введите 16 цифр.");
        }
    }

    private void handleCardCVVRegistration(long chatId, String cvv) {
        if (cvv.matches("\\d{3,4}")) {
            registrationData.get(chatId).put("cvv", cvv);
            sendMessage(chatId, "Введите срок действия карты в формате MM/YY:");
            userStates.put(chatId, UserState.REGISTERING_CARD_EXPIRATION);
        } else {
            sendMessage(chatId, "Неверный формат CVV. Пожалуйста, введите 3 или 4 цифры.");
        }
    }

    private void handleCardExpirationRegistration(long chatId, String expiration) {
        if (expiration.matches("\\d{2}/\\d{2}")) {
            registrationData.get(chatId).put("expiration", expiration);
            sendMessage(chatId, "Введите имя владельца карты:");
            userStates.put(chatId, UserState.REGISTERING_CARDHOLDER_NAME);
        } else {
            sendMessage(chatId, "Неверный формат срока действия. Пожалуйста, используйте формат MM/YY.");
        }
    }

    private void handleCardholderNameRegistration(long chatId, String cardholderName) {
        registrationData.get(chatId).put("cardholderName", cardholderName);
        saveCardToDatabase(chatId);
    }


    private void saveCardToDatabase(long chatId) {
        String login = loggedInUsers.get(chatId);
        Map<String, String> cardData = registrationData.get(chatId);

        String sql = "INSERT INTO karta (card_number, user_id, balance, cvv_code, cardholder_name, expiration_date) " +
                "VALUES (?, (SELECT id FROM polzovatel WHERE login = ?), 0, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, cardData.get("cardNumber"));
            pstmt.setString(2, login);
            pstmt.setString(3, cardData.get("cvv"));
            pstmt.setString(4, cardData.get("cardholderName"));

            String expirationDate;
            boolean validDate = false;

            while (!validDate) {
                // Преобразование MM/YY в дату
                String[] expirationParts = cardData.get("expiration").split("/");
                expirationDate = "20" + expirationParts[1] + "-" + expirationParts[0] + "-01";

                try {
                    pstmt.setDate(5, Date.valueOf(expirationDate));
                    validDate = true; // Если прошло без исключений, дата валидна
                } catch (IllegalArgumentException e) {
                    System.out.println("Ошибка преобразования даты: " + expirationDate);
                    sendMessage(chatId, "Неверный формат даты окончания. Переход в меню");

                    // Обновляем состояние пользователя для ожидания нового ввода даты
                    userStates.put(chatId, UserState.LOGGED_IN);

                     // Завершаем выполнение метода
                }
            }

            pstmt.executeUpdate();
            sendMessage(chatId, "Карта успешно добавлена. Теперь вы можете пополнить баланс и оплатить задолженность.");
            sendLoggedInMenu(chatId);
        } catch (SQLException e) {
            sendMessage(chatId, "Произошла ошибка при сохранении карты. Пожалуйста, попробуйте позже.");
            e.printStackTrace();
        } finally {
            userStates.put(chatId, UserState.LOGGED_IN);
            registrationData.remove(chatId);
        }
    }




    private void updateMeterReadingInDatabase(long chatId, BigDecimal newReading) {
        String login = loggedInUsers.get(chatId);
        String sql = "UPDATE lichny_kabinet SET consumed_kwh = ? " +
                "WHERE user_id = (SELECT id FROM polzovatel WHERE login = ?)";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setBigDecimal(1, newReading);
            pstmt.setString(2, login);

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                sendMessage(chatId, "Показания счетчика успешно обновлены.");
            } else {
                sendMessage(chatId, "Не удалось обновить показания счетчика. Пожалуйста, попробуйте еще раз.");
            }

        } catch (SQLException e) {
            sendMessage(chatId, "Произошла ошибка при обновлении показаний счетчика: " + e.getMessage());
            e.printStackTrace();
        } finally {
            userStates.put(chatId, UserState.LOGGED_IN);
        }
    }

    private void handleLoggedInState(long chatId, String messageText) {
        switch (messageText) {
            case "Личный кабинет":
                showPersonalAccount(chatId);
                break;
            case "Добавить карту":
                addBankCard(chatId);
                break;
            case "Пополнить счет":
                topUpAccount(chatId);
                break;
            case "ПОСМОТРЕТЬ МОИ ЗАПИСИ В БАНК":
                viewBankAppointments(chatId);
                break;
            case "Рассчитать задолженность":
                calculateDebt(chatId);
                break;
            case "Записаться в банк":
                scheduleBankVisit(chatId);
                break;
            case "ОПЛАТИТЬ":
                choosePaymentMethod(chatId);
                break;
            case "Обновить показания счетчика":
                updateMeterReadings(chatId);
                break;
            case "Выйти":
                logout(chatId);
                break;
            default:
                sendLoggedInMenu(chatId);
        }
    }

    private void handleRegistrationState(long chatId, String messageText, UserState state) {
        Map<String, String> userData = registrationData.getOrDefault(chatId, new HashMap<>());

        // Debug: Show current state and message text
        System.out.println("DEBUG: Current state: " + state + ", Received message: " + messageText);

        switch (state) {
            case REGISTERING_LOGIN:
                userData.put("login", messageText);
                userStates.put(chatId, UserState.REGISTERING_PASSWORD);
                sendMessage(chatId, "Введите пароль:");
                break;
            case REGISTERING_PASSWORD:
                userData.put("password", messageText);
                userStates.put(chatId, UserState.REGISTERING_NAME);
                sendMessage(chatId, "Введите ваше имя:");
                break;
            case REGISTERING_NAME:
                userData.put("name", messageText);
                userStates.put(chatId, UserState.REGISTERING_ADDRESS);
                sendMessage(chatId, "Введите ваш адрес:");
                break;
            case REGISTERING_ADDRESS:
                userData.put("address", messageText);
                userStates.put(chatId, UserState.REGISTERING_RAION);
                sendRaionSelectionButtons(chatId); // Отправляем кнопки для выбора района
                break;
            case REGISTERING_RAION:
                if (isValidRaion(messageText)) { // Проверяем, что ID района корректный
                    userData.put("raion_id", messageText);
                    userStates.put(chatId, UserState.REGISTERING_LS);
                    sendMessage(chatId, "Введите ваш лицевой счет:");
                } else {
                    sendMessage(chatId, "Неверный ID района. Пожалуйста, выберите район из кнопок.");
                    sendRaionSelectionButtons(chatId);
                }
                break;
            case REGISTERING_LS:
                userData.put("l_s", messageText);
                userStates.put(chatId, UserState.REGISTERING_EMAIL);
                sendMessage(chatId, "Введите ваш email:");
                break;
            case REGISTERING_EMAIL:
                userData.put("email", messageText);
                userStates.put(chatId, UserState.REGISTERING_PHONE);
                sendMessage(chatId, "Введите ваш номер телефона:");
                break;
            case REGISTERING_PHONE:
                userData.put("phone_number", messageText);
                System.out.println("DEBUG: Registration data: " + userData);
                registerUser(chatId, userData);
                break;
        }

        registrationData.put(chatId, userData);
    }

    private void sendRaionSelectionButtons(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Выберите ваш район:");

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement("SELECT id FROM raion")) {
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                KeyboardRow row = new KeyboardRow();
                row.add(rs.getString("id")); // Добавляем ID и название района
                keyboard.add(row);
            }

        } catch (SQLException e) {
            sendMessage(chatId, "Ошибка получения списка районов: " + e.getMessage());
            System.out.println("DEBUG: SQL Exception in sendRaionSelectionButtons: " + e.getMessage());
        }

        keyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private Map<Long, String> loggedInUsers = new HashMap<>();

    private boolean isValidRaion(String messageText) {
        // Проверяем, что район существует в базе данных
        String raionId = messageText.split(":")[0].trim();
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement("SELECT id FROM raion WHERE id = ?")) {
            stmt.setInt(1, Integer.parseInt(raionId));
            ResultSet rs = stmt.executeQuery();
            return rs.next(); // Возвращает true, если район найден
        } catch (SQLException e) {
            System.out.println("DEBUG: SQL Exception in isValidRaion: " + e.getMessage());
            return false;
        }
    }

    private void registerUser(long chatId, Map<String, String> userData) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet generatedKeys = null;

        try {
            // Open a connection
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

            // Disable auto-commit to handle transactions manually
            conn.setAutoCommit(false);

            // Insert into the "polzovatel" table
            String insertPolzovatelSQL = "INSERT INTO polzovatel (login, password, name, address, raion_id, l_s, email, phone_number) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            pstmt = conn.prepareStatement(insertPolzovatelSQL, Statement.RETURN_GENERATED_KEYS);

            // Set the values from userData
            pstmt.setString(1, userData.get("login"));
            pstmt.setString(2, userData.get("password"));  // Note: Hash the password in a real application
            pstmt.setString(3, userData.get("name"));
            pstmt.setString(4, userData.get("address"));
            pstmt.setInt(5, Integer.parseInt(userData.get("raion_id")));
            pstmt.setString(6, userData.get("l_s"));
            pstmt.setString(7, userData.get("email"));
            pstmt.setString(8, userData.get("phone_number"));

            // Execute the insert statement
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Ошибка: создание пользователя не завершилось.");
            }

            // Retrieve the generated user_id
            generatedKeys = pstmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                int userId = generatedKeys.getInt(1);

                // Insert into the "lichny_kabinet" table using the generated user_id
                String insertLichnyKabinetSQL = "INSERT INTO lichny_kabinet (user_id, name, address, consumed_kwh) VALUES (?, ?, ?, 0)";
                pstmt = conn.prepareStatement(insertLichnyKabinetSQL);
                pstmt.setInt(1, userId);
                pstmt.setString(2, userData.get("name"));
                pstmt.setString(3, userData.get("address"));

                // Execute the insert into "lichny_kabinet"
                pstmt.executeUpdate();
            } else {
                throw new SQLException("Ошибка: не удалось получить ID пользователя.");
            }

            // Commit the transaction
            conn.commit();

            // Success message
            sendMessage(chatId, "Регистрация успешно завершена!");
            loggedInUsers.put(chatId, userData.get("login")); // Сохраняем логин пользователя
            userStates.put(chatId, UserState.LOGGED_IN);
            sendLoggedInMenu(chatId);


        } catch (SQLException e) {
            // Rollback transaction on error
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    System.out.println("Ошибка отката: " + rollbackEx.getMessage());
                }
            }

            sendMessage(chatId, "Произошла ошибка при регистрации: " + e.getMessage());
            System.out.println("DEBUG: SQL Exception: " + e.getMessage());
            userStates.put(chatId, UserState.NOT_LOGGED_IN);
            sendNotLoggedInMenu(chatId);

        } finally {
            // Clean up resources
            try {
                if (generatedKeys != null) generatedKeys.close();
                if (pstmt != null) pstmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                System.out.println("Ошибка закрытия ресурсов: " + e.getMessage());
            }

            // Remove temporary registration data
            registrationData.remove(chatId);
        }
    }


    private void startRegistration(long chatId) {
        userStates.put(chatId, UserState.REGISTERING_LOGIN);
        registrationData.put(chatId, new HashMap<>());
        sendMessage(chatId, "Начинаем регистрацию. Введите логин:");
    }


    // Обновленный метод login
    private void login(long chatId, String login, String password) {
        String sql = "SELECT * FROM polzovatel WHERE login = ? AND password = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, login);
            pstmt.setString(2, password); // В реальном приложении здесь должна быть проверка хешированного пароля

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                userStates.put(chatId, UserState.LOGGED_IN);
                loggedInUsers.put(chatId, login); // Сохраняем логин пользователя
                sendMessage(chatId, "Вы успешно вошли в систему.");
                sendLoggedInMenu(chatId);
            } else {
                sendMessage(chatId, "Неверный логин или пароль.");
                sendNotLoggedInMenu(chatId);
            }

        } catch (SQLException e) {
            sendMessage(chatId, "Ошибка при входе: " + e.getMessage());
            e.printStackTrace();
            sendNotLoggedInMenu(chatId);
        }
    }

    private void handleNotLoggedInState(long chatId, String messageText) {
        switch (messageText) {
            case "Регистрация":
                startRegistration(chatId);
                break;
            case "Вход":
                startLogin(chatId); // Начинаем процесс входа
                break;
            default:
                sendNotLoggedInMenu(chatId);
        }
    }


    private void startLogin(long chatId) {
        userStates.put(chatId, UserState.LOGIN_LOGIN); // Устанавливаем состояние ожидания логина
        sendMessage(chatId, "Введите ваш логин:");
    }


    private void handleLoginState(long chatId, String messageText, UserState state) {
        Map<String, String> loginData = registrationData.getOrDefault(chatId, new HashMap<>());

        switch (state) {
            case LOGIN_LOGIN:
                loginData.put("login", messageText);
                userStates.put(chatId, UserState.LOGIN_PASSWORD); // Переходим к запросу пароля
                sendMessage(chatId, "Введите ваш пароль:");
                break;
            case LOGIN_PASSWORD:
                loginData.put("password", messageText);
                checkLogin(chatId, loginData.get("login"), loginData.get("password")); // Проверяем логин и пароль
                break;
        }

        registrationData.put(chatId, loginData); // Сохраняем введенные данные
    }


    private void checkLogin(long chatId, String login, String password) {
        String sql = "SELECT * FROM polzovatel WHERE login = ? AND password = ?"; // В реальном приложении пароли должны быть хэшированы

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, login);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                sendMessage(chatId, "Вы успешно вошли в систему.");
                userStates.put(chatId, UserState.LOGGED_IN);
                loggedInUsers.put(chatId, login); // Сохраняем логин пользователя
                sendLoggedInMenu(chatId);
            } else {
                sendMessage(chatId, "Неверный логин или пароль. Попробуйте снова.");
                userStates.put(chatId, UserState.NOT_LOGGED_IN);
                sendNotLoggedInMenu(chatId);
            }

        } catch (SQLException e) {
            sendMessage(chatId, "Ошибка при входе в систему: " + e.getMessage());
            System.out.println("DEBUG: SQL Exception in checkLogin: " + e.getMessage());
            userStates.put(chatId, UserState.NOT_LOGGED_IN);
            sendNotLoggedInMenu(chatId);
        }
    }


    // Обновленный метод logout
    private void logout(long chatId) {
        userStates.put(chatId, UserState.NOT_LOGGED_IN);
        loggedInUsers.remove(chatId); // Удаляем информацию о залогиненном пользователе
        sendMessage(chatId, "Вы вышли из системы.");
        sendNotLoggedInMenu(chatId);
    }

// ... (остальной код класса остается без изменений)

    private void showPersonalAccount(long chatId) {
        String login = loggedInUsers.get(chatId);
        if (login == null) {
            sendMessage(chatId, "Вы не вошли в систему. Пожалуйста, войдите сначала.");
            return;
        }

        String sql = "SELECT p.l_s, p.address, lk.consumed_kwh " +
                "FROM polzovatel p " +
                "LEFT JOIN lichny_kabinet lk ON p.id = lk.user_id " +
                "WHERE p.login = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, login);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String accountInfo = "Ваш лицевой счет: " + rs.getString("l_s") +
                        "\nАдрес: " + rs.getString("address") +
                        "\nПотребленные кВт·ч: " + (rs.getBigDecimal("consumed_kwh") != null ? rs.getBigDecimal("consumed_kwh").toString() : "Нет данных");
                sendMessage(chatId, accountInfo);
            } else {
                sendMessage(chatId, "Личный кабинет не найден.");
            }

        } catch (SQLException e) {
            sendMessage(chatId, "Ошибка при получении информации о личном кабинете: " + e.getMessage());
            e.printStackTrace();
        }
    }

// ... (остальной код класса остается без изменений)

    private void addBankCard(long chatId) {
        if (!hasCard(chatId)) {
            startCardRegistration(chatId);
        }
        else sendMessage(chatId, "У вас уже есть карта");

    }

    private void topUpAccount(long chatId) {
        String login = loggedInUsers.get(chatId);
        if (login == null) {
            sendMessage(chatId, "Вы не вошли в систему. Пожалуйста, войдите сначала.");
            return;
        }

        if (!hasCard(chatId)) {
            sendMessage(chatId, "У вас нет зарегистрированной карты. Пожалуйста, добавьте карту сначала.");
            return;
        }

        sendMessage(chatId, "Введите сумму для пополнения счета:");
        userStates.put(chatId, UserState.TOPPING_UP_ACCOUNT);
    }

    private void handleTopUpAmount(long chatId, String messageText) {
        try {
            BigDecimal amount = new BigDecimal(messageText);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new NumberFormatException();
            }
            processTopUp(chatId, amount);
        } catch (NumberFormatException e) {
            sendMessage(chatId, "Пожалуйста, введите корректную положительную сумму.");
            userStates.put(chatId, UserState.NOT_LOGGED_IN);
        }
    }

    private void processTopUp(long chatId, BigDecimal amount) {
        String login = loggedInUsers.get(chatId);
        String updateBalanceSql = "UPDATE karta SET balance = balance + ? WHERE user_id = (SELECT id FROM polzovatel WHERE login = ?)";


        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            conn.setAutoCommit(false);
            try (PreparedStatement updateBalanceStmt = conn.prepareStatement(updateBalanceSql)) {

                updateBalanceStmt.setBigDecimal(1, amount);
                updateBalanceStmt.setString(2, login);
                updateBalanceStmt.executeUpdate();



                conn.commit();
                sendMessage(chatId, String.format("Счет успешно пополнен на %.2f", amount));
            } catch (SQLException e) {
                conn.rollback();
                sendMessage(chatId, "Произошла ошибка при пополнении счета. Пожалуйста, попробуйте позже.");
                e.printStackTrace(); // Consider logging to a file or monitoring system
            }
        } catch (SQLException e) {
            sendMessage(chatId, "Произошла ошибка при подключении к базе данных.");
            e.printStackTrace();
        }

        userStates.put(chatId, UserState.LOGGED_IN);
        sendLoggedInMenu(chatId);
    }


    private void calculateDebt(long chatId) {
        String login = loggedInUsers.get(chatId);
        if (login == null) {
            sendMessage(chatId, "Вы не вошли в систему. Пожалуйста, войдите сначала.");
            return;
        }

        String sql = "SELECT r.price_per_kwh, lk.consumed_kwh " +
                "FROM polzovatel p " +
                "JOIN raion r ON p.raion_id = r.id " +
                "JOIN lichny_kabinet lk ON p.id = lk.user_id " +
                "WHERE p.login = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, login);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                BigDecimal pricePerKwh = rs.getBigDecimal("price_per_kwh");
                BigDecimal consumedKwh = rs.getBigDecimal("consumed_kwh");

                if (pricePerKwh != null && consumedKwh != null) {
                    BigDecimal debt = pricePerKwh.multiply(consumedKwh).setScale(2, RoundingMode.HALF_UP);

                    String message = String.format(
                            "Расчет задолженности:\n" +
                                    "Цена за кВт·ч в вашем районе: %.2f\n" +
                                    "Потреблено кВт·ч: %.2f\n" +
                                    "Общая задолженность: %.2f",
                            pricePerKwh, consumedKwh, debt);

                    sendMessage(chatId, message);
                } else {
                    sendMessage(chatId, "Не удалось рассчитать задолженность. Отсутствуют необходимые данные.");
                }
            } else {
                sendMessage(chatId, "Не удалось найти информацию для расчета задолженности.");
            }

        } catch (SQLException e) {
            sendMessage(chatId, "Произошла ошибка при расчете задолженности: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private void choosePaymentMethod(long chatId) {
        String login = loggedInUsers.get(chatId);
        if (login == null) {
            sendMessage(chatId, "Вы не вошли в систему. Пожалуйста, войдите сначала.");
            return;
        }

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Выберите способ оплаты:");

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("Запланировать визит в банк");
        row1.add("Оплатить картой");

        keyboard.add(row1);

        keyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
            userStates.put(chatId, UserState.CHOOSING_PAYMENT_METHOD);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handlePaymentMethodChoice(long chatId, String messageText) {
        switch (messageText) {
            case "Запланировать визит в банк":
                scheduleBankVisit(chatId);
                break;
            case "Оплатить картой":
                payByCard(chatId);
                break;
            default:
                sendMessage(chatId, "Пожалуйста, выберите один из предложенных вариантов.");
        }
    }

    private void sendMessageWithKeyboard(long chatId, String text, ReplyKeyboardMarkup keyboardMarkup) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setReplyMarkup(keyboardMarkup);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void scheduleBankVisit(long chatId) {
        // Сообщение с просьбой ввести дату
        sendMessage(chatId, "Пожалуйста, введите дату визита в формате DD.MM.YY:");

        // Добавляем кнопку "ОТКАТ" для возврата на главное меню
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("ОТКАТ");  // Добавляем кнопку "ОТКАТ"
        keyboard.add(row);
        replyKeyboardMarkup.setKeyboard(keyboard);

        // Отправляем сообщение с клавиатурой
        sendMessageWithKeyboard(chatId, "Введите дату или нажмите 'ОТКАТ', чтобы вернуться в главное меню.", replyKeyboardMarkup);

        // Переводим пользователя в состояние ожидания даты
        userStates.put(chatId, UserState.SCHEDULING_BANK_VISIT_DATE);
    }


    private void handleBankVisitDateInput(long chatId, String messageText) {
        // Validate date format
        if (!messageText.matches("\\d{2}\\.\\d{2}\\.\\d{2}")) {
            sendMessage(chatId, "Неверный формат даты. Пожалуйста, используйте формат DD.MM.YY.");
            return;
        }

        // Store the date temporarily
        registrationData.computeIfAbsent(chatId, k -> new HashMap<>()).put("visitDate", messageText);

        sendMessage(chatId, "Теперь введите время визита в формате HH:MM:");
        userStates.put(chatId, UserState.SCHEDULING_BANK_VISIT_TIME);
    }

    private void handleBankVisitTimeInput(long chatId, String messageText) {
        // Validate time format
        if (!messageText.matches("\\d{2}:\\d{2}")) {
            sendMessage(chatId, "Неверный формат времени. Пожалуйста, используйте формат HH:MM.");
            return;
        }

        String visitDate = registrationData.get(chatId).get("visitDate");
        String visitTime = messageText;

        // Формируем полное время визита
        String visitDateTime = visitDate + " " + visitTime + ":00";  // Например: "21.09.24 15:30:00"

        // Получаем логин пользователя
        String login = loggedInUsers.get(chatId);

        // Calculate debt
        BigDecimal debt = calculateDebtForUser(chatId);

        if (debt == null) {
            sendMessage(chatId, "Не удалось рассчитать задолженность. Пожалуйста, попробуйте позже.");
            return;
        }

        // Add payment record with visit date and time
        addPaymentRecord(login, debt, 1, visitDateTime); // Передаем логин и время визита

        sendMessage(chatId, String.format("Визит в банк запланирован на %s в %s. Сумма к оплате: %.2f", visitDate, visitTime, debt));
        userStates.put(chatId, UserState.LOGGED_IN);
    }


    private BigDecimal calculateDebtForUser(long chatId) {
        String login = loggedInUsers.get(chatId);
        if (login == null) {
            return null;
        }

        String sql = "SELECT r.price_per_kwh, lk.consumed_kwh " +
                "FROM polzovatel p " +
                "JOIN raion r ON p.raion_id = r.id " +
                "JOIN lichny_kabinet lk ON p.id = lk.user_id " +
                "WHERE p.login = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, login);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                BigDecimal pricePerKwh = rs.getBigDecimal("price_per_kwh");
                BigDecimal consumedKwh = rs.getBigDecimal("consumed_kwh");

                if (pricePerKwh != null && consumedKwh != null) {
                    return pricePerKwh.multiply(consumedKwh).setScale(2, RoundingMode.HALF_UP);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void addPaymentRecord(String login, BigDecimal debt, int paymentMethodId, String visitDateTime) {
        // Преобразование строки в нужный формат
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm:ss");
        LocalDateTime dateTime = LocalDateTime.parse(visitDateTime, formatter);
        Timestamp timestamp = Timestamp.valueOf(dateTime);

        String sql = "INSERT INTO payment_records (user_id, payment_time, payment_method, amount) " +
                "VALUES ((SELECT id FROM polzovatel WHERE login = ?), ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, login);
            pstmt.setTimestamp(2, timestamp);  // Используйте объект Timestamp
            pstmt.setInt(3, paymentMethodId);
            pstmt.setBigDecimal(4, debt);

            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private void payByCard(long chatId) {
        String login = loggedInUsers.get(chatId);
        if (login == null) {
            sendMessage(chatId, "Вы не вошли в систему. Пожалуйста, войдите сначала.");
            return;
        }

        // Проверяем, есть ли у пользователя карта
        if (!hasCard(chatId)) {
            startCardRegistration(chatId);
            return;
        }

        // Получаем баланс карты и долг пользователя
        BigDecimal cardBalance = getCardBalance(chatId);
        BigDecimal debt = calculateDebtForUser(chatId);

        if (cardBalance == null || debt == null) {
            sendMessage(chatId, "Не удалось получить информацию о балансе карты или задолженности.");
            return;
        }

        sendMessage(chatId, String.format("Ваш текущий баланс: %.2f\nВаша задолженность: %.2f", cardBalance, debt));

        if (cardBalance.compareTo(debt) >= 0) {
            askForPaymentConfirmation(chatId, debt);
        } else {
            sendMessage(chatId, "На вашей карте недостаточно средств для оплаты задолженности.");
            sendLoggedInMenu(chatId);
            userStates.put(chatId, UserState.LOGGED_IN);
        }
    }

    private boolean hasCard(long chatId) {
        String login = loggedInUsers.get(chatId);
        String sql = "SELECT COUNT(*) FROM karta k JOIN polzovatel p ON k.user_id = p.id WHERE p.login = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, login);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void startCardRegistration(long chatId) {
        sendMessage(chatId, "У вас нет зарегистрированной карты. Давайте добавим новую карту.");
        sendMessage(chatId, "Введите номер карты (16 цифр):");
        userStates.put(chatId, UserState.REGISTERING_CARD_NUMBER);
    }

    private BigDecimal getCardBalance(long chatId) {
        String login = loggedInUsers.get(chatId);
        String sql = "SELECT k.balance FROM karta k JOIN polzovatel p ON k.user_id = p.id WHERE p.login = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, login);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getBigDecimal("balance");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void askForPaymentConfirmation(long chatId, BigDecimal amount) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Вы уверены, что хотите оплатить задолженность в размере " + amount + "?");

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("Да");
        row.add("Нет");
        keyboard.add(row);
        keyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
            userStates.put(chatId, UserState.CONFIRMING_PAYMENT);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    // Добавьте этот метод в основной класс для обработки подтверждения оплаты
    private void handlePaymentConfirmation(long chatId, String messageText) {
        if ("Да".equals(messageText)) {
            processPayment(chatId);
        } else {
            sendMessage(chatId, "Оплата отменена.");
            sendLoggedInMenu(chatId);
            userStates.put(chatId, UserState.LOGGED_IN);
        }
    }

    private void processPayment(long chatId) {
        String login = loggedInUsers.get(chatId);
        BigDecimal debt = calculateDebtForUser(chatId);

        if (debt == null) {
            sendMessage(chatId, "Не удалось получить информацию о задолженности.");
            return;
        }

        String updateBalanceSql = "UPDATE karta SET balance = balance - ? WHERE user_id = (SELECT id FROM polzovatel WHERE login = ?)";
        String updateConsumedKwhSql = "UPDATE lichny_kabinet SET consumed_kwh = 0 WHERE user_id = (SELECT id FROM polzovatel WHERE login = ?)";
        String addPaymentRecordSql = "INSERT INTO payment_records (user_id, payment_time, payment_method, amount) VALUES ((SELECT id FROM polzovatel WHERE login = ?), NOW(), (SELECT id FROM vid_platezha WHERE type_name = 'Оплата картой'), ?)";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            conn.setAutoCommit(false);
            try (PreparedStatement updateBalanceStmt = conn.prepareStatement(updateBalanceSql);
                 PreparedStatement updateConsumedKwhStmt = conn.prepareStatement(updateConsumedKwhSql);
                 PreparedStatement addPaymentRecordStmt = conn.prepareStatement(addPaymentRecordSql)) {

                updateBalanceStmt.setBigDecimal(1, debt);
                updateBalanceStmt.setString(2, login);
                updateBalanceStmt.executeUpdate();

                updateConsumedKwhStmt.setString(1, login);
                updateConsumedKwhStmt.executeUpdate();

                addPaymentRecordStmt.setString(1, login);
                addPaymentRecordStmt.setBigDecimal(2, debt);
                addPaymentRecordStmt.executeUpdate();

                conn.commit();
                sendMessage(chatId, "Оплата успешно проведена. Задолженность погашена.");
                userStates.put(chatId, UserState.LOGGED_IN);
            } catch (SQLException e) {
                conn.rollback();
                sendMessage(chatId, "Произошла ошибка при проведении оплаты. Пожалуйста, попробуйте позже.");
                userStates.remove(chatId);
                e.printStackTrace();
            }
        } catch (SQLException e) {
            sendMessage(chatId, "Произошла ошибка при подключении к базе данных.");
            e.printStackTrace();
        }

        sendLoggedInMenu(chatId);
    }

    private void updateMeterReadings(long chatId) {
        String login = loggedInUsers.get(chatId);
        if (login == null) {
            sendMessage(chatId, "Вы не вошли в систему. Пожалуйста, войдите сначала.");
            return;
        }

        userStates.put(chatId, UserState.UPDATING_METER_READING);
        sendMessage(chatId, "Пожалуйста, введите новые показания счетчика (в кВт·ч):");
    }

    private void sendNotLoggedInMenu(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Выберите действие:");

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("Регистрация");
        row1.add("Вход");

        keyboard.add(row1);

        keyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendLoggedInMenu(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Выберите действие:");

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("Личный кабинет");
        row1.add("Добавить карту");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("Пополнить счет");
        row2.add("Рассчитать задолженность");

        KeyboardRow row3 = new KeyboardRow();
        row3.add("Записаться в банк");
        row3.add("ОПЛАТИТЬ");

        KeyboardRow row4 = new KeyboardRow();
        row4.add("Обновить показания счетчика");
        row4.add("ПОСМОТРЕТЬ МОИ ЗАПИСИ В БАНК");

        KeyboardRow row5 = new KeyboardRow();
        row5.add("Выйти");

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);
        keyboard.add(row4);
        keyboard.add(row5);

        keyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void viewBankAppointments(long chatId) {
        String login = loggedInUsers.get(chatId);
        if (login == null) {
            sendMessage(chatId, "Вы не вошли в систему. Пожалуйста, войдите сначала.");
            return;
        }

        String sql = "SELECT pr.payment_time, pr.amount " +
                "FROM payment_records pr " +
                "JOIN polzovatel p ON pr.user_id = p.id " +
                "JOIN vid_platezha vp ON pr.payment_method = vp.id " +
                "WHERE p.login = ? " +  // Убираем фильтр по типу платежа и оставляем только фильтр по логину
                "ORDER BY pr.payment_time DESC";


        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, login);
            ResultSet rs = pstmt.executeQuery();

            StringBuilder message = new StringBuilder("Ваши записи в банк:\n\n");
            boolean hasAppointments = false; // Установите false по умолчанию

            while (rs.next()) {
                hasAppointments = true;  // Если мы зашли в цикл, значит записи существуют
                Timestamp paymentTime = rs.getTimestamp("payment_time");
                BigDecimal amount = rs.getBigDecimal("amount");

                message.append(String.format("Дата: %s, Сумма: %.2f\n",
                        paymentTime.toLocalDateTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
                        amount));
            }

            if (!hasAppointments) {
                message = new StringBuilder("У вас нет запланированных визитов в банк.");
            }

            sendMessage(chatId, message.toString());

        } catch (SQLException e) {
            e.printStackTrace();
            sendMessage(chatId, "Произошла ошибка при получении записей о визитах в банк.");
        }
    }

    private void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}