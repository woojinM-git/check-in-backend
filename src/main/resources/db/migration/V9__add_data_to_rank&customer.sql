INSERT INTO `checkin`.`rank` (`rank`) VALUES ('Traveler');
INSERT INTO `checkin`.`rank` (`rank`) VALUES ('Explorer');
INSERT INTO `checkin`.`rank` (`rank`) VALUES ('VIP');
INSERT INTO `checkin`.`rank` (`rank`) VALUES ('First Class');
INSERT INTO `checkin`.`rank` (`rank`) VALUES ('Sky Suite');

-- 테스트 데이터: 초기 고객 계정 (비밀번호는 프로덕션에서 변경 필요)
-- 주의: 이 비밀번호는 테스트용이며, 실제 프로덕션에서는 반드시 변경해야 합니다.
INSERT INTO `checkin`.`customer` 
(`id`, `rank`, `birthday`, `nickname`, `name`, `gender`, `password`, `phone`, `email`, `cash`, `status`, `totalPrice`, `point`) 
VALUES 
('ten', 'Sky Suite', '2000-01-01', '무한가챠', 'mugen', '남성', '1111', '010-0000-0002', 'ten@naver.com', '1000', '1', '20000', '500');