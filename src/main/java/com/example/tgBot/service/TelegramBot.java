package com.example.tgBot.service;

import com.example.tgBot.config.BotConfig;
import com.example.tgBot.entity.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.json.JsonParseException;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
@EnableScheduling
public class TelegramBot extends TelegramLongPollingBot {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private DailyDomainsRepository dailyDomainsRepository;
    private BotConfig botConfig;
    private Users users = new Users();
    private final SendMessage sendMessage = new SendMessage();
    private int countDomains = 0;

    public TelegramBot(BotConfig botConfig) {
        this.botConfig = botConfig;
    }

    @Scheduled(cron = "0 0 12 * * ?")
    public void json() {
        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            dailyDomainsRepository.deleteAll();
            String json = restTemplate.getForObject("https://backorder.ru/json/?order=desc&expired=1&by=hotness&page=1&items=50",
                    String.class);
            List<DailyDomains> list = objectMapper.readValue(json, new TypeReference<>() {
            });

            Thread saveThread = new Thread(() -> {
                dailyDomainsRepository.saveAll(list);
                log.info("Сохранение выполнено успешно!");
            });
            countDomains = list.size();

            saveThread.start();

        } catch (JsonParseException e) {
            log.error("Неккоректный файл", e);

        } catch (IOException e) {
            log.error("Ошибка чтения файла", e);
        }
        sendingMessage();
    }

    // вывод сообщения о кол-ве собранных доменов
    private void sendingMessage() {
        var users = userRepository.findAll();
        LocalDate date = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("YYYY-MM-dd");

        String text = date.format(formatter) + " Собрано " + countDomains + " доменов!";
        for (Users user : users) {
            sendMessage(user.getId(), text);
        }

    }

    // регистрация входящий и исходящих сообщений от пользователя и бота
    private void registOfIncomingAndOutgoingMess(Users users, Message message, Update update) {
        if (users != null && message != null && update != null) {

            Messages messages = new Messages();
            messages.setOutgoing_mess(sendMessage.getText());
            messages.setIncoming_mess(message.getText());
            messages.setId_user(users.getId());

            messageRepository.save(messages);

            log.info(String.format("Входящее сообщение: %s, и исходящее сообщение %s, от пользователя %s были сохранены",
                    messages.getIncoming_mess(), messages.getOutgoing_mess(), users.getName()));

        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        long chatId = update.getMessage().getChatId();
        if (update.getMessage().hasText() && update.hasMessage()) {
            String message = update.getMessage().getText();

            if (message.length() > 10000) {
                sendMessage(chatId, "Слишком длинный текст!");
            } else {
                switch (message) {
                    case "/start":
                        users = registerUser(update.getMessage());
                        startCommand(chatId, update.getMessage().getChat().getFirstName());
                        registOfIncomingAndOutgoingMess(users, update.getMessage(), update);
                        break;
                    default:
                        sendMessage(chatId, "Данная команда неподдерживается!!!");
                        updateLastMessage(chatId, update.getMessage(), update);
                }
            }
        }
    }

    // сохранение последнего сообщения от пользователя
    public void updateLastMessage(long chatId, Message message, Update update) {
        Optional<Users> optionalUsers = userRepository.findById(chatId);
        if (optionalUsers.isPresent()) {
            Users users1 = optionalUsers.get();
            users1.setLast_message_at(message.getText());
            userRepository.save(users1);

            log.info("Новое сообщение пользователя: " + users1.getName() + " сохранено: " + users1.getLast_message_at());

            registOfIncomingAndOutgoingMess(users1, message, update);
        } else {
            log.error("Пользователь с ID не найден.");
        }
    }


    // регистрация нового пользователя
    private Users registerUser(Message message) {
        if (userRepository.findById(message.getChatId()).isEmpty()) {
            var chatId = message.getChatId();
            var chat = message.getChat();

            users = new Users();
            users.setId(chatId);
            users.setName(chat.getUserName());
            users.setLast_message_at(message.getText());

            userRepository.save(users);
            log.info("Пользователь " + users + " сохранен!");
        }
        return users;
    }

    // вывод начального сообщени от бота
    private void startCommand(long chatId, String name) {
        String answer = "Привет, " + name + "\n " + "Я умею только записывать твои сообщения в БД. \n " +
                "Раз в сутки буду отправлять тебе сообщение о кол-во собраных доменов!";

        log.info("пользователь: " + name);
        sendMessage(chatId, answer);
    }

    // исходящее сообщение от бота
    private void sendMessage(long chatId, String message) {
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(message);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Ошибка: " + e.getMessage());
        }
    }

    @Override
    public String getBotToken() {
        return botConfig.getToken();
    }

    @Override
    public String getBotUsername() {
        return botConfig.getBotName();
    }
}
