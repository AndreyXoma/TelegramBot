package com.example.tgBot.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity(name = "message")
public class Messages {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id_message;
    private String incoming_mess;
    private String outgoing_mess;
    private Long id_user;

}
