# 8. Đóng gói Docker

**Mục tiêu:** người khác chỉ cần cài Docker, tải 2 file, chạy 1 lệnh là có app chạy trên máy / server của họ. Mỗi bản cài (instance) có database riêng, nhiều người dùng đăng nhập Google vào bản cài đó.

## 8.1 Trải nghiệm của người cài

```bash
mkdir workbench && cd workbench
curl -O https://raw.githubusercontent.com/Son30-kins/workbench/main/docker-compose.yml
curl -o .env https://raw.githubusercontent.com/Son30-kins/workbench/main/.env.example

# tạo Google OAuth client + Telegram bot (xem 8.10), rồi điền vào .env
docker compose up -d
```

Mở `http://localhost:3000` → **Đăng nhập bằng Google** → người đăng nhập đầu tiên tự động là **admin** → dùng.

**Cập nhật lên bản mới:**
```bash
docker compose pull && docker compose up -d
```
Migration database tự chạy khi container khởi động — người cài không phải làm gì thêm.

## 8.2 Các container

```
┌──────────────────────── docker compose ─────────────────────────────┐
│                                                                     │
│  web     (sonnld3007/workbench)       Next.js, cổng 3000 ra ngoài   │
│  api     (sonnld3007/workbench-api)   Spring Boot — REST, đăng nhập │
│                                       Google, nhắc việc, bot Telegram│
│  db      (postgres:17-alpine)         volume pgdata                 │
│  caddy   (tuỳ chọn, profile https)    HTTPS tự động                 │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

- Chỉ `web` mở cổng ra ngoài. `api` và `db` chỉ nói chuyện trong mạng nội bộ của compose.
- `api` chạy **Flyway** khi khởi động → database tự cập nhật khi lên bản mới.
- Job nhắc việc và bot Telegram chạy **bên trong** `api` (không còn container worker riêng).
- `caddy` bật bằng `docker compose --profile https up -d` khi có tên miền.

## 8.3 `docker-compose.yml`

File thật: [docker-compose.yml](../docker-compose.yml). Điểm chính:
- **Mỗi container chỉ nhận đúng biến nó cần** (khai báo `environment:` từng biến, không dùng `env_file` chung):
  `web` chỉ biết `API_URL` + `AUTH_SECRET`; key Google / Telegram / mật khẩu DB chỉ vào `api`.
- Thiếu biến bắt buộc (`${VAR:?…}`) → `docker compose` dừng ngay và báo tên biến.
- Healthcheck: `api` → `/actuator/health` (có kiểm tra database); `web` → `/api/health` (gọi được API).
- `web` chờ `api` healthy, `api` chờ `db` healthy.

## 8.4 Cấu hình bằng biến môi trường (`.env.example`)

| Biến | Bắt buộc | Mặc định | Container | Ghi chú |
|---|---|---|---|---|
| `APP_URL` | ✅ | `http://localhost:3000` | api | Địa chỉ người dùng mở app — Google OAuth redirect và link trong Telegram |
| `AUTH_SECRET` | ✅ | — | web | ≥ 32 ký tự (`openssl rand -base64 32`). Gốc sinh khoá mã hoá Server Action riêng của bản cài |
| `POSTGRES_PASSWORD` | ✅ | — | api, db | `openssl rand -hex 24` |
| `GOOGLE_CLIENT_ID` | ✅ | — | api | OAuth client của chính người cài (xem 8.10) |
| `GOOGLE_CLIENT_SECRET` | ✅ | — | api | 🔒 Bí mật |
| `TELEGRAM_BOT_TOKEN` | Nên có | — | api | 🔒 Bí mật. Thiếu thì vẫn chạy, nhắc việc chỉ hiện trong app |
| `COOKIE_SECURE` | | `false` | api | `true` khi chạy HTTPS |
| `APP_VERSION` | | `latest` | web, api | Ghim phiên bản, ví dụ `0.5.1` (2 image luôn cùng số phiên bản) |
| `APP_PORT` | | `3000` | web | Cổng trên máy |
| `TZ` | | `Asia/Ho_Chi_Minh` | api | Múi giờ mặc định cho người dùng mới |
| `ALLOWED_EMAILS` / `ALLOWED_DOMAINS` | | — | api | Giá trị ban đầu; admin sửa tiếp trong app |
| `DOMAIN` | | — | caddy | Chỉ cần khi dùng profile `https` |

**Nguyên tắc:** không có giá trị mặc định cho bí mật; Telegram dùng *long polling* → không cần tên miền public.

## 8.5 Dockerfile

**API** — [backend/Dockerfile](../backend/Dockerfile), multi-stage:
1. **build** (`eclipse-temurin:25-jdk-alpine`): `mvnw package` → tách jar thành các **lớp** (`java -Djarmode=tools … extract --layers`)
   để thư viện ít thay đổi nằm lớp dưới, build lại và kéo image nhanh hơn.
2. **runner** (`eclipse-temurin:25-jre-alpine`): chỉ JRE, user `app` (uid 1001), `-XX:MaxRAMPercentage=75`.
Test không chạy trong Dockerfile (cần Docker cho Testcontainers) — CI chạy `./mvnw verify` trước khi build image.

**Web** — [frontend/Dockerfile](../frontend/Dockerfile), multi-stage trên `node:24-alpine`:
1. **deps** — `npm ci`
2. **build** — `next build` (standalone) → **xoá khoá Next.js tự sinh** khỏi bản build ([build-keys.mjs](../frontend/docker/build-keys.mjs) `scrub`)
3. **runner** — chỉ bản build; bỏ npm/yarn/corepack; user `app` (uid 1001)

`frontend/docker-entrypoint.sh` sinh khoá riêng của bản cài từ `AUTH_SECRET` (Draft Mode + `NEXT_SERVER_ACTIONS_ENCRYPTION_KEY`) rồi `node server.js`.

## 8.6 Phát hành image — Docker Hub

**Mô hình:** chỉ phát hành image, ai muốn thì tự cài. Không vận hành bản cài chung nào.

- 2 repo **public** trên Docker Hub: `sonnld3007/workbench` (web) và `sonnld3007/workbench-api` (API) — luôn phát hành cùng số phiên bản.
- **Tag:**
  | Tag | Ý nghĩa |
  |---|---|
  | `1.2.3` | Đúng một phiên bản — khuyên người cài ghim tag này (`APP_VERSION` trong `.env`) |
  | `1.2`, `1` | Tự nhận bản vá / bản nhỏ mới nhất (tag số major chỉ có từ bản 1.0) |
  | `latest` | Bản ổn định mới nhất |
- **GitHub Actions** (`.github/workflows/release.yml`): khi push tag git `v1.2.3` →
  1. chạy test
  2. build **đa kiến trúc** `linux/amd64` + `linux/arm64` (server thường, Mac chip M, Raspberry Pi) bằng `docker/build-push-action`
  3. đăng nhập Docker Hub bằng secrets `DOCKERHUB_USERNAME` + `DOCKERHUB_TOKEN` (Access Token tạo trong Docker Hub → Account settings, **không dùng mật khẩu**)
  4. đẩy các tag ở bảng trên
  5. cập nhật trang mô tả (Overview) trên Docker Hub từ file `DOCKERHUB.md`
- **Trang Overview trên Docker Hub** là nơi người cài nhìn thấy đầu tiên, nên chứa luôn: cài nhanh, nội dung `docker-compose.yml`, bảng biến môi trường, link tài liệu đầy đủ.
- Ghi chú thay đổi (changelog) cho mỗi bản, cảnh báo nếu có thay đổi cần làm tay.

**Phân phối file `docker-compose.yml` và `.env.example`:** nếu repo mã nguồn public trên GitHub thì người cài tải qua link raw như 8.1. Nếu repo private thì đặt 2 file này trên trang Overview của Docker Hub để người cài copy.

## 8.7 Quản trị bản cài

- **Người đăng nhập đầu tiên = admin.** Admin có trang quản lý: danh sách người dùng, khoá / xoá tài khoản, danh sách email / tên miền được phép đăng nhập.
- **Sao lưu:**
  ```bash
  docker compose exec db pg_dump -U workbench workbench > backup-$(date +%F).sql
  ```
- **Khôi phục:**
  ```bash
  docker compose exec -T db psql -U workbench workbench < backup-2026-10-24.sql
  ```
- Dữ liệu nằm trong volume `pgdata` — `docker compose down` **không** xoá dữ liệu; chỉ `down -v` mới xoá.

## 8.8 Môi trường phát triển

- `docker-compose.dev.yml`: chỉ chạy `db` (Postgres). API chạy `./mvnw spring-boot:run` (thư mục `backend/`), web chạy `npm run dev` (thư mục `frontend/`) ngoài Docker để hot-reload nhanh.
- Trước khi phát hành: build image thật và chạy `docker compose up` từ thư mục trống, làm đúng các bước ở 8.1 để kiểm tra.

## 8.9 Tài liệu cho người cài

`README.md` ở gốc repo có:
1. Yêu cầu: Docker 24+ với Docker Compose v2
2. Cài nhanh (đúng như 8.1)
3. Bảng biến môi trường
4. Tạo Google OAuth client và Telegram bot (như 8.10, có ảnh chụp từng bước)
5. Bật HTTPS với tên miền
6. Cập nhật, sao lưu, khôi phục
7. Xử lý lỗi thường gặp (cổng 3000 bị chiếm, thiếu biến bắt buộc, Google báo `redirect_uri_mismatch`, bot không trả lời)

## 8.10 Người cài tự tạo Google OAuth & Telegram bot

Mỗi bản cài dùng **thông tin xác thực riêng** — không dùng chung key của tác giả (key của tác giả không bao giờ nằm trong image hay repo).

**Google OAuth client** (~5 phút):
1. Vào Google Cloud Console → tạo project → **APIs & Services → OAuth consent screen**: chọn *External*, điền tên app, email.
2. **Credentials → Create credentials → OAuth client ID** → loại *Web application*.
3. **Authorized redirect URI**: `<APP_URL>/api/auth/callback/google`
   - chạy trên máy: `http://localhost:3000/api/auth/callback/google`
   - có tên miền: `https://task.example.com/api/auth/callback/google`
4. Copy Client ID + Client Secret vào `.env`.

**Giới hạn của Google cần ghi rõ cho người cài:**
- Google chỉ chấp nhận redirect về `localhost` hoặc **tên miền có HTTPS**. Truy cập bằng IP mạng LAN (ví dụ `http://192.168.1.10:3000`) sẽ **không** đăng nhập được → muốn dùng từ điện thoại / máy khác thì cần tên miền + profile `https`.
- Consent screen ở chế độ *Testing* chỉ cho tối đa 100 tài khoản test được thêm tay; dùng rộng hơn thì bấm *Publish app* (scope chỉ `openid email profile` nên thường không cần Google duyệt).

**Telegram bot** (~2 phút):
1. Chat với `@BotFather` → `/newbot` → đặt tên → nhận token.
2. Dán token vào `TELEGRAM_BOT_TOKEN` trong `.env`.
3. Một bot chỉ chạy cho **một** bản cài (long polling không dùng chung được).
