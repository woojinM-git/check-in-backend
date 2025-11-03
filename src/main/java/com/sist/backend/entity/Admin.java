package com.sist.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "admin")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Admin {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "adminIdx")
    private Integer adminIdx;
    
    @Column(name = "type")
    private Boolean type;
    
    @Column(name = "id")
    private String id;
    
    @Column(name = "pw")
    private String pw;
    
    @Column(name = "status")
    private Boolean status;
    
    @Column(name = "name")
    private String name;

    @Column(name = "refToken")
    private String refToken;

    @Column(name = "refTokenUpdatedAt")
    private LocalDateTime refTokenUpdatedAt;

    @Column(name = "phone")
    private String phone;
    
    // 양방향 관계
    @OneToMany(mappedBy = "admin", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<Center> centers = new ArrayList<>();

    @OneToMany(mappedBy = "admin", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<HotelInfo> hotelInfos = new ArrayList<>();

    @OneToMany(mappedBy = "admin", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<RegistrationRequest> registrationRequests = new ArrayList<>();
}
