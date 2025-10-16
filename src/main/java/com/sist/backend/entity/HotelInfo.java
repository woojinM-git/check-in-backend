package com.sist.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "hotelInfo")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HotelInfo {
    
    @Id
    @Column(name = "contentId", length = 50)
    private String contentId;

    /* 관리자 고유번호 */
    @Column(name = "adminIdx")
    private Integer adminIdx;
    
    @Column(name = "title")
    private String title;
    
    @Column(name = "adress")
    private String adress;
    
    @Column(name = "tel")
    private String tel;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotelCategoryCode", referencedColumnName = "hotelCategoryCode", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    private Category category;
    
    @Column(name = "hotelCategoryCode", length = 20)
    private String hotelCategoryCode;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "areaCode", referencedColumnName = "areaCode", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    private Area area;
    
    @Column(name = "areaCode", length = 20)
    private String areaCode;
    
    @Column(name = "imageUrl", length = 500)
    private String imageUrl;

    @Column(name = "status")
    private Integer status;
    
    // 양방향 관계
    @OneToOne(mappedBy = "hotelInfo", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private HotelDetail hotelDetail;
    
    @OneToMany(mappedBy = "hotelInfo", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<HotelImage> hotelImages = new ArrayList<>();
    
    @OneToOne(mappedBy = "hotelInfo", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private HotelLocation hotelLocation;
    
    @OneToMany(mappedBy = "hotelInfo", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<Room> rooms = new ArrayList<>();
    
    @OneToMany(mappedBy = "hotelInfo", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<Review> reviews = new ArrayList<>();
    
    @OneToMany(mappedBy = "hotelInfo", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<HotelBookMark> hotelBookMarks = new ArrayList<>();
    
    @OneToMany(mappedBy = "hotelInfo", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<Event> events = new ArrayList<>();
    
    @OneToMany(mappedBy = "hotelInfo", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<Dining> dinings = new ArrayList<>();
}

