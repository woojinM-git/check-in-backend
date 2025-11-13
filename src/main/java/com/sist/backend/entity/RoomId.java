package com.sist.backend.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

//아무도 사용 안하는듯
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomId implements Serializable {
    private Integer roomIdx;
    private String contentId;
}

