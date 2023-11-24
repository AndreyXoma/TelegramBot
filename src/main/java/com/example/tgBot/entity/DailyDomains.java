package com.example.tgBot.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;

@Getter
@Setter
@ToString
@Entity(name = "daily_domains")
public class DailyDomains {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String domainname;
    private Integer hotness;
    private Long price;
    private Integer x_value;
    private Integer yandex_tic;
    private Integer links;
    private Integer visitors;
    private String registrar;
    private Integer old;
    private Date delete_date;
    private Boolean rkn;
    private Boolean judicial;
    private Boolean block;

}
