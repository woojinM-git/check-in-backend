CREATE TABLE IF NOT EXISTS recentViewedHotel (
    recentViewedIdx INT NOT NULL AUTO_INCREMENT,
    customerIdx INT NOT NULL,
    contentId VARCHAR(50) NOT NULL,
    viewedAt DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    createdAt DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updatedAt DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (recentViewedIdx),
    CONSTRAINT uk_recentViewed_customer_content UNIQUE (customerIdx, contentId),
    INDEX idx_recentViewed_customer_viewedAt (customerIdx, viewedAt),
    CONSTRAINT fk_recentViewed_customer FOREIGN KEY (customerIdx)
        REFERENCES customer (customerIdx) ON DELETE CASCADE,
    CONSTRAINT fk_recentViewed_hotel FOREIGN KEY (contentId)
        REFERENCES hotelInfo (contentId) ON DELETE CASCADE
);

