CREATE TABLE refresh_token (
                               id INT AUTO_INCREMENT PRIMARY KEY,
                               student_id INT NOT NULL,
                               token VARCHAR(500) NOT NULL,
                               expires_at DATETIME NOT NULL,
                               revoked BOOLEAN DEFAULT FALSE,
                               created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

                               FOREIGN KEY(student_id)
                                   REFERENCES student(id)
                                   ON DELETE CASCADE
);