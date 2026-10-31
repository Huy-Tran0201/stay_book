# PROJECT.md – StayBook (Hệ thống đặt phòng khách sạn/nhà nghỉ nhỏ)

Đây là nguồn sự thật cho agent. Nếu thiếu hoặc mâu thuẫn, HỎI lại, không tự đoán.
Bản đầy đủ hơn nằm trong tài liệu SRS của sinh viên (nếu có `docs/SRS.md` thì dùng làm tham chiếu thêm).

## 1. Tổng quan

- Phục vụ MỘT cơ sở lưu trú. Ba vai trò: `GUEST` (khách), `STAFF` (lễ tân), `ADMIN` (chủ cơ sở; có mọi quyền của STAFF).
- Vòng đời: Cấu hình phòng và giá → Khách tìm phòng trống → Đặt phòng và giữ chỗ → Chuyển cọc (VietQR) → Lễ tân xác nhận → Nhận phòng → Trả phòng và thanh toán → Báo cáo.
- Khách xem phòng trống KHÔNG cần đăng nhập; đặt phòng thì cần đăng nhập. Lễ tân có thể đặt hộ khách không có tài khoản.
- Một đặt phòng = một phòng cụ thể, khoảng ở `[ngày nhận, ngày trả)`. Hệ thống tự gán phòng khi đặt; lễ tân có thể đổi sang phòng cùng loại.

## 2. Công nghệ và quy ước

- Java 25, Spring Boot 3.5.16 (bản 3.x cuối cùng, được chọn vì tài liệu và ví dụ phổ biến; bản này đã hết hỗ trợ bảo mật miễn phí từ 30/06/2026, chấp nhận cho đồ án, nâng lên 4.x nếu triển khai thật), Maven. Spring Web MVC, Thymeleaf (+ `thymeleaf-extras-springsecurity6`), Spring Data JPA (Hibernate), Spring Security, Bean Validation, Spring Mail, Spring Scheduling.
- CSDL: MySQL 8 (chạy thật, profile `dev`), H2 (profile `test`). `spring.jpa.hibernate.ddl-auto=update` cho dev.
- Giao diện: Bootstrap 5 và Chart.js qua WebJars (chạy được khi không có mạng). Không React/Vue. Không Lombok.
- Tên trong code: tiếng Anh. Chuỗi hiển thị: tiếng Việt trong `messages.properties`.
- Tiền: `long` đồng VND. Ngày: `LocalDate`. Thời điểm: `LocalDateTime` theo múi giờ `Asia/Ho_Chi_Minh`; luôn lấy "bây giờ" từ bean `Clock`.
- Cấu hình bí mật qua biến môi trường: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `UPLOAD_DIR`.

## 3. Cấu trúc package (theo tính năng)

```
com.staybook
├─ StaybookApplication
├─ config        SecurityConfig, ClockConfig, WebConfig, SchedulingConfig
├─ common        exception (GlobalExceptionHandler, BusinessException), util (MoneyFormat, DateRange)
├─ user          User, Role, UserStatus, PasswordResetToken, UserService, AuthController, ProfileController
├─ room          RoomType, Room, RoomBlock, SeasonPrice, Amenity, RoomTypeImage, services, admin controllers, ImageStorageService
├─ pricing       PricingService, NightPrice
├─ booking       Booking, BookingStatus, BookingNight, BookingEvent, AvailabilityService, BookingService,
│                BookingStateMachine, CancellationPolicy, guest + staff controllers
├─ payment       Payment, PaymentType, PaymentMethod, Surcharge, PaymentService, InvoiceController
├─ notification  EmailService, email templates
├─ review        Review, ReviewService
├─ report        ReportService, ReportController
├─ audit         AuditLog, AuditService
├─ setting       Setting, SettingService (đọc/ghi cấu hình)
└─ scheduler     BookingExpiryJob, CheckInReminderJob
resources/templates: layout/, fragments/ (status-badge, price-table, flash), public/, guest/, staff/, admin/, error/
resources/messages.properties (tiếng Việt)
```

## 4. Mô hình dữ liệu

Enums:
- `Role`: GUEST, STAFF, ADMIN. `UserStatus`: ACTIVE, TEMP_LOCKED, LOCKED.
- `RoomStatus`: ACTIVE, MAINTENANCE, INACTIVE. `RoomTypeStatus`: ACTIVE, INACTIVE.
- `BookingStatus`: PENDING_DEPOSIT, AWAITING_CONFIRMATION, CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED, EXPIRED, NO_SHOW.
- `BookingSource`: GUEST_ONLINE, STAFF_ASSISTED. `CancelCause`: GUEST_REQUEST, FACILITY_CANCEL, DEPOSIT_REJECTED.
- `PaymentType`: DEPOSIT, EXTRA_PAYMENT, REFUND, ADJUSTMENT. `PaymentMethod`: BANK_TRANSFER, CASH.

Thực thể (id là `Long` tự tăng):
- `User`: fullName, email (duy nhất, lưu chữ thường), phone, passwordHash, role, status, failedLoginCount, lockedUntil, createdAt.
- `PasswordResetToken`: user, tokenHash, expiresAt, usedAt.
- `RoomType`: name, description, maxGuests (1–10), areaM2, bedType, basePrice (long, >0), weekendSurchargePercent (int, mặc định 10), status; nhiều `Amenity`; nhiều `RoomTypeImage` (fileName, sortOrder, tối đa 5).
- `Room`: roomNumber (duy nhất), floor, roomType, status.
- `RoomBlock`: room, fromDate, toDate (gồm cả hai đầu), reason, createdBy. Không chồng nhau trong cùng phòng.
- `SeasonPrice`: roomType, name, fromDate, toDate (gồm cả hai đầu), basePrice. Không chồng nhau trong cùng loại phòng.
- `Booking`: code (duy nhất, `BK-yyMMdd-XXXX`), user (rỗng nếu đặt hộ), guestName, guestPhone, guestEmail, roomType, room, checkInDate, checkOutDate, guestCount, totalRoomAmount, depositAmount, status, holdExpiresAt, note (≤500), source, cancelCause, cancelReason, cancelledAt, refundEligibleAmount, refundCheckNeeded (boolean), idDocumentNote (loại giấy tờ + 4 số cuối, tùy chọn), checkedInAt, checkedOutAt, reminderSentAt, createdAt, createdBy.
- `BookingNight`: booking, date, price (giá đêm đã chốt khi đặt).
- `BookingEvent`: booking, fromStatus, toStatus, actor (tên hoặc "SYSTEM"), reason, at.
- `Surcharge`: booking, name, amount (>0), recordedBy, recordedAt.
- `Payment` (chỉ thêm, KHÔNG sửa/xóa): booking, type, amount (long; chỉ ADJUSTMENT được âm), method, note, recordedBy, recordedAt.
- `Review`: booking (duy nhất), user, roomType, stars (1–5), comment (≤1000), hidden, createdAt.
- `Setting` (khóa–giá trị): `deposit.percent=30`, `hold.minutes=30`, `refund.hours=48`, `checkin.time=14:00`, `checkout.time=12:00`, `max.pending.per.guest=3`, `hotel.name`, `hotel.address`, `hotel.phone`, `bank.name`, `bank.account`, `bank.holder`.
- `AuditLog`: user, action, targetType, targetId, detail, at.

Chỉ mục gợi ý: `booking(room_id, check_in_date, check_out_date, status)`, `booking(status, hold_expires_at)`, `booking(code)`.

## 5. Quy tắc nghiệp vụ

- BR-01: ngày nhận không ở quá khứ; ngày trả sau ngày nhận; 1 đến 30 đêm.
- BR-02: giờ nhận 14:00, trả 12:00 (chỉ hiển thị và tính hạn hủy, không tính tiền theo giờ).
- BR-03: số khách (người lớn + trẻ em) ≤ `maxGuests` của loại phòng.
- BR-04: hai đặt phòng "đang giữ chỗ" không được chồng đêm trên cùng một phòng. Trả ngày D và nhận ngày D là hợp lệ.
- BR-05: các trạng thái giữ chỗ: PENDING_DEPOSIT, AWAITING_CONFIRMATION, CONFIRMED, CHECKED_IN. Các trạng thái khác giải phóng phòng.
- BR-06: giữ chỗ khi PENDING_DEPOSIT = `hold.minutes` (30 phút).
- BR-07: cọc = tổng tiền phòng × `deposit.percent`%, làm tròn LÊN đến 1.000 đồng.
- BR-08/09: giá một đêm = giá nền + phụ thu cuối tuần. Giá nền = giá mùa nếu đêm thuộc khoảng mùa (gồm hai đầu), ngược lại = `basePrice`. Phụ thu cuối tuần (đêm Thứ 6 và Thứ 7) = giá nền × `weekendSurchargePercent` / 100, làm tròn đến đồng.
- BR-11: hủy theo chính sách: xem 5.5.
- BR-12: chỉ nhận phòng khi booking ở CONFIRMED (đã có cọc được xác nhận hoặc lễ tân đã thu đủ cọc tại quầy).
- BR-13: chỉ nhận phòng từ ngày nhận trở đi; chỉ trả phòng sau khi đã nhận phòng; trả phòng sớm không hoàn tiền đêm chưa ở; đặt phòng đã NO_SHOW không nhận lại được.
- BR-14: phòng MAINTENANCE/INACTIVE hoặc đang bị khóa theo ngày thì không được gán trong khoảng bị khóa.
- BR-16: giá từng đêm được chốt trong `BookingNight`; đổi bảng giá sau đó không ảnh hưởng đặt phòng cũ.
- BR-17: sổ thanh toán chỉ thêm, không sửa/xóa; sai thì thêm khoản ADJUSTMENT có ghi chú.
- BR-18: booking ở AWAITING_CONFIRMATION quá 24 giờ chưa xử lý → tô cảnh báo trên bảng việc, KHÔNG tự hủy.
- BR-19: mật khẩu ≥ 8 ký tự; sai 5 lần khóa tạm 15 phút; liên kết đặt lại mật khẩu hết hạn sau 30 phút, dùng một lần.
- BR-20: khách chỉ xem/hủy đặt phòng của mình; STAFF xử lý mọi đặt phòng; chỉ ADMIN cấu hình phòng, giá, chính sách, nhân viên, báo cáo.
- BR-21: loại phòng/phòng đã có lịch sử không xóa cứng, chuyển INACTIVE.
- BR-22: chỉ đánh giá khi CHECKED_OUT, mỗi booking một lần.
- BR-23: ảnh JPG/PNG, ≤ 2 MB, tối đa 5 ảnh mỗi loại phòng.
- BR-24: mỗi khách tối đa `max.pending.per.guest` (3) booking ở PENDING_DEPOSIT hoặc AWAITING_CONFIRMATION cùng lúc.
- BR-25: giá, cọc, số tiền hoàn, trạng thái, quyền luôn tính lại ở máy chủ, không tin giá trị trình duyệt gửi.
- BR-26: khi nhận phòng chỉ lưu loại giấy tờ và 4 số cuối, không lưu toàn bộ số hoặc ảnh giấy tờ.

### 5.1 Chồng lấn
`overlap(a, b) = a.checkIn < b.checkOut && b.checkIn < a.checkOut`.

### 5.2 Phòng trống
Một phòng trống cho `[in, out)` khi: `status = ACTIVE`; không có `Booking` ở trạng thái giữ chỗ (BR-05) chồng lấn; không có `RoomBlock` nào có `fromDate < out && toDate >= in`. Loại phòng xuất hiện trong kết quả tìm kiếm khi `status = ACTIVE`, `maxGuests >= guests` và có ít nhất một phòng trống.

### 5.3 Tính giá
Với mỗi đêm `d` từ `checkIn` đến `checkOut - 1`: tính theo BR-08/09. `totalRoomAmount` = tổng các đêm. Đặt phần này thành `PricingService` thuần (không phụ thuộc web) để unit test.
Ví dụ: Deluxe 800.000 ₫, phụ thu 10%, nhận Thứ 6 06/11/2026, trả Thứ 2 09/11/2026 → 880.000 + 880.000 + 800.000 = 2.560.000 ₫; cọc 768.000 ₫.
Ví dụ mùa: mùa 20/12–05/01 giá nền 1.000.000 ₫; nhận Thứ 7 19/12/2026, trả Thứ 3 22/12/2026 → 880.000 + 1.000.000 + 1.000.000 = 2.880.000 ₫.

### 5.4 Tạo đặt phòng (trong MỘT `@Transactional`)
1. Kiểm tra BR-01, BR-03, BR-24.
2. Khóa bản ghi `RoomType` bằng `PESSIMISTIC_WRITE` (để các lần đặt cùng loại được xử lý lần lượt).
3. Tìm các phòng trống (5.2), chọn phòng có `roomNumber` nhỏ nhất. Không còn → ném `BusinessException` với thông báo "Loại phòng này vừa hết phòng…".
4. Tính giá (5.3), tạo `BookingNight`, tính cọc (BR-07), `holdExpiresAt = now + hold.minutes`, sinh `code`, trạng thái PENDING_DEPOSIT, ghi `BookingEvent`.

### 5.5 Hủy và hoàn cọc
`cancelAt` là thời điểm hủy; `checkInAt = checkInDate lúc 14:00`.
- Booking ở PENDING_DEPOSIT (chưa có cọc): `refundEligibleAmount = 0`, không phát sinh tiền.
- Hủy do khách: nếu `cancelAt <= checkInAt - refund.hours` (48 giờ) → `refundEligibleAmount` = tổng DEPOSIT đã ghi; ngược lại 0.
- Hủy do cơ sở (FACILITY_CANCEL): hoàn 100% DEPOSIT đã ghi.
- NO_SHOW: không hoàn (0). Các đêm còn lại trở lại trống.
- Khách hủy khi AWAITING_CONFIRMATION: chuyển CANCELLED, đặt `refundCheckNeeded = true` để lễ tân đối chiếu; hoàn thủ công nếu đã nhận tiền.
- `refundDue = max(0, refundEligibleAmount - tổng REFUND đã ghi)`. Hoàn tiền thực hiện thủ công, hệ thống chỉ ghi `Payment(REFUND)`.

### 5.6 Số liệu thanh toán
- `paid = Σ DEPOSIT + Σ EXTRA_PAYMENT + Σ ADJUSTMENT − Σ REFUND`.
- `totalDue = totalRoomAmount + Σ Surcharge`. `remaining = totalDue − paid`.
- Trả phòng chỉ hoàn tất khi `remaining == 0`.

### 5.7 Báo cáo
- Công suất ngày D = số phòng có khách đêm D (booking CONFIRMED, CHECKED_IN, CHECKED_OUT) / số phòng khả dụng đêm D (ACTIVE và không bị khóa).
- Doanh thu theo ngày/tháng = tổng `Payment` theo `recordedAt`: DEPOSIT + EXTRA_PAYMENT + ADJUSTMENT − REFUND.

## 6. Máy trạng thái đặt phòng

| Từ | Sang | Ai được thực hiện | Điều kiện |
| --- | --- | --- | --- |
| PENDING_DEPOSIT | AWAITING_CONFIRMATION | Khách (chủ booking) | chưa quá `holdExpiresAt` |
| PENDING_DEPOSIT | CONFIRMED | STAFF | đã ghi nhận thu đủ cọc tại quầy |
| PENDING_DEPOSIT | CANCELLED | Khách (chủ), STAFF | – |
| PENDING_DEPOSIT | EXPIRED | SYSTEM | quá `holdExpiresAt` và vẫn PENDING_DEPOSIT |
| AWAITING_CONFIRMATION | CONFIRMED | STAFF | số tiền nhận ≥ cọc |
| AWAITING_CONFIRMATION | CANCELLED | STAFF (từ chối), Khách (chủ) | – |
| CONFIRMED | CHECKED_IN | STAFF | hôm nay ≥ ngày nhận |
| CONFIRMED | CANCELLED | Khách (chủ), STAFF | – |
| CONFIRMED | NO_SHOW | STAFF | từ 00:00 ngày kế tiếp ngày nhận |
| CHECKED_IN | CHECKED_OUT | STAFF | `remaining == 0` |

CANCELLED, EXPIRED, NO_SHOW, CHECKED_OUT là trạng thái kết thúc. Mọi chuyển đổi khác bị từ chối. Việc chuyển phải kiểm tra trạng thái hiện tại trong cùng giao dịch (để an toàn khi hai thao tác xảy ra cùng lúc, ví dụ khách bấm "đã chuyển khoản" đúng lúc hết hạn). Mỗi chuyển ghi một `BookingEvent`.

## 7. Chức năng và trang theo module

Công khai: `GET /` (trang chủ + form tìm phòng), `GET /rooms?checkIn&checkOut&guests` (kết quả; lọc giá/tiện nghi/sức chứa, sắp xếp), `GET /room-types/{id}` (chi tiết), `GET /login`, `GET/POST /register`, `GET/POST /forgot-password`, `GET/POST /reset-password`.

Khách (đăng nhập): `GET /booking/new` (bước Thông tin: xem lại giá từng đêm, tổng, cọc, chính sách hủy; cảnh báo và tích xác nhận nếu ngày nhận cách thời điểm đặt dưới 48 giờ), `POST /booking` (tạo), `GET /booking/{code}/payment` (mã VietQR, thông tin tài khoản, đồng hồ đếm ngược), `POST /booking/{code}/paid-notice` ("Tôi đã chuyển khoản"), `GET /my-bookings`, `GET /my-bookings/{code}`, `POST /my-bookings/{code}/cancel` (hiển thị trước số tiền hoàn), `POST /my-bookings/{code}/review`, `GET/POST /profile`.
- VietQR: hiển thị ảnh QR (số tiền + nội dung chuyển khoản = mã đặt phòng) nếu tạo được; nếu không thì hiển thị số tài khoản và nội dung bằng chữ. Số phòng cụ thể chỉ hiện cho khách khi booking đã CONFIRMED.

Lễ tân (`/staff/**`): `GET /staff/today` (nhận hôm nay, trả hôm nay, đang ở, chờ xác nhận cọc có cảnh báo BR-18, chờ cọc sắp hết hạn), `GET /staff/bookings` (lọc + tìm theo mã/tên/SĐT, phân trang 20), `GET /staff/bookings/{code}`, `POST …/confirm-deposit` (nhập số tiền thực nhận ≥ cọc, hình thức), `POST …/reject-deposit`, `POST …/check-in`, `POST …/surcharges`, `POST …/payments`, `POST …/check-out`, `POST …/cancel` (nguyên nhân: khách yêu cầu / cơ sở hủy), `POST …/no-show`, `POST …/change-room` (chỉ phòng cùng loại, còn trống các đêm còn lại), `GET/POST /staff/bookings/new` (đặt hộ, có thể thu cọc ngay), `GET /staff/room-grid?from=` (hàng = phòng, cột = 14 ngày; ô tô màu: trống / giữ chỗ / đã xác nhận / đang ở / khóa-bảo trì; bấm ô mở chi tiết), `GET /staff/bookings/{code}/invoice` (in được; chỉ khi CHECKED_OUT).

Quản trị (`/admin/**`): `/admin/room-types` (CRUD + ảnh + tiện nghi), `/admin/rooms`, `/admin/room-blocks`, `/admin/seasons`, `/admin/settings` (chính sách + thông tin cơ sở + tài khoản nhận cọc), `/admin/users` (tạo nhân viên, khóa/mở khóa, đặt lại mật khẩu; không tự khóa/hạ quyền mình), `/admin/reports` (công suất, doanh thu, thống kê, biểu đồ Chart.js), `/admin/audit`.

Tác vụ nền: `BookingExpiryJob` chạy mỗi phút (`fixedDelay`): chuyển PENDING_DEPOSIT quá hạn → EXPIRED, an toàn khi chạy lặp. `CheckInReminderJob` chạy 09:00 mỗi ngày (cron, zone `Asia/Ho_Chi_Minh`): email nhắc cho booking CONFIRMED có ngày nhận là ngày mai và `reminderSentAt` còn trống.

Email (thử lại 3 lần, cách 5 phút; lỗi không làm hỏng nghiệp vụ; chưa cấu hình SMTP thì ghi ra log): đã nhận đặt phòng, đã xác nhận cọc, đã hủy (kèm số tiền hoàn), hết hạn giữ chỗ, nhắc trước ngày nhận.

## 8. Bảo mật

Xem `.github/copilot-instructions.md`. Tóm tắt thêm: kiểm tra chủ sở hữu ở service; CSRF bật; XSS dùng `th:text`; upload kiểm tra nội dung tệp; reset password token chỉ lưu băm; khóa tạm đăng nhập; giới hạn BR-24; ghi `AuditLog` cho thao tác nhạy cảm (đăng nhập sai, xác nhận/từ chối cọc, hủy, đổi phòng, đổi giá/chính sách, ghi nhận thanh toán, tạo/khóa tài khoản).

## 9. UX

- Thông báo lỗi tiếng Việt, ví dụ: "Email đã được sử dụng.", "Email hoặc mật khẩu không chính xác.", "Ngày trả phải sau ngày nhận.", "Ngày nhận không được ở quá khứ.", "Tối đa 30 đêm cho mỗi lần đặt.", "Không còn phòng phù hợp trong các ngày bạn chọn.", "Loại phòng này vừa hết phòng trong các ngày bạn chọn. Vui lòng chọn ngày hoặc loại phòng khác.", "Số tiền nhận chưa đủ tiền cọc.", "Chưa đến ngày nhận phòng.", "Đặt phòng chưa được xác nhận cọc.", "Còn phải thu … ₫.".
- Huy hiệu trạng thái (một fragment duy nhất): PENDING_DEPOSIT "Chờ cọc" (vàng), AWAITING_CONFIRMATION "Chờ xác nhận cọc" (cam), CONFIRMED "Đã xác nhận" (xanh dương), CHECKED_IN "Đang ở" (xanh lá), CHECKED_OUT "Đã trả phòng" (xám), CANCELLED "Đã hủy" (đỏ), EXPIRED "Hết hạn" (xám), NO_SHOW "Không đến" (đỏ đậm).
- Form tìm phòng: ngày nhận mặc định hôm nay, ngày trả mặc định hôm sau, `min` hợp lệ cho ô chọn ngày. Kết quả hiện tổng tiền cả kỳ ở và giá trung bình mỗi đêm.
- Khi chuyển tới đăng nhập từ bước đặt phòng, sau đăng nhập quay lại đúng lựa chọn cũ (chỉ đường dẫn nội bộ).
- Trang lễ tân ưu tiên bảng việc "Hôm nay", nút lớn, ít bước.

## 10. Dữ liệu mẫu (chỉ chạy ở profile `dev`, bỏ qua ở môi trường thật)

- Tài khoản: `admin@staybook.local` (ADMIN), `staff1@staybook.local`, `staff2@staybook.local` (STAFF), `guest1@staybook.local`, `guest2@staybook.local` (GUEST). Mật khẩu dev chung: `Staybook@123`.
- Loại phòng và phòng: Standard (tối đa 2 khách, 500.000 ₫, phòng 101–106); Deluxe (3 khách, 800.000 ₫, phòng 201–208); Family (5 khách, 1.200.000 ₫, phòng 301–304); Suite (4 khách, 1.800.000 ₫, phòng 401–402). Tổng 20 phòng. Phụ thu cuối tuần 10%.
- Một mùa giá: Deluxe "Cao điểm cuối năm" 20/12–05/01 giá nền 1.000.000 ₫.
- Cài đặt mặc định như mục 4. Tài khoản nhận cọc: dữ liệu giả.
- Khoảng 15 đặt phòng với ngày tương đối so với hôm nay, phủ đủ 8 trạng thái, gồm ít nhất: 2 đặt phòng nhận hôm nay, 1 trả hôm nay, 2 đang ở, 2 chờ xác nhận cọc (một cái quá 24 giờ), 1 chờ cọc sắp hết hạn, 1 hết hạn, 2 đã hủy (một hoàn cọc, một không hoàn), 1 không đến, 2 đã trả phòng (một có đánh giá).

## 11. Kiểm thử bắt buộc (JUnit 5, H2, `Clock` cố định)

- `PricingServiceTest`: ngày thường, đêm Thứ 6/Thứ 7, mùa giá, đêm qua ranh giới mùa, qua tháng/qua năm, ví dụ ở 5.3.
- `AvailabilityServiceTest`: đặt liền kề hợp lệ, chồng lấn bị từ chối, phòng bảo trì, phòng bị khóa theo ngày.
- `CancellationPolicyTest`: hủy trước mốc 48 giờ một phút, đúng mốc, sau mốc; hủy do cơ sở; không đến.
- `BookingStateMachineTest`: mọi chuyển hợp lệ và một số chuyển không hợp lệ.
- `BookingConcurrencyTest`: hai luồng cùng đặt phòng cuối cùng của một loại → chỉ một thành công.
- `BookingExpiryJobTest`: hết hạn đúng giờ, chạy lặp không sai, race với "đã chuyển khoản".
- `PaymentLedgerTest`: `paid`, `remaining`, `refundDue`; trả phòng chỉ khi `remaining == 0`.
- Test bảo mật bằng MockMvc: khách A không xem được booking của khách B; khách không vào được `/staff/**`; lễ tân không vào được `/admin/**`; thiếu CSRF bị từ chối.

## 12. Ngoài phạm vi

Cổng thanh toán tự động; đồng bộ nền tảng bên thứ ba; nhiều cơ sở/nhiều chủ; xác thực email khi đăng ký; ứng dụng di động; khai báo lưu trú với cơ quan chức năng; gia hạn thêm đêm (tạo booking mới); dọn phòng, kho, nhân sự, kế toán; đa ngôn ngữ, đa tiền tệ; VAT.

## 13. Thứ tự làm việc (mỗi bước = một lần giao cho agent, commit sau mỗi bước)

1. Khung dự án và cấu hình → 2. Entity và repository → 3. Tài khoản và phân quyền → 4. Loại phòng và phòng → 5. Tính giá → 6. Tìm phòng trống → 7. Đặt phòng và cọc → 8. Quản lý đặt phòng của lễ tân và sổ thanh toán → 9. Tự hết hạn giữ chỗ → (mốc "bản dùng được") → 10. Mùa giá, khóa phòng, cấu hình → 11. Đặt hộ, đổi phòng → 12. Sơ đồ phòng, hóa đơn → 13. Email → 14. Báo cáo, đánh giá → 15. Rà soát bảo mật → 16. Rà soát UX → 17. Dữ liệu mẫu, README, kịch bản demo.

Một bước chỉ coi là xong khi: build và test qua; chạy thử thủ công đúng kịch bản; agent đã giải thích luồng; sinh viên đã đọc diff, ghi nhật ký AI và commit.
