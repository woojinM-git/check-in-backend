-- V22: Safe reset of roomIdx primary key (includes review, roomBookMark, roomReservation)
SET FOREIGN_KEY_CHECKS = 0;

-- 1️⃣ 외래키 전부 삭제
ALTER TABLE review DROP FOREIGN KEY FK_roomReservation_TO_review_2;
ALTER TABLE roomBookMark DROP FOREIGN KEY FK_room_TO_roomBookMark_1;
ALTER TABLE roomReservation DROP FOREIGN KEY FK_room_TO_roomReservation_1;

-- 2️⃣ roomIdx 리셋 준비 (PK/AUTO_INCREMENT 제거)
ALTER TABLE room MODIFY roomIdx INT;
ALTER TABLE room DROP PRIMARY KEY;

-- 3️⃣ roomIdx 재정렬
SET @rownum = 0;
UPDATE room
SET roomIdx = (@rownum := @rownum + 1)
    ORDER BY contentId, name;

-- 4️⃣ PK + AUTO_INCREMENT 복구
ALTER TABLE room MODIFY roomIdx INT AUTO_INCREMENT PRIMARY KEY;

-- 5️⃣ 외래키 다시 추가 (이름 중복 방지 위해 새 이름으로 부여)
ALTER TABLE review
    ADD CONSTRAINT fk_review_roomIdx
        FOREIGN KEY (roomIdx) REFERENCES room(roomIdx);

ALTER TABLE roomBookMark
    ADD CONSTRAINT fk_roomBookMark_roomIdx
        FOREIGN KEY (roomIdx) REFERENCES room(roomIdx);

ALTER TABLE roomReservation
    ADD CONSTRAINT fk_roomReservation_roomIdx
        FOREIGN KEY (roomIdx) REFERENCES room(roomIdx);

SET FOREIGN_KEY_CHECKS = 1;
