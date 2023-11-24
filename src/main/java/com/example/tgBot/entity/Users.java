package com.example.tgBot.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.objects.Message;

@Setter
@Getter
@Slf4j
@Entity(name = "users")
public class Users {

    @Id
    private Long id;
    private String name;
    private String last_message_at;

    public void insertLastMess(Message message) {
        if (message != null) {
            this.last_message_at = message.getText();
        } else {
            log.error("Ошибка в сообщении!!");
        }
    }

    @Override
    public String toString() {
        return String.format("Пользователь %s, id = %d, последнее сообщение: %s", name, id, last_message_at);
    }
}
