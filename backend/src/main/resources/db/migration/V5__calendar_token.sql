-- Link lịch iCal: chỉ lưu SHA-256 của token bí mật trong URL. null = chưa bật.
alter table users add column calendar_token_hash text unique;
