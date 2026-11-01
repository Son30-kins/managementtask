# Hướng dẫn cài Workbench cho người mới

Workbench là app quản lý công việc chạy **trên máy của bạn** (tự host): điền một form mẫu là app tự tạo cả chuỗi
task có hạn chót và mức ưu tiên, nhắc việc qua Telegram trước khi đến hạn. Giao diện có Tiếng Việt, English và
Deutsch.

Hướng dẫn này dành cho người **chưa từng cài**. Làm lần lượt từ trên xuống, mất khoảng **30 phút**. Bạn không cần
biết lập trình — chỉ cần gõ (hoặc dán) vài lệnh vào cửa sổ dòng lệnh.

**Bạn sẽ có:** app chạy ở `http://localhost:3000` trên máy của bạn, đăng nhập bằng tài khoản Google của bạn, dữ
liệu nằm hoàn toàn trên máy bạn. Người đăng nhập đầu tiên là **quản trị viên (admin)**.

---

## Mục lục

1. [Chuẩn bị](#1-chuẩn-bị)
2. [Cài Docker](#2-cài-docker)
3. [Tải file cài đặt](#3-tải-file-cài-đặt)
4. [Tạo khoá đăng nhập Google](#4-tạo-khoá-đăng-nhập-google)
5. [Tạo bot Telegram (nên có)](#5-tạo-bot-telegram-nên-có)
6. [Điền file cấu hình `.env`](#6-điền-file-cấu-hình-env)
7. [Chạy app](#7-chạy-app)
8. [Dùng lần đầu](#8-dùng-lần-đầu)
9. [Việc thường ngày: tắt, bật, cập nhật, sao lưu](#9-việc-thường-ngày-tắt-bật-cập-nhật-sao-lưu)
10. [Gặp lỗi?](#10-gặp-lỗi)
11. [Giữ an toàn cho khoá bí mật](#11-giữ-an-toàn-cho-khoá-bí-mật)

---

## 1. Chuẩn bị

| Cần có | Ghi chú |
|---|---|
| Máy tính Windows 10/11, macOS hoặc Linux | RAM còn trống ít nhất 2 GB, ổ cứng trống 3 GB |
| Tài khoản Google | Để đăng nhập app và tạo khoá Google (miễn phí) |
| Telegram trên điện thoại | Không bắt buộc — để nhận nhắc việc |
| Kết nối Internet | Để tải app lần đầu |

**Cửa sổ dòng lệnh** là nơi bạn gõ các lệnh trong hướng dẫn:

- **Windows:** bấm nút Start, gõ `PowerShell`, mở **Windows PowerShell**.
- **macOS:** mở **Terminal** (Launchpad → gõ `Terminal`).
- **Linux:** mở **Terminal**.

> Mỗi khối lệnh bên dưới: chọn đúng khối cho hệ điều hành của bạn, dán vào cửa sổ dòng lệnh rồi nhấn **Enter**.

---

## 2. Cài Docker

Docker là phần mềm chạy app trong các "hộp" riêng — bạn không phải cài Java, Node hay database.

- **Windows / macOS:** tải **Docker Desktop** tại <https://www.docker.com/products/docker-desktop/>, cài như phần mềm
  bình thường rồi mở lên. Chờ tới khi biểu tượng cá voi báo **"Docker Desktop is running"**.
  - Windows: nếu được hỏi về **WSL 2**, chọn đồng ý và khởi động lại máy khi được yêu cầu.
  - Mac chip Apple (M1/M2/M3…): chọn bản **Apple Silicon**.
- **Linux:** cài Docker Engine theo <https://docs.docker.com/engine/install/> (có sẵn Docker Compose).

Kiểm tra đã cài xong — gõ:

```bash
docker compose version
```

Thấy dòng kiểu `Docker Compose version v2.x.x` là được.

---

## 3. Tải file cài đặt

Tạo một thư mục tên `workbench` và tải 2 file: `docker-compose.yml` (mô tả app) và `.env` (cấu hình của riêng bạn).

**macOS / Linux:**

```bash
mkdir workbench && cd workbench
curl -O https://raw.githubusercontent.com/Son30-kins/workbench/main/docker-compose.yml
curl -o .env https://raw.githubusercontent.com/Son30-kins/workbench/main/.env.example
```

**Windows (PowerShell):**

```powershell
mkdir workbench; cd workbench
curl.exe -O https://raw.githubusercontent.com/Son30-kins/workbench/main/docker-compose.yml
curl.exe -o .env https://raw.githubusercontent.com/Son30-kins/workbench/main/.env.example
```

> Từ giờ, mọi lệnh đều chạy **bên trong thư mục `workbench`** này. Lần sau mở lại cửa sổ dòng lệnh, nhớ gõ
> `cd workbench` trước.

---

## 4. Tạo khoá đăng nhập Google

App đăng nhập bằng Google nên bạn cần một **OAuth client** của riêng mình. Miễn phí, không cần thẻ thanh toán.

1. Mở <https://console.cloud.google.com/> và đăng nhập tài khoản Google của bạn.
2. Ở thanh trên cùng, bấm ô chọn project → **New Project** → đặt tên `Workbench` → **Create**. Chờ tạo xong rồi chọn
   project đó.
3. Ô tìm kiếm trên cùng → gõ **Google Auth Platform** → mở → bấm **Get started**:
   - **App name:** `Workbench`; **User support email:** email của bạn → **Next**.
   - **Audience:** chọn **External** → **Next**.
   - **Contact information:** email của bạn → **Next** → tick đồng ý → **Create**.
4. Menu bên trái → **Audience** → mục **Test users** → **Add users** → nhập **email Google bạn sẽ dùng để đăng nhập
   app** → **Save**.
   > App đang ở chế độ *Testing*: chỉ những email trong danh sách này đăng nhập được. Muốn thêm người dùng chung bản
   > cài của bạn thì thêm email của họ vào đây.
5. Menu bên trái → **Clients** → **Create client**:
   - **Application type:** `Web application`; **Name:** `Workbench`.
   - **Authorized redirect URIs** → **Add URI** → dán chính xác:

     ```
     http://localhost:3000/api/auth/callback/google
     ```

   - Bấm **Create**.
6. Cửa sổ hiện ra **Client ID** và **Client secret**. Giữ cửa sổ này mở (hoặc tải file JSON về) — bước 6 sẽ dùng.

> **Client secret là bí mật.** Không gửi cho ai, không chụp màn hình đăng lên mạng.

---

## 5. Tạo bot Telegram (nên có)

Bỏ qua bước này nếu chỉ cần thông báo trong app. Có bot thì bạn nhận nhắc việc trên điện thoại, kèm nút **✅ Xong /
⏰ Hoãn** ngay trong tin nhắn.

1. Mở Telegram, tìm **@BotFather** (có dấu tích xanh) → bấm **Start**.
2. Gửi `/newbot`.
3. Đặt tên hiển thị, ví dụ `Workbench của Lan`.
4. Đặt username kết thúc bằng `bot`, ví dụ `workbench_lan_bot` (phải chưa ai dùng).
5. BotFather trả về một **token** dạng `123456789:AAH...`. Giữ lại cho bước 6.

> **Token là bí mật** — ai có token sẽ điều khiển được bot của bạn. Mỗi bản cài cần **bot riêng**: một token không
> chạy được ở hai máy cùng lúc.

---

## 6. Điền file cấu hình `.env`

### 6.1 Tạo 2 chuỗi ngẫu nhiên

App cần một chuỗi bí mật (`AUTH_SECRET`) và mật khẩu database (`POSTGRES_PASSWORD`). Chạy lệnh dưới, **mỗi lệnh in
ra một chuỗi** — giữ lại cả hai.

**macOS / Linux:**

```bash
openssl rand -base64 32   # → AUTH_SECRET
openssl rand -hex 24      # → POSTGRES_PASSWORD
```

**Windows (PowerShell):**

```powershell
$b = New-Object byte[] 32; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)   # → AUTH_SECRET
$b = New-Object byte[] 24; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); -join ($b | ForEach-Object { $_.ToString("x2") })   # → POSTGRES_PASSWORD
```

### 6.2 Mở file `.env`

- **macOS:** `open -e .env`
- **Windows:** `notepad .env`
- **Linux:** `nano .env` (lưu: `Ctrl+O`, Enter, thoát: `Ctrl+X`)

### 6.3 Điền các dòng sau

Điền ngay sau dấu `=`, **không có dấu cách, không có dấu ngoặc kép**:

| Dòng | Điền gì |
|---|---|
| `APP_URL=` | Giữ nguyên `http://localhost:3000` |
| `AUTH_SECRET=` | Chuỗi thứ nhất ở bước 6.1 |
| `POSTGRES_PASSWORD=` | Chuỗi thứ hai ở bước 6.1 |
| `GOOGLE_CLIENT_ID=` | Client ID ở bước 4 (kết thúc bằng `.apps.googleusercontent.com`) |
| `GOOGLE_CLIENT_SECRET=` | Client secret ở bước 4 |
| `TELEGRAM_BOT_TOKEN=` | Token ở bước 5 — bỏ trống nếu không dùng Telegram |
| `TZ=` | Múi giờ của bạn, mặc định `Asia/Ho_Chi_Minh` (ví dụ `Europe/Berlin`) |

Các dòng còn lại giữ nguyên. File sau khi điền trông như sau — thay **cả phần `<…>`** (kể cả dấu `<` `>`) bằng
giá trị của bạn:

```dotenv
APP_URL=http://localhost:3000
AUTH_SECRET=<chuỗi thứ nhất ở bước 6.1>
POSTGRES_PASSWORD=<chuỗi thứ hai ở bước 6.1>
GOOGLE_CLIENT_ID=<Client ID ở bước 4, dạng 1234…-abc….apps.googleusercontent.com>
GOOGLE_CLIENT_SECRET=<Client secret ở bước 4, bắt đầu bằng GOCSPX->
TELEGRAM_BOT_TOKEN=<token ở bước 5, dạng 123456789:AAH…>
TZ=Asia/Ho_Chi_Minh
```

**Lưu file.**

---

## 7. Chạy app

```bash
docker compose up -d
```

Lần đầu Docker tải app về (vài trăm MB, mất vài phút tuỳ mạng). Sau đó kiểm tra:

```bash
docker compose ps
```

Chờ tới khi cột **STATUS** của `web`, `api`, `db` đều có chữ **`(healthy)`** — thường 1–2 phút. Nếu sau 5 phút vẫn
chưa, xem mục [10. Gặp lỗi?](#10-gặp-lỗi).

Mở trình duyệt vào **<http://localhost:3000>**.

---

## 8. Dùng lần đầu

1. Bấm **Đăng nhập bằng Google** → chọn tài khoản đã thêm vào *Test users* ở bước 4.
   - Google có thể hiện cảnh báo *"Google hasn't verified this app"* vì app đang ở chế độ Testing. Đây là app của
     chính bạn — bấm **Continue**.
2. Bạn là người đầu tiên → tự động là **admin**. Tài khoản mới có sẵn **3 form mẫu** (báo cáo tuần, chuẩn bị họp,
   việc nhanh) để thử.
3. **Đổi ngôn ngữ:** nút **🌐 VI / EN / DE** ở góc trên, hoặc **Cài đặt → Ngôn ngữ**.
4. **Kết nối Telegram** (nếu đã làm bước 5): **Cài đặt → Kết nối Telegram** → Telegram mở ra → bấm **Start** → quay
   lại app tải lại trang. Bấm **Gửi tin thử** để kiểm tra.
5. **Lịch nhắc:** **Cài đặt** → chọn nhắc trước hạn bao lâu cho từng mức ưu tiên, giờ yên lặng, bản tin buổi sáng,
   báo cáo tuần.
6. **Xem hạn task trong Google / Apple Calendar:** **Cài đặt → Lịch iCal → Tạo link lịch** rồi dán link vào ứng
   dụng lịch. Apple Calendar trên chính máy cài dùng được ngay; Google Calendar tải link từ Internet nên chỉ hoạt động
   khi app chạy với tên miền công khai (mục 9.5).

Lưu ý: địa chỉ `localhost` chỉ mở được trên **chính máy cài**. Muốn mở từ điện thoại hoặc máy khác, xem mục 9.5.

---

## 9. Việc thường ngày: tắt, bật, cập nhật, sao lưu

Mọi lệnh chạy trong thư mục `workbench`.

### 9.1 Tắt và bật

```bash
docker compose stop     # tắt (dữ liệu vẫn giữ)
docker compose start    # bật lại
```

Docker Desktop đang mở thì app **tự chạy lại** sau khi khởi động máy. Muốn Docker Desktop tự mở cùng máy: Docker
Desktop → **Settings → General → Start Docker Desktop when you sign in**.

### 9.2 Cập nhật lên bản mới

```bash
docker compose pull
docker compose up -d
```

Database tự nâng cấp khi app khởi động. Nên **sao lưu trước** (mục 9.3).

### 9.3 Sao lưu dữ liệu

Giống nhau trên mọi hệ điều hành:

```bash
docker compose exec db pg_dump -U workbench --clean --if-exists -f /tmp/backup.sql workbench
docker compose cp db:/tmp/backup.sql backup.sql
```

Thư mục `workbench` giờ có file `backup.sql` chứa toàn bộ task của bạn. Đổi tên kèm ngày (ví dụ
`backup-2026-10-25.sql`) rồi cất ở nơi an toàn (ổ ngoài, Google Drive riêng…).

### 9.4 Khôi phục từ bản sao lưu

> Thao tác này **ghi đè** dữ liệu hiện có bằng nội dung file sao lưu.

Đặt file sao lưu vào thư mục `workbench` với tên `backup.sql`, rồi chạy:

```bash
docker compose stop web api
docker compose cp backup.sql db:/tmp/backup.sql
docker compose exec db psql -U workbench -d workbench -q -f /tmp/backup.sql
docker compose start api web
```

### 9.5 Mở app từ điện thoại hoặc máy khác (nâng cao)

Google chỉ cho đăng nhập qua `http://localhost` hoặc qua **tên miền có HTTPS** — không chấp nhận địa chỉ IP nội bộ
kiểu `http://192.168.1.10:3000`. Để mở từ thiết bị khác bạn cần:

1. Một tên miền trỏ về máy cài (ví dụ `workbench.ten-cua-ban.com`), mở cổng 80 và 443 trên router.
2. Trong `.env`: `DOMAIN=workbench.ten-cua-ban.com`, `APP_URL=https://workbench.ten-cua-ban.com`,
   `COOKIE_SECURE=true`.
3. Google Cloud → **Clients** → client Workbench → thêm redirect URI
   `https://workbench.ten-cua-ban.com/api/auth/callback/google` → **Save**.
4. Chạy `docker compose --profile https up -d` — app tự lấy chứng chỉ HTTPS miễn phí.

### 9.6 Gỡ app

```bash
docker compose down        # gỡ app, GIỮ dữ liệu
docker compose down -v     # gỡ app VÀ XOÁ HẾT dữ liệu — không lấy lại được
```

---

## 10. Gặp lỗi?

| Hiện tượng | Cách xử lý |
|---|---|
| `docker compose up` báo **`Chưa đặt … trong .env`** | Dòng đó trong `.env` còn trống — điền rồi chạy lại |
| `Cannot connect to the Docker daemon` / `docker: command not found` | Docker Desktop chưa mở — mở và chờ báo *running* |
| Google báo **`Error 400: redirect_uri_mismatch`** | Redirect URI ở bước 4 phải **đúng từng ký tự**: `http://localhost:3000/api/auth/callback/google` (không có `/` ở cuối, `http` chứ không phải `https`) |
| Google báo **`Access blocked`** / *app is being tested* | Email bạn dùng chưa có trong **Audience → Test users** (bước 4.4) |
| Trang đăng nhập báo **email không có trong danh sách** | Admin giới hạn email trong **Quản trị → Ai được đăng nhập** |
| Cổng 3000 đã bị chiếm (`port is already allocated`) | Trong `.env` đổi `APP_PORT=3001` **và** `APP_URL=http://localhost:3001`, sửa redirect URI ở Google cho khớp, rồi `docker compose up -d` |
| `up -d` báo **`dependency failed to start: container … is unhealthy`** | Máy chậm hoặc đang bận (game, nhiều ứng dụng) nên `api` khởi động lâu hơn thời gian chờ. Tải file `docker-compose.yml` mới nhất (bước 3) rồi chạy lại `docker compose up -d` — dữ liệu không mất |
| `api` mãi không `(healthy)` | Xem nhật ký: `docker compose logs api --tail 50`. Lỗi mật khẩu database thường do đổi `POSTGRES_PASSWORD` sau lần chạy đầu — đặt lại giá trị cũ |
| Bot Telegram không trả lời | Xem `docker compose logs api --tail 100`, tìm dòng có chữ *Telegram*. Sai token thì sửa `.env` rồi `docker compose up -d`; token đang chạy ở máy khác thì tạo bot mới |
| Muốn xem nhật ký | `docker compose logs -f` (thoát: `Ctrl+C`) |

Vẫn không được? Gửi kết quả của `docker compose ps` và `docker compose logs api --tail 50` cho người hướng dẫn bạn
— **xoá các dòng có khoá bí mật trước khi gửi** (dù app không in khoá ra nhật ký).

---

## 11. Giữ an toàn cho khoá bí mật

- File `.env` chứa **mọi khoá bí mật** — không gửi qua chat, email, không đưa lên GitHub hay Google Drive công khai.
- Mỗi bản cài dùng **khoá của riêng mình**: OAuth client Google, bot Telegram, `AUTH_SECRET`, `POSTGRES_PASSWORD`.
- Lỡ để lộ:
  - **Client secret Google:** Google Cloud → Clients → client Workbench → tạo secret mới, xoá secret cũ → sửa `.env`
    → `docker compose up -d`.
  - **Token Telegram:** @BotFather → `/revoke` → chọn bot → điền token mới vào `.env` → `docker compose up -d`.
- App không gửi dữ liệu của bạn đi đâu — mọi thứ nằm trong Docker trên máy bạn. Google chỉ cung cấp tên, email, ảnh
  đại diện khi đăng nhập.

---

Mã nguồn và tài liệu kỹ thuật: <https://github.com/Son30-kins/workbench>
