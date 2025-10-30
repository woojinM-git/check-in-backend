-- Create table for check-in and check-out times
-- 입실, 퇴실 시간 테이블 생성

CREATE TABLE IF NOT EXISTS reservationTime (
    orderIdx INT NOT NULL PRIMARY KEY COMMENT '결제번호',
    inTime DATETIME DEFAULT NULL COMMENT '체크인 시간',
    outTime DATETIME DEFAULT NULL COMMENT '체크아웃 시간',
    FOREIGN KEY (orderIdx) REFERENCES roomPayment(orderIdx) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='입실, 퇴실 시간';