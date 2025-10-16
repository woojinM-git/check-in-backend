CREATE TABLE `registrationRequest` (
	`registrationIdx`	int	NOT NULL,
	`adminIdx`	int	NOT NULL,
	`contentid`	VARCHAR(50)	NOT NULL,
	`regiDate`	DATETIME	NULL,
	`status`	TINYINT(1)	NULL,
	`approvDate`	DATETIME	NULL
);