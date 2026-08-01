CREATE TABLE favorite_summoner (
                                   id INT AUTO_INCREMENT PRIMARY KEY,
                                   student_id INT NOT NULL,
                                   summoner_name VARCHAR(50) NOT NULL,
                                   tag VARCHAR(10) NOT NULL,
                                   UNIQUE(student_id, summoner_name, tag),

                                   FOREIGN KEY(student_id)
                                       REFERENCES student(id)
                                       ON DELETE CASCADE
);