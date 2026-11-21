<div align="center">

# hTeleport

**Plugin dịch chuyển tiện lợi, giàu tính năng dành cho máy chủ Minecraft.**

[![Modrinth](https://img.shields.io/badge/Modrinth-T%E1%BA%A3i%20plugin-1bd96a?style=for-the-badge&logo=modrinth&logoColor=white)](https://modrinth.com/plugin/hteleport)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.4%2B-45a049?style=for-the-badge&logo=minecraft&logoColor=white)](https://modrinth.com/plugin/hteleport)
[![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.java.com/)
[![License: MIT](https://img.shields.io/github/license/h2rhenry/hTeleport?style=for-the-badge&color=8b5cf6)](LICENSE)
[![Latest commit](https://img.shields.io/github/last-commit/h2rhenry/hTeleport?style=for-the-badge&color=6d28d9)](https://github.com/h2rhenry/hTeleport/commits/main)

[Modrinth](https://modrinth.com/plugin/hteleport) · [Tính năng](#tính-năng) · [Cài đặt](#cài-đặt) · [Lệnh](#lệnh) · [Build từ mã nguồn](#build-từ-mã-nguồn)

</div>

---

## Giới thiệu

**hTeleport** là plugin Minecraft tập trung vào các tiện ích dịch chuyển cho máy chủ Paper. Plugin cung cấp random teleport, quản lý home, yêu cầu dịch chuyển giữa người chơi, warp, spawn và quay lại vị trí trước đó.

## Tính năng

| Tính năng | Mô tả |
| --- | --- |
| **Random Teleport** | Dịch chuyển ngẫu nhiên; hỗ trợ tùy chỉnh theo thế giới và hạn chế vị trí nguy hiểm. |
| **Quản lý Home** | Tạo, xoá và dịch chuyển đến home; có giao diện GUI để quản lý. |
| **TPA** | Gửi, chấp nhận, từ chối hoặc huỷ yêu cầu dịch chuyển giữa người chơi. |
| **Warp & Spawn** | Dịch chuyển đến warp hoặc spawn của máy chủ. |
| **Back** | Trở về vị trí trước đó hoặc vị trí chết gần nhất. |
| **Cooldown & Delay** | Tùy chỉnh thời gian chờ và hồi chiêu cho các lệnh. |
| **Tuỳ chỉnh ngôn ngữ** | Tuỳ chỉnh nội dung tin nhắn trong cấu hình; mặc định hỗ trợ tiếng Việt. |

## Yêu cầu

- Máy chủ **Paper 1.21.4 trở lên** (xem các phiên bản tương thích mới nhất trên [Modrinth](https://modrinth.com/plugin/hteleport)).
- **Java 21**.

## Cài đặt

1. Tải phiên bản phù hợp từ [Modrinth – hTeleport](https://modrinth.com/plugin/hteleport).
2. Chép file `.jar` vào thư mục `plugins` của máy chủ.
3. Khởi động hoặc khởi động lại máy chủ.
4. Chỉnh các file cấu hình được plugin tạo trong `plugins/hTeleport/` theo nhu cầu.

## Lệnh

| Lệnh | Mô tả |
| --- | --- |
| `/rtp [world]` | Dịch chuyển ngẫu nhiên trong thế giới hiện tại hoặc thế giới chỉ định. |
| `/home [id]` | Mở giao diện Home hoặc dịch chuyển đến home. |
| `/sethome [id]` | Tạo home tại vị trí hiện tại. |
| `/delhome [id]` | Xoá home theo ID. |
| `/tpa <player>` | Gửi yêu cầu dịch chuyển đến người chơi. |
| `/tphere <player>` | Yêu cầu người chơi dịch chuyển đến chỗ bạn. |
| `/tpaccept` | Chấp nhận yêu cầu dịch chuyển. |
| `/tpdeny` | Từ chối yêu cầu dịch chuyển. |
| `/tpcancel` | Huỷ yêu cầu đang chờ. |
| `/spawn` | Dịch chuyển đến spawn. |
| `/warp <name>` | Dịch chuyển đến warp. |
| `/back` | Trở về vị trí trước đó hoặc điểm chết gần nhất. |

> Lệnh và tuỳ chọn có thể thay đổi theo phiên bản. Hãy xem thông tin phiên bản hiện hành trên [Modrinth](https://modrinth.com/plugin/hteleport).

## Quyền

- `hteleport.use` — quyền sử dụng cơ bản.
- `hteleport.admin` — quyền quản trị.

Hãy kiểm tra cấu hình và thông tin của phiên bản đang chạy để biết các quyền chi tiết nếu máy chủ cần phân quyền theo từng tính năng.

## Build từ mã nguồn

Dự án sử dụng Maven và yêu cầu JDK 21.

```bash
git clone https://github.com/h2rhenry/hTeleport.git
cd hTeleport
mvn package
```

File JAR sau khi build sẽ nằm trong thư mục `target/`.

## Báo lỗi và đề xuất

Nếu gặp lỗi hoặc muốn đề xuất tính năng, hãy [mở Issue](https://github.com/h2rhenry/hTeleport/issues). Khi báo lỗi, vui lòng cung cấp phiên bản hTeleport, phiên bản Minecraft và log liên quan.

## Giấy phép

hTeleport được phát hành theo giấy phép [MIT](LICENSE).

<div align="center">

Made with ♥ by **[h2rhenry](https://github.com/h2rhenry)**

</div>
