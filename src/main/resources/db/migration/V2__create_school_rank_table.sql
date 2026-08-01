CREATE TABLE school_rank (
                             id INT AUTO_INCREMENT PRIMARY KEY,
                             student_id INT NOT NULL UNIQUE,
                             tier VARCHAR(20) NOT NULL,
                             rank_tier VARCHAR(5),
                             lp INT NOT NULL,
                             wins INT NOT NULL,
                             losses INT NOT NULL,
                             updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,

                             FOREIGN KEY(student_id)
                                 REFERENCES student(id)
                                 ON DELETE CASCADE
);