# Workbench

App quản lý công việc **tự host**: điền **form mẫu** → app **tự tạo cả chuỗi task** với hạn chót và mức ưu tiên, **nhắc việc qua Telegram** trước khi đến hạn. Đăng nhập bằng Google, nhiều người dùng, dữ liệu riêng từng người. Task lặp lại, form mẫu tự chạy theo lịch, lịch tháng / tuần + link iCal, thống kê + báo cáo tuần. Giao diện **Tiếng Việt / English / Deutsch**.

**Lần đầu cài?** Làm theo [hướng dẫn từng bước cho người mới](https://github.com/Son30-kins/workbench/blob/main/docs/huong-dan-cai-dat.md) — Windows / macOS / Linux, tạo khoá Google, bot Telegram, sao lưu, gỡ lỗi.

Gồm 2 image phát hành cùng phiên bản: **`sonnld3007/workbench`** (giao diện web, Next.js) và **`sonnld3007/workbench-api`** (backend Spring Boot). File `docker-compose.yml` (tải ở bước dưới) chạy cả hai cùng PostgreSQL.

Mã nguồn & tài liệu: https://github.com/Son30-kins/workbench

## Cài đặt nhanh

Cần Docker 24+ (có Docker Compose v2).

```bash
mkdir workbench && cd workbench
curl -O https://raw.githubusercontent.com/Son30-kins/workbench/main/docker-compose.yml
curl -o .env https://raw.githubusercontent.com/Son30-kins/workbench/main/.env.example
```

Điền `.env`:

| Biến | Cách lấy |
|---|---|
| `APP_URL` | Địa chỉ bạn mở app, ví dụ `http://localhost:3000` |
| `AUTH_SECRET` | `openssl rand -base64 32` |
| `POSTGRES_PASSWORD` | `openssl rand -hex 24` |
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` | Tạo OAuth client tại Google Cloud Console, redirect URI `<APP_URL>/api/auth/callback/google` |
| `TELEGRAM_BOT_TOKEN` (nên có) | Chat với `@BotFather` → `/newbot` |

```bash
docker compose up -d
```

Mở `APP_URL` → **Đăng nhập bằng Google** → người đầu tiên là admin.

## Cập nhật

```bash
docker compose pull && docker compose up -d
```

Database tự cập nhật khi khởi động. Nên sao lưu trước:

```bash
docker compose exec db pg_dump -U workbench --clean --if-exists -f /tmp/backup.sql workbench
docker compose cp db:/tmp/backup.sql backup.sql
```

## Tag

| Tag | Ý nghĩa |
|---|---|
| `1.2.3` | Đúng một phiên bản (khuyên dùng — đặt `APP_VERSION=1.2.3` trong `.env`) |
| `1.2` | Bản vá mới nhất của nhánh đó (tag `1` có từ bản 1.0) |
| `latest` | Bản ổn định mới nhất |

Kiến trúc: `linux/amd64`, `linux/arm64` (Mac chip M, Raspberry Pi).

## Bảo mật

Image **không chứa bí mật nào**. Mọi key được truyền lúc chạy qua `.env` trên máy bạn, mỗi container chỉ nhận đúng biến nó cần; khoá nội bộ của Next.js được sinh riêng cho từng bản cài từ `AUTH_SECRET`. API và database không mở cổng ra ngoài. Mỗi bản phát hành được quét bí mật và lỗ hổng (Trivy) trước khi đẩy lên.

Giấy phép MIT.
