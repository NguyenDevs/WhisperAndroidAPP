<div align="center">

<img src="https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" />
<img src="https://img.shields.io/badge/Python-3776AB?style=for-the-badge&logo=python&logoColor=white" />
<img src="https://img.shields.io/badge/FastAPI-009688?style=for-the-badge&logo=fastapi&logoColor=white" />
<img src="https://img.shields.io/badge/OpenAI_Whisper-412991?style=for-the-badge&logo=openai&logoColor=white" />

# 🎙️ WhisperAndroidAPP

**Ứng dụng chuyển đổi giọng nói thành văn bản trên Android sử dụng mô hình AI OpenAI Whisper**

*Đề tài Nghiên cứu Khoa học Sinh viên — Trường Đại học Sư phạm Kỹ thuật Vinh*

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Android API](https://img.shields.io/badge/Android-API%2026%2B-brightgreen)](https://developer.android.com)
[![Python](https://img.shields.io/badge/Python-3.10%20|%203.11-blue)](https://python.org)
[![FastAPI](https://img.shields.io/badge/FastAPI-latest-teal)](https://fastapi.tiangolo.com)

</div>

---

## 📖 Mục lục

- [Giới thiệu](#-giới-thiệu)
- [Kiến trúc hệ thống](#-kiến-trúc-hệ-thống)
- [Tính năng](#-tính-năng)
- [Yêu cầu hệ thống](#-yêu-cầu-hệ-thống)
- [Hướng dẫn cài đặt](#-hướng-dẫn-cài-đặt)
  - [1. Git](#1-git)
  - [2. Python](#2-python)
  - [3. FFmpeg](#3-ffmpeg)
  - [4. OpenSSH](#4-openssh)
  - [5. OpenSSL & Chứng chỉ SSL](#5-openssl--chứng-chỉ-ssl)
  - [6. OpenAI Whisper & Dependencies](#6-openai-whisper--dependencies)
- [Cấu hình & Khởi động Server](#-cấu-hình--khởi-động-server)
- [Cài đặt ứng dụng Android](#-cài-đặt-ứng-dụng-android)
- [Cấu trúc dự án](#-cấu-trúc-dự-án)
- [Xử lý sự cố](#-xử-lý-sự-cố)
- [Nhóm thực hiện](#-nhóm-thực-hiện)

---

## 🧩 Giới thiệu

**WhisperAndroidAPP** là ứng dụng Android tích hợp mô hình AI **OpenAI Whisper** để thực hiện chuyển đổi giọng nói thành văn bản (Speech-to-Text) với độ chính xác cao, hỗ trợ đa ngôn ngữ bao gồm tiếng Việt.

Ứng dụng được phát triển như một đề tài nghiên cứu khoa học cấp sinh viên, hướng tới việc áp dụng các mô hình AI tiên tiến vào bài toán thực tế trong môi trường học thuật và giáo dục.

---

## 🏗️ Kiến trúc hệ thống

```
┌─────────────────────────────────────────────────────────┐
│                   Android Client                         │
│                                                         │
│   [Ghi âm / Chọn file]  ──►  [Gửi HTTPS Request]      │
│         ▲                                               │
│         └──────────── [Hiển thị kết quả]               │
└─────────────────────┬───────────────────────────────────┘
                      │  HTTPS (port 8443)
                      │  Multipart/form-data
                      ▼
┌─────────────────────────────────────────────────────────┐
│              Python FastAPI Server                       │
│                                                         │
│   [Nhận file âm thanh]  ──►  [Whisper Model]           │
│                                    │                    │
│   [Trả JSON kết quả]  ◄──  [Nhận diện giọng nói]       │
└─────────────────────────────────────────────────────────┘
```

| Thành phần | Công nghệ |
|---|---|
| **Android Client** | Java/Kotlin, Android SDK |
| **Backend Server** | Python, FastAPI, Uvicorn |
| **AI Model** | OpenAI Whisper |
| **Audio Processing** | FFmpeg |
| **Transport Security** | HTTPS / TLS (Self-signed cert) |

---

## ✨ Tính năng

| Tính năng | Mô tả |
|---|---|
| 🎤 **Ghi âm trực tiếp** | Thu âm từ micro thiết bị, tự động gửi và nhận kết quả |
| 📁 **Nhập file âm thanh** | Hỗ trợ WAV, MP3, M4A và nhiều định dạng khác |
| 🌐 **Đa ngôn ngữ** | Nhận diện tiếng Việt, tiếng Anh và hơn 90 ngôn ngữ |
| 🔊 **Text-to-Speech** | Đọc to nội dung văn bản kết quả |
| 🌍 **Dịch thuật** | Chuyển văn bản sang nhiều ngôn ngữ |
| 📋 **Quản lý lịch sử** | Xem, chỉnh sửa, xóa và xuất lịch sử chuyển đổi |
| 📤 **Chia sẻ nhanh** | Xuất văn bản qua Zalo, WhatsApp, Email, ... |

---

## 💻 Yêu cầu hệ thống

### Máy chủ (Windows 10/11)

| Thành phần | Tối thiểu | Khuyến nghị |
|---|---|---|
| **OS** | Windows 10 64-bit | Windows 11 |
| **RAM** | 4 GB | 8–16 GB |
| **Ổ cứng** | 10 GB trống | SSD ≥ 20 GB |
| **CPU** | 4 nhân | 8 nhân trở lên |
| **GPU** | *(không bắt buộc)* | NVIDIA hỗ trợ CUDA |
| **Mạng** | WiFi / LAN | LAN ổn định |

### Thiết bị Android

| Thành phần | Yêu cầu |
|---|---|
| **Android** | 8.0 (API 26) trở lên |
| **RAM** | ≥ 2 GB |
| **Mạng** | Cùng LAN với server hoặc có Internet |

---

## 🛠️ Hướng dẫn cài đặt

> **⚠️ Lưu ý:** Thực hiện đúng thứ tự. Sau mỗi bước, hãy chạy lệnh kiểm tra để xác nhận cài đặt thành công trước khi tiếp tục.

---

### 1. Git

Git dùng để tải mã nguồn từ repository.

**Tải về tại:** https://git-scm.com/download/win — chọn tùy chọn mặc định và nhấn **Install**.

**Kiểm tra:**
```bash
git --version
# ✅ Mong đợi: git version 2.xx.x.windows.x
```

---

### 2. Python

Server Whisper chạy bằng Python. Khuyến nghị dùng **Python 3.10** hoặc **3.11** — tránh 3.12+ vì một số thư viện chưa tương thích.

**Tải về tại:** https://www.python.org/downloads/

> **⚠️ Quan trọng:** Trong màn hình cài đặt, **bắt buộc phải tích ô** `☑ Add Python to PATH` trước khi nhấn Install.

**Kiểm tra:**
```bash
python --version
pip --version
# ✅ Mong đợi: Python 3.11.x | pip xx.x.x
```

---

### 3. FFmpeg

FFmpeg chuyển đổi định dạng âm thanh (MP3, M4A, WEBM...) sang WAV trước khi Whisper xử lý.

**Cách 1 — Dùng winget (nhanh nhất):**
```bash
# Chạy CMD với quyền Administrator
winget install --id=Gyan.FFmpeg -e
```

**Cách 2 — Cài thủ công:**

1. Tải `ffmpeg-release-essentials.zip` tại: https://www.gyan.dev/ffmpeg/builds/
2. Giải nén vào `C:\ffmpeg\`
3. Thêm `C:\ffmpeg\bin` vào biến môi trường **PATH**:
   - `Win + R` → `sysdm.cpl` → tab **Advanced** → **Environment Variables**
   - Trong **System variables** → tìm `Path` → **Edit** → **New** → nhập `C:\ffmpeg\bin`

**Kiểm tra:**
```bash
ffmpeg -version
# ✅ Mong đợi: ffmpeg version 7.x.x ...
```

---

### 4. OpenSSH

OpenSSH cho phép kết nối và quản lý server từ xa qua giao thức SSH.

**Cài qua PowerShell (chạy với quyền Administrator):**
```powershell
Add-WindowsCapability -Online -Name OpenSSH.Client~~~~0.0.1.0
Add-WindowsCapability -Online -Name OpenSSH.Server~~~~0.0.1.0

# Khởi động và đặt chế độ tự động
Start-Service sshd
Set-Service -Name sshd -StartupType 'Automatic'
```

**Kiểm tra:**
```bash
ssh -V
# ✅ Mong đợi: OpenSSH_for_Windows_9.x
```

---

### 5. OpenSSL & Chứng chỉ SSL

OpenSSL tạo chứng chỉ SSL tự ký để server chạy **HTTPS** — bắt buộc để Android giao tiếp an toàn với server.

**Tải về tại:** https://slproweb.com/products/Win32OpenSSL.html

Chọn **Win64 OpenSSL v3.x.x** (bản đầy đủ, không phải Light). Cài vào `C:\Program Files\OpenSSL-Win64\` và thêm `...\bin` vào PATH (tương tự FFmpeg).

**Kiểm tra:**
```bash
openssl version
# ✅ Mong đợi: OpenSSL 3.x.x
```

**Tạo chứng chỉ SSL tự ký** (chạy trong thư mục dự án server):
```bash
# Bước 1: Tạo Private Key
openssl genrsa -out server.key 2048

# Bước 2: Tạo Certificate Signing Request
openssl req -new -key server.key -out server.csr

# Bước 3: Ký chứng chỉ (hiệu lực 365 ngày)
openssl x509 -req -days 365 -in server.csr -signkey server.key -out server.crt
```

Sau bước này, thư mục server phải có đủ `server.key` và `server.crt`.

---

### 6. OpenAI Whisper & Dependencies

```bash
# Tạo thư mục dự án
cd C:\
mkdir whisper_project && cd whisper_project

# Clone mã nguồn Whisper
git clone https://github.com/openai/whisper.git
cd whisper

# Cài đặt Whisper và dependencies
pip install -r requirements.txt
pip install openai-whisper

# Cài đặt thư viện server
pip install fastapi uvicorn python-multipart psutil pydantic
```

**Kiểm tra:**
```bash
whisper --help
# ✅ Mong đợi: Hiển thị danh sách các tùy chọn lệnh whisper
```

---

## 🚀 Cấu hình & Khởi động Server

### Bước 1 — Kiểm tra cấu trúc thư mục

Đảm bảo thư mục `C:\whisper_project\whisper\` có đủ các file:
```
whisper/
├── server.py       # File server FastAPI
├── server.key      # Private Key SSL
└── server.crt      # Chứng chỉ SSL
```

### Bước 2 — Khởi động server

```bash
cd C:\whisper_project\whisper
python server.py
```

Server khởi động thành công khi hiển thị:
```
INFO:     Uvicorn running on https://0.0.0.0:8443
INFO:     Application startup complete.
```

### Bước 3 — Mở cổng tường lửa

Để Android kết nối được, cần mở cổng TCP **8443**:

```powershell
# PowerShell với quyền Administrator
New-NetFirewallRule -DisplayName "Whisper Server" -Direction Inbound -Protocol TCP -LocalPort 8443 -Action Allow
```

Hoặc thủ công: **Windows Defender Firewall** → **Inbound Rules** → **New Rule** → **Port** → **TCP 8443** → **Allow** → đặt tên `WHISPER`.

### Bước 4 — Lấy địa chỉ IP máy chủ

```bash
ipconfig
```

Ghi lại **IPv4 Address** (ví dụ: `192.168.1.100`) để cài vào ứng dụng Android.

---

## 📱 Cài đặt ứng dụng Android

**Bước 1** — Tải và cài đặt **Android Studio**: https://developer.android.com/studio

**Bước 2** — Clone repository:
```bash
git clone <URL_REPOSITORY>
cd WhisperAndroidAPP
```

**Bước 3** — Mở dự án trong Android Studio:
**File** → **Open** → chọn thư mục `WhisperAndroidAPP`

**Bước 4** — Chờ Gradle sync hoàn tất *(lần đầu có thể mất 5–10 phút)*

**Bước 5** — Cập nhật địa chỉ IP server trong file cấu hình của ứng dụng (thay `192.168.1.100` bằng IP thực của máy server)

**Bước 6** — Kết nối điện thoại qua USB:
- Bật **Developer Options** trên điện thoại
- Bật **USB Debugging**

**Bước 7** — Nhấn **Run ▶** để build và cài ứng dụng lên thiết bị

---

## 📁 Cấu trúc dự án

```
WhisperAndroidAPP/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/               # Mã nguồn Java/Kotlin
│   │   │   ├── res/                # Tài nguyên UI (layout, drawable, ...)
│   │   │   └── AndroidManifest.xml
│   │   └── test/                   # Unit tests
│   └── build.gradle.kts
├── build.gradle.kts
├── gradle/
├── settings.gradle.kts
├── whisper.rar                     # Mã nguồn server Whisper (nén)
└── README.md
```

---

## 🔧 Xử lý sự cố

<details>
<summary><b>❌ "Python was not found"</b></summary>

**Nguyên nhân:** Python chưa được thêm vào PATH.

**Giải pháp:** Cài đặt lại Python và đảm bảo tích chọn **"Add Python to PATH"** trong màn hình cài đặt.

</details>

<details>
<summary><b>❌ "ffmpeg is not recognized"</b></summary>

**Nguyên nhân:** FFmpeg chưa được thêm vào biến môi trường PATH, hoặc CMD chưa được mở lại sau khi thêm.

**Giải pháp:** Kiểm tra lại `C:\ffmpeg\bin` trong Environment Variables, sau đó **đóng và mở lại CMD**.

</details>

<details>
<summary><b>❌ "Connection refused" trên Android</b></summary>

**Nguyên nhân:** Server chưa chạy, cổng 8443 chưa mở, hoặc điện thoại không cùng mạng với máy tính.

**Kiểm tra lần lượt:**
1. Server đang chạy: `python server.py`
2. Cổng 8443 đã được mở trong Windows Firewall
3. Điện thoại và máy tính kết nối cùng một mạng WiFi / LAN

</details>

<details>
<summary><b>❌ "SSL Certificate Error" trên Android</b></summary>

**Nguyên nhân:** Chứng chỉ tự ký (self-signed) không được Android tin tưởng theo mặc định.

**Giải pháp:** Ứng dụng đã được cấu hình bỏ qua lỗi SSL cho môi trường thử nghiệm. Nếu vẫn lỗi, kiểm tra `server.crt` và `server.key` đã tồn tại trong thư mục server.

</details>

<details>
<summary><b>❌ "CUDA out of memory"</b></summary>

**Nguyên nhân:** GPU không đủ VRAM để chạy model Whisper lớn.

**Giải pháp:** Chuyển sang model nhỏ hơn trong `server.py`:

```python
# Thay "large" bằng "base" hoặc "small"
model = whisper.load_model("base")
```

| Model | VRAM yêu cầu | Ghi chú |
|---|---|---|
| `tiny` | ~1 GB | Nhanh nhất, độ chính xác thấp hơn |
| `base` | ~1 GB | Cân bằng tốt |
| `small` | ~2 GB | Khuyến nghị |
| `medium` | ~5 GB | Chính xác cao |
| `large` | ~10 GB | Chính xác nhất |

</details>

<details>
<summary><b>❌ Gradle sync thất bại trong Android Studio</b></summary>

**Nguyên nhân:** Mất kết nối Internet hoặc phiên bản Android Studio cũ.

**Giải pháp:**
1. Kiểm tra kết nối Internet
2. Cập nhật Android Studio lên phiên bản mới nhất
3. **File** → **Invalidate Caches / Restart**

</details>

---

## 👥 Nhóm thực hiện

| | |
|---|---|
| **Đề tài** | Nghiên cứu mô hình AI trong chuyển đổi giọng nói thành văn bản |
| **Đơn vị** | Đại học Sư phạm Kỹ thuật Vinh — Khoa Công nghệ Thông tin |
| **Lớp / Khóa** | DHCTTCK17A2, Khóa 17 |
| **Chủ nhiệm đề tài** | Phan Thị Hoài |
| **Thành viên** | Nguyễn Tài Nguyên |
| **Giáo viên hướng dẫn** | Nguyễn Thị Quỳnh Vinh |

---

<div align="center">

*Đọc kỹ README trước khi cài đặt. Nếu gặp vấn đề nằm ngoài phần xử lý sự cố, vui lòng liên hệ giáo viên hướng dẫn hoặc các thành viên nhóm.*

</div>
