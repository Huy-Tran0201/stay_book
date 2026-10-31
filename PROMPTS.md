# PROMPTS.md – Bộ prompt giao việc cho Copilot Agent (StayBook)

## Cách dùng (đọc trước)

1. Mở khung Copilot Chat (Ctrl+Alt+I), ở ô chọn chế độ chuyển từ **Ask** sang **Agent**.
2. Mỗi prompt dưới đây = **một cuộc trò chuyện mới** (nút New Chat / dấu +). Như vậy agent không bị loãng ngữ cảnh và tiết kiệm hạn mức.
3. Trước khi gửi prompt: `git status` phải sạch, hoặc đã `git commit` bước trước. Nếu agent làm hỏng, bạn quay lại được bằng `git restore .` hoặc nút Undo.
4. Dán nguyên khối prompt. Các cụm `#file:...` là cách đính kèm file vào ngữ cảnh; nếu VS Code không nhận, bấm nút kẹp giấy (Add Context) rồi chọn file đó.
5. Agent sẽ xin phép chạy lệnh terminal; đọc lệnh trước khi bấm Allow.
6. Sau khi agent xong: (a) đọc danh sách file đã đổi và đọc kỹ phần lõi, (b) chạy `mvn test`, (c) chạy ứng dụng và thử theo "Kiểm tra thủ công", (d) dùng **Prompt giải thích** bên dưới, (e) ghi `docs/AI_LOG.md`, (f) commit.
7. Nếu một prompt quá nặng làm agent dừng giữa chừng, nhắn tiếp: "Tiếp tục từ chỗ đang dở, kiểm tra git diff để biết đã làm gì."

Phần cuối file có sẵn các prompt tiện ích (giải thích, sửa lỗi, tự rà soát).

---

## Tuần 1

### Prompt 1 – Khung dự án và cấu hình

```text
Đọc #file:PROJECT.md (mục 1 đến 3) và #file:.github/copilot-instructions.md.

Mục tiêu: hoàn thiện khung dự án chạy được. CHƯA làm nghiệp vụ.

Việc cần làm:
1. Phiên bản: dự án này phải dùng Java 25 và Spring Boot 3.5.16 (bản 3.x cuối cùng) để khớp tài liệu. Kiểm tra pom.xml: nếu parent đang là bản 4.x thì chuyển về 3.5.16 và đổi các starter cho đúng bản 3.5 (spring-boot-starter-web, -thymeleaf, -data-jpa, -security, -validation, -mail, -test; mysql-connector-j; h2; spring-security-test; devtools). Thêm thymeleaf-extras-springsecurity6 và WebJars cho Bootstrap 5 và Chart.js nếu thiếu (theo cách đúng với bản 3.5). Không thêm Lombok. Sau khi sửa, chạy "mvn clean test" để chắc chắn dự án build được.
2. Cấu hình: application.properties, application-dev.properties (MySQL qua biến môi trường DB_URL, DB_USERNAME, DB_PASSWORD; ddl-auto=update), application-test.properties (H2). Múi giờ Asia/Ho_Chi_Minh cho JVM, Jackson và Hibernate. Thư mục tải ảnh lấy từ UPLOAD_DIR. Tạo application-local.properties.example (không chứa mật khẩu thật). Cập nhật .gitignore để bỏ application-local.properties và thư mục uploads.
3. Tạo các package theo mục 3 của PROJECT.md (thêm class placeholder nếu cần để package tồn tại).
4. Tạo bean Clock (múi giờ Việt Nam), GlobalExceptionHandler và BusinessException, trang lỗi 403/404/500 thân thiện bằng tiếng Việt.
5. Tạo layout Thymeleaf dùng chung (header có menu theo vai trò, vùng thông báo flash, footer), trang chủ tạm, Bootstrap qua WebJars, responsive. Tạo messages.properties tiếng Việt.
6. SecurityConfig tạm: cho phép công khai "/", "/login", "/register", tài nguyên tĩnh; mọi đường dẫn khác yêu cầu đăng nhập (chi tiết làm ở bước sau).

Tiêu chí hoàn thành: "mvn clean test" qua; chạy ứng dụng với profile dev, mở http://localhost:8080 thấy trang chủ.

Khi xong: liệt kê file đã tạo/sửa, giải thích cấu trúc dự án cho người mới học Spring Boot, nêu việc chưa làm và giả định.
```

Kiểm tra thủ công: mở trang chủ, mở một đường dẫn không tồn tại để thấy trang 404 tiếng Việt.

### Prompt 2 – Entity, enum và repository

```text
Đọc #file:PROJECT.md (mục 4, 5.1, 5.2) và #file:.github/copilot-instructions.md.

Mục tiêu: tạo toàn bộ mô hình dữ liệu. KHÔNG viết logic nghiệp vụ ở bước này.

Việc cần làm:
1. Tạo mọi enum và entity trong mục 4, đúng tên và kiểu dữ liệu, đặt vào đúng package theo mục 3. Quan hệ JPA đúng; ràng buộc duy nhất (email, roomNumber, booking code, review theo booking); độ dài cột hợp lý; chỉ mục gợi ý ở cuối mục 4. Tiền dùng long.
2. Tạo repository cho từng entity. Thêm các truy vấn JPQL có tham số (không nối chuỗi):
   - tìm booking đang giữ chỗ (trạng thái PENDING_DEPOSIT, AWAITING_CONFIRMATION, CONFIRMED, CHECKED_IN) chồng lấn với khoảng [in, out) theo một phòng;
   - RoomTypeRepository.findByIdForUpdate dùng PESSIMISTIC_WRITE;
   - tìm phòng ACTIVE của một loại phòng không có booking giữ chỗ chồng lấn và không có RoomBlock chồng lấn.
3. Viết vài test @DataJpaTest (H2): lưu/đọc Booking cùng BookingNight; truy vấn chồng lấn đúng với trường hợp liền kề (trả ngày 8, nhận ngày 8 thì KHÔNG chồng lấn).
4. Tạo file docs/ERD.md chứa sơ đồ quan hệ dạng mermaid (erDiagram) đúng với entity vừa tạo, để em đối chiếu với tài liệu SRS.

Tiêu chí hoàn thành: "mvn clean test" qua; ứng dụng khởi động với profile dev và tạo đủ bảng trong MySQL.

Khi xong: liệt kê file, giải thích các quan hệ chính và vì sao truy vấn chồng lấn viết như vậy, nêu việc chưa làm.
```

### Prompt 3 – Tài khoản và phân quyền

```text
Đọc #file:PROJECT.md (mục 4 phần User, mục 5 BR-19 và BR-20, mục 7 phần Công khai, mục 8) và #file:.github/copilot-instructions.md.

Mục tiêu: đăng ký, đăng nhập, phân quyền 3 vai trò.

Việc cần làm:
1. Đăng ký khách (họ tên, email, SĐT 10 số bắt đầu bằng 0, mật khẩu ≥ 8 ký tự, nhập lại mật khẩu). email lưu chữ thường, không trùng ("email đã được sử dụng."). Mật khẩu băm BCrypt. Vai trò mặc định GUEST.
2. Đăng nhập bằng Spring Security (form login, UserDetailsService từ User). Sai 5 lần liên tiếp → TemP_LOCKED 15 phút; hết thời gian tự mở. Tài khoản LOCKED báo "Tài khoản đã bị khóa. Vui lòng liên hệ cơ sở." Thông báo sai: "email hoặc mật khẩu không chính xác.". Đăng nhập thành công thì xóa bộ đếm sai.
3. SecurityConfig: công khai (/, /rooms, /room-types/**, /login, /register, /forgot-password, /reset-password, tài nguyên tĩnh); /staff/** cho STAFF và ADMIN; /admin/** chỉ ADMIN; còn lại cần đăng nhập. Bật CSRF. Sau đăng nhập quay lại trang trước đó nhưng CHỈ đường dẫn nội bộ. Điều hướng sau đăng nhập: GUEST về trang chủ, STAFF/ADMIN về /staff/today (tạo trang tạm).
4. Quên/đổi mật khẩu: token ngẫu nhiên, chỉ lưu băm, dùng một lần, hết hạn 30 phút. Gửi email qua emailService tạm (ghi ra log). Trang hồ sơ cho phép đổi họ tên, SĐT và mật khẩu (nhập mật khẩu cũ).
5. Giao diện: menu thay đổi theo vai trò (dùng thymeleaf-extras-springsecurity6). Lỗi hiện cạnh ô nhập.
6. Ghi AuditLog khi đăng nhập sai, khóa tài khoản. (Có thể tạo AuditService đơn giản.)
7. Viết test MockMvc: khách không vào được /staff/** và /admin/**; STAFF không vào được /admin/**; thiếu CSRF thì bị từ chối; khóa tạm sau 5 lần sai (dùng Clock cố định).
8. Chỉ ở profile dev: tự tạo sẵn 5 tài khoản theo mục 10 của PROJECT.md (admin@staybook.local, staff1@staybook.local, staff2@staybook.local, guest1@staybook.local, guest2@staybook.local, mật khẩu Staybook@123), CHƯA tạo dữ liệu nào khác. Chạy lặp không được nhân đôi tài khoản; không chạy ở môi trường khác dev.

Tiêu chí hoàn thành: test qua; chạy thử đăng ký, đăng nhập, đăng xuất, sai mật khẩu 5 lần.

Khi xong: liệt kê file, giải thích luồng đăng nhập và cách Spring Security kiểm tra quyền, nêu việc chưa làm.
```

Kiểm tra thủ công: đăng ký, đăng nhập, thử mật khẩu sai 5 lần, thử mở `/admin` bằng tài khoản khách.

---

## Tuần 2

### Prompt 4 – Loại phòng và phòng (quản trị)

```text
Đọc #file:PROJECT.md (mục 4 phần RoomType/Room/Amenity/RoomTypeImage, mục 5 BR-14, BR-21, BR-23, mục 7 phần Quản trị) và #file:.github/copilot-instructions.md.

Mục tiêu: ADMIN quản lý loại phòng và phòng.

Việc cần làm:
1. /admin/room-types: danh sách, thêm, sửa, ngừng hoạt động (không xóa cứng nếu đã có lịch sử). Trường: tên, mô tả, sức chứa 1–10, diện tích, loại giường, giá cơ bản > 0, phụ thu cuối tuần %, tiện nghi (chọn từ danh sách Amenity có sẵn: Wi-Fi, điều hòa, TV, tủ lạnh, bồn tắm, ban công, bữa sáng, chỗ đỗ xe), ảnh.
2. Tải ảnh: JPG/PNG, tối đa 2 MB, tối đa 5 ảnh mỗi loại phòng; kiểm tra bằng NỘI DUNG tệp (không chỉ đuôi), đổi tên tệp ngẫu nhiên, lưu vào UPLOAD_DIR, phục vụ qua một đường dẫn riêng. Từ chối kèm lý do khi vi phạm.
3. /admin/rooms: danh sách (nhóm theo loại), thêm, sửa; số phòng không trùng; trạng thái ACTIVE/MAINTENANCE/INACTIVE. Không cho chuyển sang MAINTENANCE hoặc INACTIVE khi phòng còn booking giữ chỗ từ hôm nay trở đi; hiển thị danh sách các booking bị ảnh hưởng.
4. Form dùng Form/DTO riêng, validate bằng Bean Validation, báo lỗi tiếng Việt cạnh ô nhập.
5. Test: kiểm tra từ chối tệp .exe, ảnh 3 MB, ảnh thứ 6; từ chối chuyển phòng sang bảo trì khi còn booking giữ chỗ.

Tiêu chí hoàn thành: test qua; ADMIN tạo được 4 loại phòng và 20 phòng như mục 10 (làm bằng giao diện, chưa cần seed).

Khi xong: liệt kê file, giải thích luồng tải ảnh an toàn, nêu việc chưa làm.
```

### Prompt 5 – Tính giá (logic cốt lõi, em sẽ đọc kỹ từng dòng)

```text
Đọc #file:PROJECT.md (mục 5 BR-08, BR-09, BR-16, mục 5.3, 5.4 bước 4) và #file:.github/copilot-instructions.md.

Mục tiêu: PricingService thuần (không phụ thuộc web/controller) và test thật kỹ.

Việc cần làm:
1. Tạo PricingService với phương thức tính danh sách giá từng đêm (NightPrice: ngày, giá nền, phụ thu, giá đêm) cho một RoomType và khoảng [checkIn, checkOut), cùng tổng tiền và tiền cọc (làm tròn LÊN đến 1.000 đồng, tỷ lệ cọc đọc từ Setting).
2. Khoảng mùa giá gồm cả hai đầu. Phụ thu chỉ cho đêm Thứ 6 và Thứ 7, tính trên giá nền (kể cả giá mùa), làm tròn đến đồng.
3. Thêm SeasonPrice vào tính toán (dù UI quản lý mùa giá làm ở bước sau).
4. Unit test (không cần Spring context nếu được): ví dụ 1 và ví dụ 2 trong mục 5.3 phải ra đúng 2.560.000 ₫ / cọc 768.000 ₫ và 2.880.000 ₫; đêm đơn lẻ; qua tháng và qua năm; đêm đầu tiên của mùa và đêm cuối cùng của mùa; làm tròn cọc (ví dụ tổng 1.234.000 ₫ → cọc 371.000 ₫).
5. Ở trang chi tiết loại phòng (tạo trang tạm /room-types/{id} nếu chưa có), hiển thị "Bảng giá tham khảo": giá ngày thường, giá đêm cuối tuần.

Tiêu chí hoàn thành: tất cả test của PricingService qua.

Khi xong: liệt kê file, giải thích từng bước thuật toán bằng ví dụ có số, nêu giả định về làm tròn.
```

---

## Tuần 3

### Prompt 6 – Tìm phòng trống và chi tiết loại phòng

```text
Đọc #file:PROJECT.md (mục 5 BR-01, BR-03, BR-04, BR-05, BR-14, mục 5.2, mục 7 phần Công khai, mục 9) và #file:.github/copilot-instructions.md.

Mục tiêu: khách tìm phòng trống không cần đăng nhập.

Việc cần làm:
1. AvailabilityService: trả danh sách loại phòng còn ít nhất một phòng trống TRỌN khoảng [checkIn, checkOut) và đủ sức chứa; mỗi kết quả kèm số phòng còn lại, giá trung bình mỗi đêm và tổng giá (dùng PricingService).
2. Trang chủ có form tìm: ngày nhận mặc định hôm nay, ngày trả mặc định hôm sau, ô chọn ngày không cho chọn ngày không hợp lệ, số khách. Validate phía máy chủ theo BR-01 với đúng 3 thông báo trong mục 9. Không có kết quả: "Không còn phòng phù hợp trong các ngày bạn chọn." kèm gợi ý đổi ngày.
3. Trang /rooms: danh sách kết quả dạng thẻ, ảnh đại diện, tiện nghi chính, số phòng còn lại, giá trung bình, tổng giá cho cả kỳ. Lọc theo khoảng giá, tiện nghi, sức chứa tối thiểu; sắp xếp theo giá tăng/giảm, sức chứa.
4. Trang /room-types/{id}: thư viện ảnh, mô tả, tiện nghi, diện tích, giường, sức chứa, giá và ghi chú phụ thu cuối tuần, chính sách cọc và hủy (đọc từ Setting), giờ nhận/trả, nút "Đặt phòng" mang theo ngày đã chọn.
5. Giữ nguyên tham số tìm kiếm khi người dùng chuyển giữa các trang.
6. Test AvailabilityService: đặt liền kề vẫn còn phòng, chồng lấn thì hết, phòng bảo trì và phòng bị khóa theo ngày không tính, sức chứa không đủ thì loại.

Tiêu chí hoàn thành: test qua; giao diện dùng tốt ở màn hình điện thoại (kiểm tra bằng chế độ giả lập điện thoại của trình duyệt).

Khi xong: liệt kê file, giải thích truy vấn tìm phòng trống, nêu việc chưa làm.
```

### Prompt 7A – Tạo đặt phòng, giữ chỗ và trang chuyển cọc (logic cốt lõi)

```text
Đọc #file:PROJECT.md (mục 5 BR-01 đến BR-07, BR-16, BR-24, BR-25, mục 5.4, mục 6, mục 7 phần Khách, mục 9) và #file:.github/copilot-instructions.md.

Mục tiêu: khách đặt phòng và giữ chỗ, chống đặt trùng.

Việc cần làm:
1. BookingService.createBooking theo đúng 4 bước ở mục 5.4 trong MỘT @Transactional (khóa RoomType bằng PESSIMISTIC_WRITE, chọn phòng số nhỏ nhất còn trống, tính giá lại ở máy chủ, tạo BookingNight, tạo mã BK-yyMMdd-XXXX duy nhất, ghi BookingEvent). Form KHÔNG chứa giá hay cọc; máy chủ tự tính (BR-25).
2. BookingStatemachine / bảng chuyển trạng thái đúng mục 6. Mọi chuyển trạng thái đi qua đây và ghi BookingEvent. Chuyển PENDING_DEPOSIT → AWAITING_CONFIRMATION cho khách chủ booking, từ chối nếu quá hạn giữ chỗ.
3. GET /booking/new (bước Thông tin): hiện loại phòng, ngày, số khách, thông tin liên hệ (lấy từ hồ sơ, sửa được), ghi chú, bảng giá từng đêm, tổng tiền, tiền cọc, chính sách hủy. Nếu ngày nhận cách thời điểm đặt dưới 48 giờ: cảnh báo "Đặt phòng này sẽ không được hoàn cọc nếu hủy." và bắt buộc tích ô xác nhận. Chưa đăng nhập thì chuyển tới đăng nhập rồi quay lại đúng lựa chọn.
4. POST /booking → tạo booking. Hết phòng giữa chừng: báo "Loại phòng này vừa hết phòng trong các ngày bạn chọn. Vui lòng chọn ngày hoặc loại phòng khác." Quá BR-24: báo cần thanh toán hoặc hủy bớt đặt phòng đang chờ.
5. GET /booking/{code}/payment: thanh toán cọc: mã đặt, số tiền cọc, ảnh VietQR (số tiền + nội dung chuyển khoản = mã đặt; nếu không tạo được ảnh thì hiện số tài khoản và nội dung bằng chữ), thông tin tài khoản từ Setting, đồng hồ đếm ngược giữ chỗ, nút "em đã chuyển khoản" (POST /booking/{code}/paid-notice). Thanh tiến trình 4 bước: Chọn phòng → Thông tin → Chuyển cọc → Hoàn tất. Kiểm tra chủ sở hữu booking.
6. Test: unit test createBooking (giá, cọc, trạng thái, hạn giữ chỗ); BookingConcurrencyTest: hai luồng cùng đặt phòng cuối cùng của một loại → chỉ một thành công, không có hai booking chồng lấn trên cùng phòng; đặt liền kề hợp lệ; BR-24; form bị sửa giá vẫn dùng giá máy chủ.

Tiêu chí hoàn thành: test qua (đặc biệt test đồng thời, chạy nhiều lần vẫn qua); thử thủ công đặt phòng từ lúc tìm đến trang chuyển cọc.

Khi xong: liệt kê file, giải thích bằng lời từng bước vì sao không thể đặt trùng, nêu việc chưa làm.
```

### Prompt 7B – Đặt phòng của em và hủy phòng

```text
Đọc #file:PROJECT.md (mục 5 BR-11, BR-20, mục 5.5, 5.6, mục 6, mục 7 phần Khách) và #file:.github/copilot-instructions.md.

Mục tiêu: khách xem và hủy đặt phòng của mình, hoàn cọc đúng chính sách.

Việc cần làm:
1. CancellationPolicy: tính refundEligibleAmount theo mục 5.5 (mốc 48 giờ tính từ 14:00 ngày nhận; đúng mốc vẫn được hoàn). Đọc refund.hours từ Setting.
2. GET /my-bookings (mới nhất trước, có huy hiệu trạng thái) và GET /my-bookings/{code} (các đêm và giá, cọc, các khoản thanh toán, hạn giữ chỗ, số phòng chỉ hiện khi CONFIRMED). Khách chỉ thấy booking của mình; booking người khác trả 404.
3. POST /my-bookings/{code}/cancel: hiển thị trước số tiền được hoàn, hộp thoại xác nhận nêu rõ hậu quả; chỉ hủy được khi PENDING_DEPOSIT, AWAITING_CONFIRMATION, CONFIRMED. Hủy khi AWAITING_CONFIRMATION thì đặt refundCheckNeeded = true. Lưu cancelCause, cancelReason, cancelledAt, refundEligibleAmount và ghi BookingEvent.
4. Trang trống ("Bạn chưa có đặt phòng nào") có nút dẫn tới tìm phòng.
5. Test: hủy trước mốc 1 phút, đúng mốc, sau mốc 1 phút; hủy PENDING_DEPOSIT không phát sinh tiền; khách A không xem/hủy được booking của khách B; không hủy được khi CHECKED_IN.

Tiêu chí hoàn thành: test qua; thử thủ công hai ca hủy (trước và sau mốc 48 giờ).

Khi xong: liệt kê file, giải thích cách tính hoàn cọc bằng ví dụ có số (nhận 06/11 14:00, hủy 03/11 10:00 và 05/11 10:00), nêu việc chưa làm.
```

---

## Tuần 4 (mốc "bản dùng được")

### Prompt 8A – Bảng việc của lễ tân và xác nhận cọc

```text
Đọc #file:PROJECT.md (mục 4 phần Payment, mục 5 BR-17, BR-18, mục 5.6, mục 6, mục 7 phần Lễ tân, mục 9) và #file:.github/copilot-instructions.md.

Mục tiêu: lễ tân xem việc cần làm và xác nhận tiền cọc.

Việc cần làm:
1. PaymentService: ghi Payment (chỉ thêm, không có sửa/xóa), tính paid, remaining, refundDue đúng mục 5.6. Mỗi lần ghi có recordedBy và ghi AuditLog.
2. GET /staff/today: các nhóm Nhận phòng hôm nay, Trả phòng hôm nay, Đang ở, Chờ xác nhận cọc (tô cảnh báo nếu quá 24 giờ kể từ lúc khách báo chuyển), Chờ cọc sắp hết hạn. Mỗi dòng có nút mở chi tiết.
3. GET /staff/bookings: lọc theo trạng thái, ngày nhận, ngày trả, loại phòng; tìm theo mã, tên, SĐT; phân trang 20 dòng. GET /staff/bookings/{code}: chi tiết gồm khách, phòng, các đêm, thanh toán, lịch sử chuyển trạng thái.
4. POST confirm-deposit: nhập số tiền thực nhận và hình thức; nếu < cọc báo "Số tiền nhận chưa đủ tiền cọc."; đủ thì ghi Payment(DEPOSIT), chuyển CONFIRMED qua state machine. POST reject-deposit: nhập lý do, chuyển CANCELLED với cancelCause = DEPOSIT_REJECTED.
5. Fragment huy hiệu trạng thái dùng chung (xem mục 9). Nút lớn, ít bước, hộp thoại xác nhận cho thao tác từ chối.
6. Test: PaymentLedgerTest; confirm-deposit đủ/thiếu tiền; STAFF mới dùng được, GUEST bị từ chối.

Tiêu chí hoàn thành: test qua; thử thủ công: khách đặt phòng, bấm "em đã chuyển khoản", lễ tân xác nhận, khách thấy trạng thái Đã xác nhận.

Khi xong: liệt kê file, giải thích sổ thanh toán và vì sao không cho sửa/xóa khoản đã ghi, nêu việc chưa làm.
```

### Prompt 8B – Nhận phòng, phụ phí, trả phòng, hủy, không đến

```text
Đọc #file:PROJECT.md (mục 5 BR-12, BR-13, BR-26, mục 5.5, 5.6, mục 6, mục 7 phần Lễ tân) và #file:.github/copilot-instructions.md.

Mục tiêu: hoàn tất vòng đời đặt phòng từ nhận phòng đến trả phòng.

Việc cần làm:
1. Nhận phòng (POST check-in): chỉ khi CONFIRMED và hôm nay ≥ ngày nhận ("Chưa đến ngày nhận phòng."; chưa xác nhận cọc: "Đặt phòng chưa được xác nhận cọc."). Có ô tùy chọn loại giấy tờ + 4 số cuối (không lưu toàn bộ số giấy tờ). Ghi checkedInAt.
2. Phụ phí (POST surcharges: tên, số tiền > 0) và thanh toán thêm (POST payments: EXTRA_PAYMENT, số tiền, hình thức). Hiển thị rõ: tiền phòng, phụ phí, đã thu, CÒN PHẢI THU.
3. Trả phòng (POST check-out): chỉ khi CHECKED_IN và remaining == 0, nếu chưa thì báo "Còn phải thu … ₫.". Trả sớm vẫn được nhưng không hoàn tiền đêm chưa ở. Ghi checkedOutAt.
4. Hủy bởi lễ tân (POST cancel): chọn nguyên nhân "Khách yêu cầu" (hoàn theo CancellationPolicy) hoặc "Cơ sở hủy" (hoàn 100% DEPOSIT); hiển thị số tiền cần hoàn; ghi Payment(REFUND) khi lễ tân xác nhận đã hoàn; hiển thị "Cần hoàn" cho tới khi hoàn xong.
5. Không đến (POST no-show): chỉ từ 00:00 ngày kế tiếp ngày nhận; cọc giữ lại (refundEligibleAmount = 0); các đêm còn lại trở lại trống; booking đã NO_SHOW không nhận phòng lại được.
6. Hộp thoại xác nhận nêu rõ hậu quả cho hủy, không đến, trả phòng.
7. Test: ví dụ trả phòng (tiền phòng 2.560.000 ₫, cọc 768.000 ₫, phụ phí 150.000 ₫ → còn phải thu 1.942.000 ₫); không trả phòng được khi chưa thu đủ; nhận phòng sớm bị từ chối; không đến; hủy do cơ sở; các chuyển trạng thái không hợp lệ bị từ chối.

Tiêu chí hoàn thành: test qua; chạy thủ công trọn vòng: đặt → cọc → xác nhận → nhận phòng → phụ phí → thu tiền → trả phòng.

Khi xong: liệt kê file, giải thích máy trạng thái và công thức còn phải thu bằng ví dụ có số, nêu việc chưa làm.
```

### Prompt 9 – Tự hết hạn giữ chỗ

```text
Đọc #file:PROJECT.md (mục 5 BR-06, mục 6, mục 7 phần Tác vụ nền) và #file:.github/copilot-instructions.md.

Mục tiêu: tự giải phóng phòng khi hết hạn giữ chỗ.

Việc cần làm:
1. Bật Spring Scheduling. BookingExpiryJob chạy mỗi phút (fixedDelay): tìm booking PENDING_DEPOSIT có holdExpiresAt < now (lấy now từ Clock); với từng booking mở giao dịch riêng, đọc lại trạng thái và chỉ chuyển sang EXPIRED nếu vẫn là PENDING_DEPOSIT; ghi BookingEvent với actor "SYSTem". Lỗi ở một booking không làm dừng các booking còn lại.
2. Sau khi EXPIRED, phòng phải xuất hiện lại khi tìm kiếm.
3. Test (Clock cố định): đặt lúc 10:00 không cọc, lúc 10:31 chạy job → EXPIRED; chạy job hai lần không lỗi; khách bấm "đã chuyển khoản" trước khi job chạy thì KHÔNG bị hết hạn; khách bấm đúng lúc quá hạn thì bị từ chối; booking AWAITING_CONFIRMATION không bao giờ bị job hủy.
4. Hiển thị cho khách trên trang "Đặt phòng của em" thông báo "Đặt phòng đã hết hạn giữ chỗ. Vui lòng đặt lại." với nút đặt lại.

Tiêu chí hoàn thành: test qua; thử thủ công bằng cách đặt tạm hold.minutes = 1.

Khi xong: liệt kê file, giải thích vì sao job an toàn khi chạy lặp, nêu việc chưa làm.
```

Mốc cuối Tuần 4: chạy trọn kịch bản demo ở Prompt 17 bằng tay. Nếu xong và test qua, bạn đã có bản dùng được.

---

## Tuần 5 (nhóm "Nên có")

### Prompt 10 – Mùa giá, khóa phòng, cài đặt

```text
Đọc #file:PROJECT.md (mục 4 phần SeasonPrice/RoomBlock/Setting, mục 5 BR-10, BR-14, mục 7 phần Quản trị) và #file:.github/copilot-instructions.md.

Việc cần làm:
1. /admin/seasons: thêm/sửa/xóa mùa giá theo loại phòng (tên, từ ngày, đến ngày, giá nền). Khoảng ngày không được chồng lấn trong cùng loại: "Khoảng ngày bị trùng với mùa giá đã có." Đổi giá không ảnh hưởng booking cũ (BookingNight đã chốt giá).
2. /admin/room-blocks: khóa phòng theo khoảng ngày (gồm cả hai đầu) kèm lý do; không tạo được nếu chồng lấn booking giữ chỗ (liệt kê booking bị ảnh hưởng); khoảng khóa cùng phòng không chồng nhau; xóa được khóa.
3. /admin/settings: sửa các Setting (tỷ lệ cọc, thời gian giữ chỗ, mốc hoàn cọc, giờ nhận/trả, thông tin cơ sở, tài khoản nhận cọc). Validate giá trị hợp lệ. Ghi AuditLog khi đổi giá hoặc chính sách.
4. Test: mùa chồng lấn bị từ chối; khóa phòng chồng booking bị từ chối; đổi giá không đổi tổng tiền booking cũ.

Khi xong: liệt kê file, giải thích và nêu việc chưa làm.
```

### Prompt 11 – Đặt hộ khách và đổi phòng

```text
Đọc #file:PROJECT.md (mục 5 BR-04, BR-14, mục 6, mục 7 phần Lễ tân) và #file:.github/copilot-instructions.md.

Việc cần làm:
1. GET/POST /staff/bookings/new: lễ tân nhập tên, SĐT, email (tùy chọn), tìm phòng như trang khách, tạo booking qua CÙNG BookingService.createBooking (cùng chống trùng). source = STAFF_ASSISTED, user rỗng. Cho chọn "thu cọc ngay" (số tiền ≥ cọc, hình thức) → ghi DEPOSIT và vào thẳng CONFIRMED; nếu không thì PENDING_DEPOSIT như bình thường.
2. POST change-room: với booking CONFIRMED hoặc CHECKED_IN, đổi sang phòng CÙNG LOẠI còn trống các đêm còn lại; không đổi giá; ghi BookingEvent và AuditLog; phòng có booking chồng lấn thì từ chối.
3. Test: đặt hộ có chống trùng; thu cọc ngay; đổi phòng thành công/thất bại; phòng cũ trống lại sau khi đổi.

Khi xong: liệt kê file, giải thích và nêu việc chưa làm.
```

### Prompt 12 – Sơ đồ phòng và hóa đơn

```text
Đọc #file:PROJECT.md (mục 7 phần Lễ tân: room-grid và invoice, mục 9) và #file:.github/copilot-instructions.md.

Việc cần làm:
1. GET /staff/room-grid?from=yyyy-MM-dd: bảng hàng là từng phòng (nhóm theo loại), cột là 14 ngày từ ngày chọn, nút trước/sau 14 ngày và "Hôm nay". Màu theo tình trạng đêm đó: trống, giữ chỗ (PENDING_DEPOSIT/AWAITING_CONFIRMATION), đã xác nhận, đang ở, bảo trì/khóa. Tình trạng được SUY RA từ booking và RoomBlock, không lưu riêng. Có chú giải màu bằng chữ. Bấm ô có booking mở chi tiết. Tính bằng vài truy vấn gộp, tránh truy vấn lặp từng ô.
2. GET /staff/bookings/{code}/invoice: hóa đơn in được (CSS in riêng): thông tin cơ sở, mã đặt, khách, ngày nhận/trả, danh sách từng đêm và giá, phụ phí, tổng cộng, đã cọc, thanh toán thêm, còn lại; nút In. Chỉ cho booking CHECKED_OUT.
3. Test: dữ liệu sơ đồ phòng đúng cho vài tình huống; hóa đơn từ chối khi chưa trả phòng.

Khi xong: liệt kê file, giải thích và nêu việc chưa làm.
```

### Prompt 13 – email thông báo

```text
Đọc #file:PROJECT.md (mục 7 phần Tác vụ nền và email) và #file:.github/copilot-instructions.md.

Việc cần làm:
1. emailService dùng JavaMailSender. Cấu hình SMTP Gmail qua biến môi trường MAIL_USERNAME, MAIL_PASSWORD (mật khẩu ứng dụng). Khi chưa cấu hình thì chỉ ghi nội dung email ra log.
2. email (template Thymeleaf, tiếng Việt, đủ mã đặt, ngày, tổng tiền, chính sách): đã nhận đặt phòng (kèm hạn giữ chỗ), đã xác nhận cọc, đã hủy (kèm số tiền hoàn nếu có), hết hạn giữ chỗ, nhắc trước ngày nhận.
3. Gửi bất đồng bộ hoặc sau khi giao dịch commit (không để lỗi email làm hỏng nghiệp vụ). Lỗi thì thử lại tối đa 3 lần cách 5 phút rồi ghi AuditLog.
4. CheckInReminderJob chạy 09:00 mỗi ngày (cron, zone Asia/Ho_Chi_Minh): booking CONFIRMED có ngày nhận là ngày mai và reminderSentAt trống → gửi và ghi reminderSentAt. Chạy lặp không gửi trùng.
5. Test: gửi đúng loại email ở đúng thời điểm chuyển trạng thái (dùng emailService giả); nhắc không gửi trùng; lỗi email không làm rollback giao dịch.

Khi xong: liệt kê file, giải thích và nêu việc chưa làm.
```

### Prompt 14 – Báo cáo và đánh giá

```text
Đọc #file:PROJECT.md (mục 5.7, mục 7 phần Quản trị, mục 5 BR-22) và #file:.github/copilot-instructions.md.

Việc cần làm:
1. /admin/reports: chọn khoảng ngày (tối đa 366 ngày). Công suất mỗi ngày, doanh thu theo ngày và tháng (tính từ Payment theo mục 5.7), số booking theo trạng thái, tỉ lệ hủy/không đến/hết hạn, loại phòng được đặt nhiều nhất, tỉ lệ khách tự đặt so với lễ tân đặt hộ. Biểu đồ Chart.js (WebJars) + bảng số liệu.
2. Đánh giá: POST /my-bookings/{code}/review (1–5 sao, nhận xét ≤ 1000 ký tự) chỉ khi CHECKED_OUT và chưa đánh giá. Hiển thị điểm trung bình và nhận xét trên trang chi tiết loại phòng; ADMIN ẩn/hiện nhận xét. Hiển thị nhận xét bằng th:text (chống XSS).
3. Test: 10 phòng khả dụng, 4 phòng có khách → công suất 40%; doanh thu khớp tổng sổ thanh toán; đánh giá chỉ một lần và chỉ sau trả phòng; nhận xét chứa thẻ HTML được hiển thị dạng chữ.

Khi xong: liệt kê file, giải thích và nêu việc chưa làm.
```

---

## Tuần 6 (rà soát và hoàn thiện)

### Prompt 15 – Rà soát bảo mật

```text
Đọc #file:PROJECT.md (mục 5 BR-19, BR-20, BR-24, BR-25, mục 8) và #file:.github/copilot-instructions.md.

Hãy RÀ SOÁT toàn bộ dự án như một người kiểm tra bảo mật, theo danh sách sau và lập báo cáo trước khi sửa:
1. Mọi đường dẫn có tham số mã đặt phòng/id: đã kiểm tra chủ sở hữu hoặc vai trò ở tầng service chưa? Có thể đoán id để xem dữ liệu người khác không?
2. Mọi form POST đã có CSRF chưa? Có endpoint nào đổi dữ liệu bằng GET không?
3. Có chỗ nào tin giá, cọc, số tiền, trạng thái, vai trò từ trình duyệt không?
4. Mọi chỗ hiển thị dữ liệu người dùng nhập: có dùng th:utext không? Có XSS không?
5. Tải ảnh: kiểm tra nội dung, kích thước, tên tệp, đường dẫn (path traversal)?
6. Đăng nhập: khóa tạm, thông báo lỗi, chuyển hướng sau đăng nhập (open redirect), cookie HttpOnly/SameSite, session fixation?
7. Đặt lại mật khẩu: token ngẫu nhiên, chỉ lưu băm, dùng một lần, hết hạn?
8. Có lộ stack trace, mật khẩu, khóa bí mật trong mã nguồn, log hoặc trang lỗi không?
9. Dữ liệu cá nhân của khách (tên, SĐT, email, giấy tờ): ai xem được? Có ghi vào log không?
10. Truy vấn có nối chuỗi SQL không?

Sau khi lập báo cáo (liệt kê vấn đề kèm mức độ cao/trung bình/thấp và file liên quan), SỬA các vấn đề mức cao và trung bình, thêm test cho từng lỗi đã sửa, rồi báo lại. Vấn đề mức thấp chỉ liệt kê.
```

### Prompt 16 – Rà soát UX

```text
Đọc #file:PROJECT.md (mục 9) và #file:.github/copilot-instructions.md.

Hãy RÀ SOÁT trải nghiệm người dùng trên toàn bộ giao diện, lập báo cáo trước khi sửa, theo danh sách:
1. Luồng đặt phòng 4 bước có thanh tiến trình? Luôn thấy tổng tiền, số đêm, chính sách hủy trước khi bấm đặt?
2. Mọi lỗi hiện ngay cạnh ô nhập bằng tiếng Việt dễ hiểu? Có chỗ nào hiện lỗi kỹ thuật không?
3. Thao tác khó hoàn tác (hủy, không đến, trả phòng, từ chối cọc) có hộp thoại xác nhận nêu rõ hậu quả không?
4. Huy hiệu trạng thái thống nhất ở mọi nơi (tên + màu + chữ)?
5. Trang trống và kết quả không có dữ liệu có câu hướng dẫn bước tiếp theo?
6. Giao diện dùng tốt ở màn hình điện thoại (bảng cuộn ngang được, nút đủ lớn)? Sơ đồ phòng dùng được trên máy tính?
7. Ô nhập có nhãn, đủ tương phản, thứ tự tab hợp lý, nút có chữ rõ nghĩa?
8. Định dạng tiền (1.250.000 ₫) và ngày (dd/MM/yyyy) nhất quán?
9. Trang lễ tân: bảng việc "Hôm nay" có làm được việc chính trong tối đa 3 lần bấm?

Sau báo cáo, SỬA các điểm quan trọng nhất (tối đa 10 điểm), liệt kê điểm nào đã sửa và điểm nào để lại.
```

### Prompt 17 – Dữ liệu mẫu, README và kịch bản demo

```text
Đọc #file:PROJECT.md (mục 10, mục 11) và #file:.github/copilot-instructions.md.

Việc cần làm:
1. Dữ liệu mẫu theo mục 10, chỉ chạy ở profile dev (CommandLineRunner hoặc tương đương), chạy lặp không nhân đôi dữ liệu. Booking mẫu dùng ngày tương đối so với hôm nay và phủ đủ 8 trạng thái.
2. README.md: giới thiệu ngắn, yêu cầu cài đặt (JDK 17, MySQL 8), cách tạo CSDL, biến môi trường, cách chạy ("mvn spring-boot:run" với profile dev), tài khoản mẫu, cách chạy test, cấu trúc thư mục.
3. docs/DemO.md: kịch bản demo 8 đến 10 phút gồm các bước bấm cụ thể: (a) khách tìm phòng và thấy giá cuối tuần; (b) đặt phòng, thấy đếm ngược giữ chỗ; (c) báo đã chuyển khoản; (d) lễ tân xác nhận cọc; (e) minh họa chống trùng: hai tab trình duyệt cùng đặt phòng cuối; (f) nhận phòng, thêm phụ phí, thu tiền, trả phòng, in hóa đơn; (g) hủy sớm và hủy sát giờ để thấy khác biệt hoàn cọc; (h) sơ đồ phòng; (i) báo cáo công suất và doanh thu.
4. Chạy toàn bộ test và đảm bảo qua. Liệt kê test nào có, test nào còn thiếu so với mục 11.

Khi xong: liệt kê file và cách em chạy thử toàn bộ.
```

---

## Prompt tiện ích

### A. Prompt giải thích (dùng sau MỖI bước, để chuẩn bị bảo vệ)

```text
Hãy giải thích phần vừa làm (các file thay đổi trong git diff) cho em như đang chuẩn bị bảo vệ trước giảng viên:
1. Luồng xử lý từ lúc người dùng bấm nút đến lúc ghi vào CSDL, đi qua những lớp nào.
2. Các quy tắc nghiệp vụ được cài ở đâu (tên lớp, tên hàm).
3. Những chỗ dễ sai hoặc em nên đọc kỹ nhất, và vì sao.
4. 3 câu hỏi giảng viên có thể hỏi về phần này và gợi ý trả lời.
Giải thích bằng tiếng Việt đơn giản, kèm ví dụ số cụ thể khi có thể.
```

### B. Prompt sửa lỗi (dán kèm đoạn log lỗi)

```text
Ứng dụng bị lỗi khi em làm như sau: [mô tả thao tác]. Log lỗi:

[dán log]

Hãy tìm nguyên nhân gốc (không chỉ che lỗi), sửa với thay đổi nhỏ nhất, thêm test tái hiện lỗi này rồi chạy lại toàn bộ test. Báo cho em biết nguyên nhân là gì và vì sao cách sửa này đúng.
```

### C. Prompt tự rà soát code vừa viết

```text
Hãy đóng vai người review code nghiêm khắc cho các thay đổi gần nhất (git diff). Kiểm tra: đúng quy tắc trong #file:PROJECT.md chưa; có chỗ nào tin dữ liệu từ trình duyệt; thiếu @Transactional hoặc kiểm tra quyền; thiếu test cho ca biên; code khó hiểu với người mới. Liệt kê vấn đề theo mức độ, đề xuất sửa, CHƯA sửa cho tới khi em đồng ý.
```

### D. Prompt khi mở cuộc trò chuyện mới giữa chừng

```text
Đọc #file:PROJECT.md và #file:.github/copilot-instructions.md. Sau đó xem cấu trúc dự án hiện tại và git log để tóm tắt: đã làm xong những bước nào trong mục 13 của PROJECT.md, bước nào đang dở, test nào đang đỏ. CHƯA sửa gì; chỉ báo cáo.
```
