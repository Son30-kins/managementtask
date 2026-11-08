# Workbench — Tài liệu dự án

App quản lý công việc tự host bằng Docker, nhiều người dùng (đăng nhập Google, dữ liệu riêng từng người), nhắc việc qua Telegram: điền **form mẫu** → app **tự sinh task**, có **mức độ ưu tiên**, và **thông báo** khi task sắp đến hạn.

## Mục lục

| # | Tài liệu | Nội dung |
|---|----------|----------|
| 1 | [01-tong-quan.md](01-tong-quan.md) | Mục tiêu, phạm vi, người dùng |
| 2 | [02-yeu-cau-chuc-nang.md](02-yeu-cau-chuc-nang.md) | Tính năng chi tiết + user stories |
| 3 | [03-mo-hinh-du-lieu.md](03-mo-hinh-du-lieu.md) | Các bảng dữ liệu, quan hệ |
| 4 | [04-kien-truc-cong-nghe.md](04-kien-truc-cong-nghe.md) | Công nghệ đề xuất, kiến trúc |
| 5 | [05-lo-trinh.md](05-lo-trinh.md) | Các giai đoạn, checklist công việc |
| 6 | [06-cau-hoi-mo.md](06-cau-hoi-mo.md) | Những điểm cần bạn quyết định |
| 7 | [07-ui-design.md](07-ui-design.md) | Thiết kế giao diện (theo Figma), design tokens |
| 8 | [08-dong-goi-docker.md](08-dong-goi-docker.md) | Đóng gói Docker, cấu hình, phát hành image |
| 9 | [09-git-va-bao-mat.md](09-git-va-bao-mat.md) | Quy trình Git, quản lý & bảo vệ key bí mật |
| 10 | [10-backend-spring-boot.md](10-backend-spring-boot.md) | Backend Spring Boot: 4 tầng DDD, luồng request, cách thêm tính năng, Lombok, đa ngôn ngữ |
| — | [huong-dan-cai-dat.md](huong-dan-cai-dat.md) | **Hướng dẫn cài cho người mới**: Docker, khoá Google, bot Telegram, `.env`, sao lưu, gỡ lỗi |

## Trạng thái

- 2026-10-24: Khởi tạo tài liệu, lên plan. Chưa có code.
- 2026-10-24: Đọc design Figma (Kanban board, Light/Dark), viết tài liệu UI, lưu ảnh vào `ui-reference/`.
- 2026-10-24: Chốt nhiều người dùng — thêm đăng ký / đăng nhập, phân quyền dữ liệu, chuyển sang PostgreSQL, thêm giai đoạn Tài khoản và Deploy vào lộ trình.
- 2026-10-24: Chốt phát hành bằng Docker — người khác kéo image và chạy `docker compose up -d`; email đổi sang SMTP, thêm vai trò admin.
- 2026-10-24: Chốt chỉ phát hành image (không vận hành bản cài chung), đăng lên Docker Hub.
- 2026-10-24: Chốt thông báo qua Telegram, đăng nhập chỉ bằng Google (bỏ mật khẩu, email, SMTP), code quản lý trên GitHub, quy tắc bảo vệ key bí mật.
- 2026-10-24: **Code xong MVP** (giai đoạn 0–5): đăng nhập Google, board/danh sách, form mẫu, nhắc việc Telegram, quản trị, Docker, CI/CD. Đã kiểm thử trên máy dev và cài thử bằng Docker. Còn chờ: tạo repo GitHub, token Docker Hub, Google OAuth client, Telegram bot.
- 2026-10-25: Đổi tên dự án **ManagementTask → Workbench** (repo GitHub, image Docker Hub, tên app, database).
- 2026-10-25: **Phát hành v0.1.0** lên Docker Hub [`sonnld3007/workbench`](https://hub.docker.com/r/sonnld3007/workbench) (amd64 + arm64). Đã cài thử từ thư mục trống bằng image công khai và quét lại image: không có bí mật.
- 2026-10-25: **Chuyển backend sang Spring Boot 4** (Java 25, Maven, DDD, Lombok). Next.js chỉ còn là giao diện, gọi REST API. Phát hành 2 image: `workbench` (web) + `workbench-api`.
- 2026-10-25: Giao diện mèo cute (pastel hồng – đào, font Nunito, mascot SVG); đa ngôn ngữ **Tiếng Việt / English / Deutsch** (cả lỗi API, tin Telegram, form mẫu mặc định); thẻ task hiện task con. Viết [hướng dẫn cài cho người mới](huong-dan-cai-dat.md). Phát hành **v0.3.0**.
- 2026-10-25: **Mèo nhắc việc** — bé mèo mướp xám bước bằng 4 chân có khớp, thỉnh thoảng đi ra góc màn hình nhắc task quá hạn / sắp đến hạn, ngồi nói rồi chạy đi (bật / tắt trong Cài đặt). Phát hành **v0.4.0**.
- 2026-10-25: Bỏ chuyển động của mèo nhắc việc — chỉ hiện thẻ có hình mèo (ảnh tĩnh) kèm lời nhắc. Phát hành **v0.4.1**.
- 2026-10-25: Lời nhắc của mèo chuyển thành bong bóng thoại trên đầu mèo. Phát hành **v0.4.2**.
- 2026-11-01: **Task lặp lại**, **form mẫu tự chạy theo lịch**, **lịch tháng / tuần + link iCal**, **thống kê + báo cáo tuần** (Flyway V3–V6). Phát hành **v0.5.0**.
- 2026-11-08: Sửa: mèo không còn nhắc task đã xong / đã huỷ; compose chờ `api` khởi động lâu hơn trên máy chậm. Phát hành **v0.5.1**.
