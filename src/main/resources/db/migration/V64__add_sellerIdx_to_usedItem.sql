-- usedItem 테이블에 sellerIdx 컬럼 추가
ALTER TABLE usedItem ADD COLUMN sellerIdx INT NULL DEFAULT NULL;

-- sellerIdx에 대한 외래키 제약조건 추가 (customer 테이블 참조)
ALTER TABLE usedItem ADD CONSTRAINT fk_usedItem_sellerIdx 
    FOREIGN KEY (sellerIdx) REFERENCES customer(customerIdx) 
    ON DELETE SET NULL ON UPDATE CASCADE;

-- sellerIdx에 대한 인덱스 추가 (조회 성능 향상)
CREATE INDEX idx_usedItem_sellerIdx ON usedItem(sellerIdx);


