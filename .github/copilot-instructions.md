# Hướng dẫn cho GitHub Copilot – dự án StayBook

StayBook là web đặt phòng cho MỘT khách sạn/nhà nghỉ nhỏ ở Việt Nam (khách tự đặt, lễ tân xử lý, chủ quản trị).
Mọi quy tắc nghiệp vụ nằm trong `PROJECT.md` (ở thư mục gốc). Luôn đọc `PROJECT.md` trước khi làm.

## Cách làm việc

- Không tự bịa quy tắc nghiệp vụ hay tính năng. Nếu `PROJECT.md` thiếu hoặc mâu thuẫn, hãy HỎI lại thay vì đoán.
- Chỉ làm đúng phạm vi của yêu cầu hiện tại. Không refactor lớn hay đổi cấu trúc khi chưa được yêu cầu.
- Sau khi sửa code, chạy `mvn -q test` (hoặc `./mvnw -q test`) và sửa cho tới khi qua. Có thay đổi giao diện thì chạy ứng dụng để kiểm tra.
- Khi xong, luôn báo cáo gọn: (1) file đã tạo/sửa, (2) cách kiểm tra thủ công, (3) việc chưa làm, (4) giả định hoặc rủi ro.
- Sinh viên phải giải thích được code này. Viết đơn giản, đặt tên rõ nghĩa, comment bằng tiếng Việt cho các quy tắc nghiệp vụ. Tránh kỹ thuật cầu kỳ (không reactive, không CQRS, không event sourcing, không generic quá mức).
- Không thêm thư viện mới nếu không nêu lý do. Không dùng Lombok. Không dùng framework JavaScript (React, Vue…); JavaScript thuần chỉ ở mức tối thiểu.

## Kiến trúc

- Java 17, Spring Boot 3.x, Maven, Thymeleaf, Spring Data JPA, Spring Security, Bean Validation, Spring Mail. CSDL: MySQL (chạy thật) và H2 (test).
- Tổ chức package theo tính năng (xem `PROJECT.md` mục 3). Luồng: Controller → Service → Repository.
- Controller mỏng: nhận form, gọi service, trả view. Mọi quy tắc nghiệp vụ nằm trong service.
- Dùng lớp Form/DTO riêng cho dữ liệu nhập; không bind trực tiếp từ form vào entity.
- Mọi thay đổi trạng thái đặt phòng đi qua MỘT chỗ duy nhất (`BookingService` + bảng chuyển trạng thái) và ghi `BookingEvent`.
- Service thay đổi dữ liệu dùng `@Transactional`. Truy vấn dùng JPA/JPQL có tham số, không nối chuỗi SQL.

## Quy ước code

- Tên lớp, biến, bảng, API: tiếng Anh. Chuỗi hiển thị cho người dùng: tiếng Việt, đặt trong `messages.properties`, không viết cứng trong template hoặc service.
- Tiền: kiểu `long`, đơn vị đồng (VND). Hiển thị dạng `1.250.000 ₫` bằng một hàm định dạng dùng chung. Không dùng `double`/`float` cho tiền.
- Ngày: `LocalDate` cho ngày nhận/trả; `LocalDateTime` cho thời điểm. Múi giờ `Asia/Ho_Chi_Minh`.
- Lấy "bây giờ" từ bean `Clock`, không gọi `LocalDateTime.now()` trực tiếp trong nghiệp vụ (để test được).
- Khoảng ở là nửa mở: `[ngày nhận, ngày trả)`. Hai khoảng chồng lấn khi `a.checkIn < b.checkOut && b.checkIn < a.checkOut`.
- Dùng enum cho trạng thái; không dùng chuỗi tự do hay số ma thuật. Hằng số cấu hình (tỷ lệ cọc, thời gian giữ chỗ…) đọc từ bảng cài đặt, không viết cứng.

## Bảo mật (bắt buộc)

- Không tin dữ liệu từ trình duyệt: giá, tiền cọc, số tiền hoàn, trạng thái, vai trò luôn tính lại ở máy chủ.
- Phân quyền ở cấp URL (`/staff/**` cho STAFF và ADMIN, `/admin/**` cho ADMIN) VÀ ở cấp service. Khách chỉ truy cập được đặt phòng của chính mình (kiểm tra chủ sở hữu, không dùng id đoán được mà không kiểm tra).
- Mật khẩu băm BCrypt. Giới hạn đăng nhập sai. Bật CSRF cho mọi form POST. Cookie phiên HttpOnly và SameSite.
- Chống XSS: dùng `th:text`, không dùng `th:utext` với dữ liệu người dùng nhập.
- Tải ảnh: kiểm tra loại tệp bằng nội dung (không chỉ đuôi), giới hạn dung lượng, đổi tên tệp, không cho thực thi.
- Chuyển hướng sau đăng nhập chỉ tới đường dẫn nội bộ. Không để lộ stack trace cho người dùng.
- Không ghi mật khẩu, khóa bí mật vào mã nguồn hay commit. Dùng biến môi trường và `application-local.properties` (đã nằm trong `.gitignore`).

## Giao diện (UX)

- Ưu tiên điện thoại (Bootstrap 5, responsive). Tiếng Việt dễ hiểu, không hiện lỗi kỹ thuật.
- Luồng đặt phòng 4 bước có thanh tiến trình: Chọn phòng → Thông tin → Chuyển cọc → Hoàn tất. Luôn hiện tổng tiền, số đêm, chính sách hủy trước khi bấm đặt.
- Lỗi hiện ngay cạnh ô nhập. Thao tác khó hoàn tác (hủy, không đến, trả phòng) có hộp thoại xác nhận nêu rõ hậu quả.
- Trạng thái đặt phòng dùng cùng một fragment huy hiệu (tên + màu + chữ), không chỉ dùng màu.
- Trang trống/không có kết quả phải có câu hướng dẫn bước tiếp theo.

## Kiểm thử

- Mọi quy tắc tính toán và chuyển trạng thái phải có unit test (JUnit 5). Bắt buộc có test: tính giá, chồng lấn ngày (kể cả đặt liền kề), hoàn cọc đúng mốc 48 giờ, chuyển trạng thái hợp lệ/không hợp lệ, đặt phòng đồng thời, hết hạn giữ chỗ.
- Test dùng H2 và `Clock` cố định. Không để test phụ thuộc ngày hiện tại thật.

## Git

- Gợi ý commit nhỏ theo từng giai đoạn, thông điệp tiếng Việt rõ nghĩa (ví dụ: "Thêm tính giá theo đêm và test").
