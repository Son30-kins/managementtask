-- Báo cáo tuần sáng thứ 2: ngày đã gửi gần nhất (chống gửi trùng khi chạy nhiều instance)
alter table users add column last_weekly_report_on date;
