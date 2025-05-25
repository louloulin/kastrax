-- Initialize test database for MySQL tests

-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create posts table with foreign key to users
CREATE TABLE IF NOT EXISTS posts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Create comments table with foreign keys to users and posts
CREATE TABLE IF NOT EXISTS comments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    post_id INT NOT NULL,
    user_id INT NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (post_id) REFERENCES posts(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Insert sample users
INSERT INTO users (username, email) VALUES 
('testuser1', 'test1@example.com'),
('testuser2', 'test2@example.com'),
('testuser3', 'test3@example.com');

-- Insert sample posts
INSERT INTO posts (user_id, title, content) VALUES 
(1, 'Test Post 1', 'This is test post 1 by user 1'),
(1, 'Test Post 2', 'This is test post 2 by user 1'),
(2, 'Test Post 3', 'This is test post 3 by user 2'),
(3, 'Test Post 4', 'This is test post 4 by user 3');

-- Insert sample comments
INSERT INTO comments (post_id, user_id, content) VALUES 
(1, 2, 'Comment on post 1 by user 2'),
(1, 3, 'Another comment on post 1 by user 3'),
(2, 3, 'Comment on post 2 by user 3'),
(3, 1, 'Comment on post 3 by user 1');

-- Create an index on the users table
CREATE INDEX idx_users_username ON users(username);

-- Create a unique index on the users table
CREATE UNIQUE INDEX idx_users_email ON users(email); 