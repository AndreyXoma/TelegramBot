package com.example.tgBot.service;


import com.example.tgBot.config.BotConfig;
import com.example.tgBot.entity.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.json.JsonParseException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class TelegramBot  extends TelegramLongPollingBot {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private DailyDomainsRepository dailyDomainsRepository;
    private BotConfig botConfig;
    private Users users = new Users();

    private final SendMessage sendMessage = new SendMessage();
    private List<DailyDomains> list = new ArrayList<>();

    public TelegramBot(BotConfig botConfig) {
        this.botConfig = botConfig;
    }

    @Scheduled(cron = "*/10  * * * *" )
    public void json() {
        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            String json = restTemplate.getForObject("https://backorder.ru/json/?order=desc&expired=1&by=hotness&page=1&items=50",
                    String.class);

            list = objectMapper.readValue(json, new TypeReference<>() {});

            Thread saveThread = new Thread(() -> {
                dailyDomainsRepository.saveAll(list);
                log.info("Сохранение выполнено успешно!");
            });

            saveThread.start();

        } catch (JsonParseException e) {
          log.error("Неккоректный файл", e);

        } catch (IOException e) {
           log.error("Ошибка чтения файла", e);
        }
    }

    private void sendingMessage() {
        var users = userRepository.findAll();

        String text = "Было собрано: " + list.size() + " доменов!";
        for(Users user : users) {
            sendMessage(user.getId(),  text);
        }

    }

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

            switch (message) {
                case "/start":
                    users = registerUser(update.getMessage());
                    startCommand(chatId, update.getMessage().getChat().getFirstName());
                    break;
                default:
                    updateLastMessage(update.getMessage(), users);

                    sendMessage(chatId, "Данная команда неподдерживается!!!");
                    registOfIncomingAndOutgoingMess(users, update.getMessage(), update);


            }

        }
        json();
        sendingMessage();
    }

    public void updateLastMessage(Message message, Users users) {
        if (userRepository.findById(message.getChatId()).isPresent()) {

            users.insertLastMess(message);
            userRepository.save(users);

           log.info("Новое сообщение пользователя: " + users.getName() +
                   " сохранено: " + users.getLast_message_at());
        } else {
            sendMessage(message.getChatId(), "Для начала работы нажми на: /start ");
        }
    }


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

    private void startCommand(long chatId, String name) {
        String answer = "Привет, " + name;

       log.info("пользователь: " + name);
        sendMessage(chatId, answer);

    }

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
