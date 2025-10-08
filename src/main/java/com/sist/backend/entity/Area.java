package com.sist.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "area")
public class Area {

    @Id
    @Column(name = "areaCode")
    private String areaCode;

    @Column(name = "areaName")
    private String areaName;
}
