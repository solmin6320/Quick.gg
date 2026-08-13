CREATE TABLE student (
                         id INT AUTO_INCREMENT PRIMARY KEY,
                         student_number VARCHAR(10) NOT NULL UNIQUE,
                         name VARCHAR(20) NOT NULL,
                         password VARCHAR(255) NOT NULL,
                         summoner_name VARCHAR(50) NOT NULL,
                         puuid VARCHAR(100) NOT NULL UNIQUE,
                         tag VARCHAR(10) NOT NULL,
                         created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                         UNIQUE(summoner_name, tag)
);