-- Quy tắc lặp lại của task (jsonb: frequency, interval, weekdays, monthDay). null = không lặp.
alter table tasks add column recurrence jsonb;
