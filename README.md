# 📱 WhisperAndroidAPP – Ứng dụng Chuyển đổi Giọng nói Thành Văn bản

> **Đề tài NCKH sinh viên** – Nghiên cứu mô hình AI trong chuyển đổi giọng nói thành văn bản áp dụng cho sinh viên ngành CNTT trường ĐHSPKT Vinh.

---

## 📋 Mục lục

1. [Giới thiệu](#1-giới-thiệu)
2. [Yêu cầu hệ thống](#2-yêu-cầu-hệ-thống)
3. [Hướng dẫn cài đặt môi trường](#3-hướng-dẫn-cài-đặt-môi-trường)
   - [3.1 Cài đặt Git](#31-cài-đặt-git)
   - [3.2 Cài đặt Python](#32-cài-đặt-python)
   - [3.3 Cài đặt FFmpeg](#33-cài-đặt-ffmpeg)
   - [3.4 Cài đặt OpenSSH](#34-cài-đặt-openssh)
   - [3.5 Cài đặt OpenSSL](#35-cài-đặt-openssl)
   - [3.6 Cài đặt OpenAI Whisper](#36-cài-đặt-openai-whisper)
4. [Cấu hình và chạy Server](#4-cấu-hình-và-chạy-server)
5. [Mở ứng dụng Android trong Android Studio](#5-mở-ứng-dụng-android-trong-android-studio)
6. [Cấu trúc thư mục dự án](#6-cấu-trúc-thư-mục-dự-án)
7. [Các chức năng chính](#7-các-chức-năng-chính)
8. [Xử lý lỗi thường gặp](#8-xử-lý-lỗi-thường-gặp)
9. [Thông tin liên hệ](#9-thông-tin-liên-hệ)

---

## 1. Giới thiệu

**WhisperAndroidAPP** là ứng dụng Android tích hợp mô hình AI **OpenAI Whisper** để chuyển đổi giọng nói thành văn bản (Speech-to-Text). Hệ thống hoạt động theo kiến trúc **Client – Server**:

- **Client (Android)**: Ghi âm giọng nói, gửi file âm thanh đến server qua HTTPS, hiển thị kết quả văn bản.
- **Server (Python/FastAPI)**: Tiếp nhận file âm thanh, xử lý bằng mô hình Whisper, trả kết quả dạng JSON.

### Tính năng nổi bật
- ✅ Ghi âm trực tiếp từ micro thiết bị
- ✅ Nhập file âm thanh có sẵn (WAV, MP3, M4A, ...)
- ✅ Chuyển đổi giọng nói thành văn bản đa ngôn ngữ (tiếng Việt, Anh, ...)
- ✅ Text-to-Speech (đọc to văn bản)
- ✅ Dịch thuật đa ngôn ngữ
- ✅ Quản lý lịch sử chuyển đổi

---

## 2. Yêu cầu hệ thống

### Máy tính chạy Server (Windows 10/11)
| Thành phần | Yêu cầu tối thiểu | Khuyến nghị |
|---|---|---|
| **OS** | Windows 10 64-bit | Windows 11 |
| **RAM** | 4 GB | 8–16 GB |
| **Ổ cứng** | 10 GB trống | SSD 20 GB+ |
| **CPU** | 4 nhân | 8 nhân trở lên |
| **GPU** | Không bắt buộc | NVIDIA (hỗ trợ CUDA) |
| **Kết nối mạng** | LAN hoặc WiFi | LAN ổn định |

### Thiết bị di động (Android)
| Thành phần | Yêu cầu |
|---|---|
| **OS** | Android 8.0 (API 26) trở lên |
| **RAM** | 2 GB trở lên |
| **Kết nối mạng** | Cùng mạng LAN với server hoặc Internet |

---

## 3. Hướng dẫn cài đặt môi trường

> ⚠️ **Lưu ý**: Thực hiện đúng thứ tự các bước sau. Sau mỗi bước cài đặt, hãy kiểm tra lại bằng lệnh xác nhận.

### 3.1 Cài đặt Git

Git là công cụ quản lý mã nguồn. Dùng để tải mã nguồn Whisper và ứng dụng về máy.

**Bước 1**: Truy cập trang chính thức: [https://git-scm.com/download/win](https://git-scm.com/download/win)

**Bước 2**: Tải và chạy trình cài đặt. Chọn **"Next"** liên tục với tùy chọn mặc định, nhấn **"Install"**.

**Bước 3**: Kiểm tra sau cài đặt. Mở **Command Prompt (CMD)** và nhập:
```bash
git --version
```
✅ Kết quả thành công: `git version 2.xx.x.windows.x`

---

### 3.2 Cài đặt Python

Python là ngôn ngữ lập trình dùng để chạy server xử lý giọng nói với mô hình Whisper.

**Bước 1**: Truy cập trang chính thức: [https://www.python.org/downloads/](https://www.python.org/downloads/)

**Bước 2**: Tải phiên bản **Python 3.10** hoặc **3.11** (khuyến nghị, tránh dùng 3.12+ vì một số thư viện chưa tương thích).

**Bước 3**: Chạy trình cài đặt.

> ⚠️ **QUAN TRỌNG**: Tích chọn **"Add Python to PATH"** trước khi nhấn Install!

```
☐ Install launcher for all users (recommended)
☑ Add Python 3.xx to PATH   ← PHẢI TÍCH Ô NÀY
```

**Bước 4**: Nhấn **"Install Now"** và chờ cài đặt xong.

**Bước 5**: Kiểm tra sau cài đặt:
```bash
python --version
pip --version
```
✅ Kết quả thành công: `Python 3.11.x` và `pip xx.x.x`

---

### 3.3 Cài đặt FFmpeg

FFmpeg là công cụ xử lý âm thanh và video. Whisper dùng FFmpeg để chuyển đổi các định dạng âm thanh (MP3, M4A, WEBM...) sang WAV trước khi xử lý.

**Cách 1 – Dùng winget (nhanh nhất)**:

Mở CMD với quyền **Administrator** và chạy:
```bash
winget install --id=Gyan.FFmpeg -e
```

**Cách 2 – Tải thủ công**:

**Bước 1**: Truy cập [https://www.gyan.dev/ffmpeg/builds/](https://www.gyan.dev/ffmpeg/builds/)

**Bước 2**: Tải file `ffmpeg-release-essentials.zip`

**Bước 3**: Giải nén và đặt vào `C:\ffmpeg\`

**Bước 4**: Thêm FFmpeg vào biến môi trường PATH:
- Nhấn `Windows + R` → gõ `sysdm.cpl` → **OK**
- Chọn tab **Advanced** → **Environment Variables**
- Trong phần **System variables**, tìm **Path** → nhấn **Edit**
- Nhấn **New** → thêm đường dẫn: `C:\ffmpeg\bin`
- Nhấn **OK** để lưu

**Bước 5**: Mở CMD mới và kiểm tra:
```bash
ffmpeg -version
```
✅ Kết quả thành công: `ffmpeg version 7.x.x ...`

---

### 3.4 Cài đặt OpenSSH

OpenSSH dùng để kết nối và quản lý server từ xa (máy tính khác hoặc VPS) qua giao thức SSH an toàn.

**Cài đặt trên Windows 10/11**:

**Bước 1**: Mở **Settings** → **Apps** → **Optional Features**

**Bước 2**: Nhấn **"Add a feature"**

**Bước 3**: Tìm **"OpenSSH Client"** và **"OpenSSH Server"** → nhấn **Install**

**Hoặc dùng PowerShell (chạy với Administrator)**:
```powershell
Add-WindowsCapability -Online -Name OpenSSH.Client~~~~0.0.1.0
Add-WindowsCapability -Online -Name OpenSSH.Server~~~~0.0.1.0
```

**Bước 4**: Khởi động dịch vụ SSH:
```powershell
Start-Service sshd
Set-Service -Name sshd -StartupType 'Automatic'
```

**Bước 5**: Kiểm tra:
```bash
ssh -V
```
✅ Kết quả thành công: `OpenSSH_for_Windows_9.x`

---

### 3.5 Cài đặt OpenSSL

OpenSSL dùng để tạo chứng chỉ SSL tự ký (Self-Signed Certificate), giúp server chạy giao thức **HTTPS** – bắt buộc để ứng dụng Android có thể gửi dữ liệu an toàn.

**Bước 1**: Truy cập [https://slproweb.com/products/Win32OpenSSL.html](https://slproweb.com/products/Win32OpenSSL.html)

**Bước 2**: Tải **Win64 OpenSSL v3.x.x** (phiên bản đầy đủ, không phải Light)

**Bước 3**: Cài đặt vào thư mục mặc định `C:\Program Files\OpenSSL-Win64\`

**Bước 4**: Thêm OpenSSL vào PATH:
- Thêm `C:\Program Files\OpenSSL-Win64\bin` vào biến môi trường PATH (tương tự FFmpeg)

**Bước 5**: Kiểm tra:
```bash
openssl version
```
✅ Kết quả thành công: `OpenSSL 3.x.x`

**Bước 6**: Tạo chứng chỉ SSL tự ký (chạy trong thư mục Whisper):
```bash
# Tạo Private Key
openssl genrsa -out server.key 2048

# Tạo Certificate Signing Request (CSR)
openssl req -new -key server.key -out server.csr

# Tạo chứng chỉ SSL tự ký (hạn 365 ngày)
openssl x509 -req -days 365 -in server.csr -signkey server.key -out server.crt
```

---

### 3.6 Cài đặt OpenAI Whisper

Whisper là mô hình AI của OpenAI dùng để chuyển đổi giọng nói thành văn bản.

**Bước 1**: Mở CMD và di chuyển đến thư mục bạn muốn cài đặt:
```bash
cd C:\
mkdir whisper_project
cd whisper_project
```

**Bước 2**: Tải mã nguồn Whisper từ GitHub:
```bash
git clone https://github.com/openai/whisper.git
cd whisper
```

**Bước 3**: Cài đặt các thư viện Python cần thiết:
```bash
pip install -r requirements.txt
```

**Bước 4**: Cài đặt Whisper:
```bash
pip install openai-whisper
```

**Bước 5**: Cài đặt thêm các thư viện cho server:
```bash
pip install fastapi uvicorn python-multipart psutil pydantic
```

**Bước 6**: Kiểm tra Whisper đã cài đặt thành công:
```bash
whisper --help
```
✅ Kết quả thành công: Hiển thị danh sách các tùy chọn của lệnh whisper.

---

## 4. Cấu hình và chạy Server

### Bước 1: Chuẩn bị file server.py

Tạo file `server.py` trong thư mục Whisper với nội dung xử lý API. File này đã được cung cấp trong thư mục `whisper/` của dự án.

### Bước 2: Đảm bảo chứng chỉ SSL đã tạo
Kiểm tra thư mục whisper_project có đủ 2 file:
- `server.key` – Private Key
- `server.crt` – Chứng chỉ SSL

### Bước 3: Chạy server
```bash
cd C:\whisper_project\whisper
python server.py
```

✅ Server khởi động thành công khi hiển thị:
```
INFO:     Uvicorn running on https://0.0.0.0:8443
INFO:     Application startup complete.
```

### Bước 4: Mở cổng tường lửa

Để điện thoại Android kết nối được, cần mở cổng **TCP 8443** trên Windows Firewall:

```powershell
# Chạy PowerShell với quyền Administrator
New-NetFirewallRule -DisplayName "Whisper Server" -Direction Inbound -Protocol TCP -LocalPort 8443 -Action Allow
```

Hoặc thực hiện thủ công:
1. Tìm kiếm **"Windows Defender Firewall with Advanced Security"**
2. Chọn **Inbound Rules** → **New Rule**
3. Chọn **Port** → **TCP** → nhập **8443**
4. Chọn **Allow the connection** → đặt tên **"WHISPER"** → **Finish**

### Bước 5: Lấy địa chỉ IP máy tính
```bash
ipconfig
```
Ghi lại địa chỉ **IPv4 Address** (ví dụ: `192.168.1.100`) để cài vào ứng dụng Android.

---

## 5. Mở ứng dụng Android trong Android Studio

**Bước 1**: Tải và cài đặt **Android Studio** từ [https://developer.android.com/studio](https://developer.android.com/studio)

**Bước 2**: Clone repository về máy:
```bash
git clone <URL_repository_này>
cd WhisperAndroidAPP
```

**Bước 3**: Mở Android Studio → **File** → **Open** → chọn thư mục `WhisperAndroidAPP`

**Bước 4**: Chờ Gradle sync hoàn tất (lần đầu có thể mất 5–10 phút)

**Bước 5**: Cập nhật địa chỉ IP server trong file cấu hình của ứng dụng (thay `192.168.1.100` bằng IP của máy bạn)

**Bước 6**: Kết nối điện thoại Android với máy tính qua USB (bật chế độ **Developer Options** và **USB Debugging**)

**Bước 7**: Nhấn nút **Run ▶** trong Android Studio để build và cài ứng dụng lên điện thoại

---

## 6. Cấu trúc thư mục dự án

```
WhisperAndroidAPP/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/             # Mã nguồn Java/Kotlin
│   │   │   ├── res/              # Tài nguyên (layout XML, drawable, ...)
│   │   │   └── AndroidManifest.xml
│   │   └── test/                 # Unit tests
│   └── build.gradle.kts
├── build.gradle.kts
├── gradle/
├── settings.gradle.kts
├── whisper.rar                   # File nén mã nguồn server Whisper
└── README.md                     # File này
```

---

## 7. Các chức năng chính

| Chức năng | Mô tả |
|---|---|
| **Ghi âm trực tiếp** | Nhấn nút micro để ghi âm, tự động gửi và nhận kết quả văn bản |
| **Nhập file âm thanh** | Chọn file WAV/MP3/M4A từ bộ nhớ để chuyển đổi |
| **Chuyển đổi giọng nói → văn bản** | Whisper AI nhận diện và chuyển đổi đa ngôn ngữ |
| **Text-to-Speech** | Đọc to văn bản kết quả bằng giọng tổng hợp |
| **Dịch thuật** | Dịch văn bản sang nhiều ngôn ngữ khác nhau |
| **Quản lý lịch sử** | Xem, chỉnh sửa, xóa và xuất các kết quả đã chuyển đổi |
| **Chia sẻ văn bản** | Xuất và chia sẻ kết quả qua email, Zalo, WhatsApp, ... |

---

## 8. Xử lý lỗi thường gặp

### ❌ Lỗi: "Python was not found"
**Nguyên nhân**: Python chưa được thêm vào PATH.  
**Giải pháp**: Cài đặt lại Python và tích chọn **"Add Python to PATH"**.

---

### ❌ Lỗi: "ffmpeg is not recognized"
**Nguyên nhân**: FFmpeg chưa được thêm vào biến môi trường PATH.  
**Giải pháp**: Kiểm tra lại đường dẫn `C:\ffmpeg\bin` trong Environment Variables, sau đó **mở CMD mới** và thử lại.

---

### ❌ Lỗi: "Connection refused" trên Android
**Nguyên nhân**: Server chưa chạy hoặc cổng 8443 chưa mở, hoặc điện thoại không cùng mạng với máy tính.  
**Giải pháp**:
1. Kiểm tra server đã chạy: `python server.py`
2. Kiểm tra cổng 8443 đã mở trong Windows Firewall
3. Đảm bảo điện thoại kết nối cùng mạng WiFi với máy tính server

---

### ❌ Lỗi: "SSL Certificate Error" trên Android
**Nguyên nhân**: Chứng chỉ SSL tự ký không được Android tin tưởng mặc định.  
**Giải pháp**: Ứng dụng đã cấu hình bỏ qua SSL self-signed cho môi trường thử nghiệm. Nếu vẫn lỗi, kiểm tra file `server.crt` và `server.key` đã có trong thư mục server chưa.

---

### ❌ Lỗi: "CUDA out of memory"
**Nguyên nhân**: GPU không đủ VRAM để chạy model Whisper lớn.  
**Giải pháp**: Sử dụng model nhỏ hơn. Sửa trong `server.py`:
```python
# Thay "large" bằng "base" hoặc "small"
model = whisper.load_model("base")
```

---

### ❌ Lỗi: Gradle sync thất bại trong Android Studio
**Nguyên nhân**: Thiếu kết nối Internet hoặc phiên bản Android Studio không tương thích.  
**Giải pháp**:
1. Kiểm tra kết nối Internet
2. Cập nhật Android Studio lên phiên bản mới nhất
3. Nhấn **File** → **Invalidate Caches / Restart**

---

## 9. Thông tin liên hệ

| | Thông tin |
|---|---|
| **Đề tài** | Nghiên cứu mô hình AI trong chuyển đổi giọng nói thành văn bản |
| **Trường** | Đại học Sư phạm Kỹ thuật Vinh |
| **Chủ nhiệm** | Phan Thị Hoài |
| **Thành viên** | Nguyễn Tài Nguyên |
| **Lớp** | DHCTTCK17A2, Khóa 17, Khoa CNTT |
| **Giáo viên hướng dẫn** | Nguyễn Thị Quỳnh Vinh |

---

> 📌 **Lưu ý**: Đọc kỹ từng bước trong README trước khi bắt đầu cài đặt. Nếu gặp vấn đề không có trong mục xử lý lỗi, vui lòng liên hệ giáo viên hướng dẫn hoặc các thành viên nhóm.
