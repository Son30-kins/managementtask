# Workbench

App quản lý công việc tự host bằng Docker: điền **form mẫu** → app **tự sinh task**, có **mức độ ưu tiên**, **nhắc việc qua Telegram** khi sắp đến hạn. Đăng nhập bằng Google, nhiều người dùng, dữ liệu riêng từng người.

[![CI](https://github.com/Son30-kins/workbench/actions/workflows/ci.yml/badge.svg)](https://github.com/Son30-kins/workbench/actions/workflows/ci.yml)
[![Docker Hub](https://img.shields.io/docker/v/sonnld3007/workbench?label=docker%20hub)](https://hub.docker.com/r/sonnld3007/workbench)

## Tính năng

- **Form mẫu → nhiều task**: tự định nghĩa form (chữ, số, ngày, lựa chọn, có/không) và quy tắc sinh task. Tiêu đề dùng biến `{{ten_truong}}`, hạn chót tính tương đối theo một trường ngày ("trước hạn nộp 2 ngày, lúc 17:00"), task có điều kiện, task con, nhãn. Xem trước rồi mới tạo.
- **Bảng Kanban** kéo thả (Cần làm / Đang làm / Hoàn thành) và **danh sách**; lọc Hôm nay / Quá hạn / 7 ngày tới; tìm kiếm.
- **4 mức ưu tiên** — Khẩn cấp, Cao, Trung bình, Thấp — mỗi mức có lịch nhắc riêng.
- **Task lặp lại**: hằng ngày / ngày làm việc / hằng tuần (chọn thứ) / hằng tháng / mỗi N ngày-tuần-tháng; đánh dấu xong là có ngay lần kế tiếp.
- **Form mẫu tự chạy theo lịch**: đặt lịch + giá trị sẵn (trường ngày tính theo ngày chạy, vd. "hạn nộp = ngày chạy + 4"), đến giờ app tự tạo task và báo bạn.
- **Lịch tháng / tuần**: kéo task sang ngày khác để đổi hạn; **link iCal** để xem hạn task trong Google / Apple Calendar.
- **Thống kê**: việc xong mỗi tuần (đúng hạn / trễ hạn), tỉ lệ đúng hạn, việc đang mở theo ưu tiên; **báo cáo tuần** sáng thứ 2 qua Telegram.
- **Nhắc việc qua Telegram** với nút ✅ Xong / ⏰ Hoãn ngay trong tin nhắn; nhắc lại khi quá hạn; bản tin buổi sáng; giờ yên lặng.
- **Chuông thông báo** trong app, **Sáng / Tối**, dùng được trên điện thoại.
- **Mèo nhắc việc**: thỉnh thoảng một bé mèo hiện ở góc màn hình, bong bóng trên đầu nhắc task quá hạn / sắp đến hạn, bấm là mở task (bật / tắt trong Cài đặt).
- **3 ngôn ngữ — Tiếng Việt, English, Deutsch**: tự chọn theo trình duyệt, đổi ở góc trên hoặc trong Cài đặt; áp dụng cho cả thông báo lỗi, tin nhắn Telegram và 3 form mẫu mặc định của tài khoản mới.
- **Quản trị**: người đăng nhập đầu tiên là admin; giới hạn email / tên miền được đăng nhập; khoá tài khoản.

## Cài đặt

Lần đầu cài? Làm theo **[hướng dẫn từng bước cho người mới](docs/huong-dan-cai-dat.md)** (Windows / macOS / Linux, khoá Google, bot Telegram, sao lưu, gỡ lỗi). Chi tiết kỹ thuật: [docs/08-dong-goi-docker.md](docs/08-dong-goi-docker.md).

```bash
mkdir workbench && cd workbench
curl -O https://raw.githubusercontent.com/Son30-kins/workbench/main/docker-compose.yml
curl -o .env https://raw.githubusercontent.com/Son30-kins/workbench/main/.env.example
# điền .env (Google OAuth, Telegram bot, mật khẩu…)
docker compose up -d
```

## Phát triển

Cần **Java 25**, **Node.js 24+**, **Docker**. Backend: Spring Boot 4 (Maven, DDD, Lombok) — đọc [docs/10-backend-spring-boot.md](docs/10-backend-spring-boot.md).

```bash
git clone https://github.com/Son30-kins/workbench.git
cd workbench
brew install gitleaks                    # bắt buộc — quét bí mật trước mỗi commit
git config core.hooksPath .githooks      # bật hook chặn commit có bí mật
cp .env.example .env.local               # điền key dev (Google OAuth client cho localhost)
# trong .env.local:  DATABASE_URL=jdbc:postgresql://127.0.0.1:5433/workbench
#                    DATABASE_USER=workbench
docker compose -f docker-compose.dev.yml up -d   # Postgres cho dev (chỉ mở trên 127.0.0.1)
(cd backend && ./mvnw spring-boot:run)           # API: http://localhost:8080 (Flyway tự tạo bảng)
(cd frontend && npm ci && npm run dev)           # web: http://localhost:3000 (cửa sổ terminal khác)
```

Dùng Claude Code (macOS) thì gõ `/start-web` để bật cả database, API và web một lần, `/end-web` để tắt (giữ dữ liệu) — xem [.claude/skills/](.claude/skills/).

| Lệnh | Việc |
|---|---|
| `cd backend && ./mvnw test` | Unit test + test tích hợp (Testcontainers, cần Docker) |
| `cd backend && ./mvnw spring-boot:run` | Chạy API; thêm `API_DOCS_ENABLED=true` để mở Swagger UI |
| `cd frontend && npm run lint && npm run typecheck` | Kiểm tra frontend |
| `cd frontend && npm run build` | Build web |

Công nghệ: Spring Boot 4.1 · Java 25 · Spring Security (Google OIDC) · Spring Data JPA · Flyway · PostgreSQL 17 · Next.js 16 · React 19 · Tailwind CSS 4.

## Phát hành

Tạo tag `vX.Y.Z` trên `main` → GitHub Actions quét bí mật & lỗ hổng, build 2 image (`workbench`, `workbench-api`) cho `linux/amd64` + `linux/arm64` và đẩy lên Docker Hub.

## Bảo mật

- Không commit `.env`, key, token, mật khẩu — hook pre-commit và CI (gitleaks) sẽ chặn.
- Image trên Docker Hub không chứa bí mật nào; khoá nội bộ được sinh riêng cho từng bản cài lúc khởi động.
- Phát hiện lỗ hổng: báo riêng qua [GitHub Security Advisories](https://github.com/Son30-kins/workbench/security/advisories/new), không tạo issue công khai.

Chi tiết: [docs/09-git-va-bao-mat.md](docs/09-git-va-bao-mat.md) · Tài liệu thiết kế: [docs/](docs/README.md)

## Ghi nguồn & giấy phép

Giao diện tham khảo [To do list dashboard (Freebie)](https://www.figma.com/community) trên Figma Community. Mã nguồn theo giấy phép [MIT](LICENSE).
