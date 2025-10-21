-- V23: Add non-unique search indexes for room table
ALTER TABLE room
    ADD INDEX idx_room_search (contentId, name);

ALTER TABLE room
    ADD INDEX idx_room_content (contentId);

ALTER TABLE room
    ADD INDEX idx_room_name (name);