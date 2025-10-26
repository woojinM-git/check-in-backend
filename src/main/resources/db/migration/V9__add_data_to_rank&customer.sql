INSERT INTO `checkin`.`rank` (`rank`) VALUES ('Traveler');
INSERT INTO `checkin`.`rank` (`rank`) VALUES ('Explorer');
INSERT INTO `checkin`.`rank` (`rank`) VALUES ('VIP');
INSERT INTO `checkin`.`rank` (`rank`) VALUES ('First Class');
INSERT INTO `checkin`.`rank` (`rank`) VALUES ('Sky Suite');

INSERT INTO `checkin`.`customer` 
(`id`, `rank`, `birthday`, `nickname`, `name`, `gender`, `password`, `phone`, `email`, `cash`, `status`, `totalPrice`, `point`) 
VALUES 
('ten', 'Sky Suite', '2000-01-01', '무한가챠', 'mugen', '남성', '1111', '010-0000-0002', 'ten@naver.com', '1000', '1', '20000', '500');