-- 랭크테이블에 pointRate , yearCoupon, maxDiscount, condition 컬럼 추가 전부 int형
ALTER TABLE `rank` ADD COLUMN pointRate INT;
ALTER TABLE `rank` ADD COLUMN yearCoupon INT;
ALTER TABLE `rank` ADD COLUMN maxDiscount INT;
ALTER TABLE `rank` ADD COLUMN conditions INT;