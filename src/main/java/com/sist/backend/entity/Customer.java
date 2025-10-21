package com.sist.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customer")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Customer {
    
    @Id
    @Column(name = "customerIdx")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer customerIdx;


    @Column(name = "id", length = 20)
    private String id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rank", referencedColumnName = "rank", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    private Rank rankEntity;
    
    @Column(name = "customrank", nullable = false, length = 50)
    private String customrank;
    
    @Column(name = "birthday")
    private LocalDate birthday;
    
    @Column(name = "nickname", length = 50)
    private String nickname;
    
    @Column(name = "name", length = 20)
    private String name;
    
    @Column(name = "gender", length = 20)
    private String gender;
    
    @Column(name = "password")
    private String password;
    
    @Column(name = "phone", length = 20)
    private String phone;
    
    @Column(name = "email")
    private String email;
    
    @Column(name = "cash")
    private Integer cash;
    
    @Column(name = "status")
    private Integer status;
    
    @Column(name = "totalPrice")
    private Integer totalPrice;
    
    @Column(name = "point")
    private Integer point;
    
    @Column(name = "refToken")
    private String refToken;

    @Column(name ="provider")
    private Integer provider;
    
    @Column(name = "joinDate")
    private LocalDateTime joinDate;

    // 양방향 관계
    @OneToMany(mappedBy = "customer", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<Review> reviews = new ArrayList<>();
    
    @OneToMany(mappedBy = "customer", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<HotelBookMark> hotelBookMarks = new ArrayList<>();
    
    @OneToMany(mappedBy = "customer", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<RoomBookMark> roomBookMarks = new ArrayList<>();
    
    @OneToMany(mappedBy = "customer", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<RoomReservation> roomReservations = new ArrayList<>();
    
    @OneToMany(mappedBy = "customer", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<RoomPayment> roomPayments = new ArrayList<>();
}

