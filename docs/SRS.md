# TÀI LIỆU ĐẶC TẢ YÊU CẦU PHẦN MỀM (SRS)

**HỆ THỐNG ĐẶT PHÒNG TRỰC TUYẾN CHO KHÁCH SẠN/NHÀ NGHỈ NHỎ (StayBook)**

## 1. Giới thiệu

### 1.1. Mục đích

Hệ thống hỗ trợ **khách** xem phòng trống và **đặt phòng trực tuyến**, đồng thời hỗ trợ **nhân viên lễ tân và chủ cơ sở** quản lý đặt phòng, nhận/trả phòng, thu tiền và theo dõi công suất, doanh thu. Hệ thống dựa trên loại phòng, phòng cụ thể, giá theo từng đêm và chính sách cọc/hủy để tính tiền, chống đặt trùng và quản lý vòng đời của mỗi đặt phòng.

Tài liệu này cũng là “đề bài” để giao cho AI Agent: nội dung được dùng làm nguồn cho tệp PROJECT.md của dự án.

### 1.2. Phạm vi

Hệ thống quản lý trọn vòng đời đặt phòng của **một cơ sở lưu trú**:

*Cấu hình phòng và giá → Khách tìm phòng trống → Đặt phòng và giữ chỗ → Chuyển cọc → Nhân viên xác nhận → Nhận phòng → Trả phòng và thanh toán → Báo cáo.*

**Ngoài phạm vi:** thanh toán trực tuyến tự động qua cổng thanh toán (chủ cơ sở xác nhận cọc thủ công); đồng bộ với nền tảng đặt phòng bên thứ ba; nhiều cơ sở hoặc nhiều chủ trên cùng hệ thống; xác thực email khi đăng ký; ứng dụng di động riêng; khai báo lưu trú với cơ quan chức năng; gia hạn thêm đêm (nhân viên tạo đặt phòng mới cho các đêm kế tiếp nếu còn phòng); quản lý dọn phòng, kho, nhân sự, kế toán đầy đủ; đa ngôn ngữ và đa tiền tệ.

### 1.3. Thuật ngữ

| **Thuật ngữ** | **Ý nghĩa** |
| --- | --- |
| Loại phòng | Nhóm phòng có cùng đặc điểm và giá (ví dụ Standard, Deluxe, Family). Khách chọn theo loại phòng. |
| Phòng | Một phòng cụ thể có số phòng (ví dụ 101) thuộc một loại phòng. |
| Đặt phòng | Một yêu cầu ở của khách: một loại phòng, một phòng được gán, từ ngày nhận đến ngày trả. |
| Đêm | Đêm của ngày D là đêm từ D đến D+1. Đặt phòng từ ngày nhận N đến ngày trả T gồm các đêm N, N+1, …, T−1. |
| Giữ chỗ | Trạng thái phòng đang được dành cho một đặt phòng chưa hoàn tất, không bán cho người khác. |
| Cọc | Khoản tiền khách chuyển trước để đảm bảo đặt phòng. |
| Mùa giá | Khoảng ngày có giá nền khác giá cơ bản (cao điểm, lễ). |
| Phụ phí | Khoản phát sinh khi ở (nước uống, giặt ủi…), ghi nhận lúc trả phòng. |
| Sổ thanh toán | Danh sách các khoản thu và hoàn tiền của một đặt phòng. |
| Không đến (no-show) | Khách đã đặt và xác nhận nhưng không nhận phòng. |
| Công suất phòng | Tỷ lệ đêm-phòng đã bán trên tổng đêm-phòng có thể bán trong một khoảng thời gian. |

## 2. Đối tượng sử dụng

### 2.1. Khách

- Tìm phòng trống, xem chi tiết loại phòng mà không cần đăng nhập.
- Đăng ký, đăng nhập, đặt phòng, chuyển cọc, xem và hủy đặt phòng của mình.
- Đánh giá sau khi ở.

### 2.2. Nhân viên (lễ tân)

- Xem và xử lý mọi đặt phòng: xác nhận cọc, đặt phòng hộ khách, nhận phòng, trả phòng, hủy, đánh dấu không đến, đổi phòng.
- Ghi nhận phụ phí và thanh toán; in hóa đơn; xem sơ đồ phòng.
- Không được thay đổi giá, loại phòng, chính sách và tài khoản.

### 2.3. Quản trị viên (chủ cơ sở)

- Có mọi quyền của nhân viên.
- Quản lý loại phòng, phòng, bảng giá, chính sách, tài khoản nhân viên; xem báo cáo và nhật ký thao tác.

### 2.4. Hệ thống (tiến trình nền)

- Tự động hủy giữ chỗ hết hạn, gửi email nhắc trước ngày nhận phòng.

## 3. Quy tắc nghiệp vụ

### 3.1. Bảng quy tắc

Các giá trị dưới đây là **giá trị mặc định đề xuất**, lưu trong cấu hình hệ thống; quản trị viên chỉnh được các mục đánh dấu ở FR-10.

| **Mã** | **Quy tắc** | **Giá trị mặc định** |
| --- | --- | --- |
| BR-01 | Ngày nhận không ở quá khứ; ngày trả sau ngày nhận; số đêm mỗi đặt phòng tối thiểu và tối đa | 1 đến 30 đêm |
| BR-02 | Giờ nhận phòng và giờ trả phòng tiêu chuẩn (chỉ để hiển thị và tính hạn hủy, không tính tiền theo giờ) | 14:00 / 12:00 |
| BR-03 | Tổng số khách (người lớn và trẻ em) không vượt sức chứa tối đa của loại phòng | Theo loại phòng |
| BR-04 | Hai đặt phòng đang giữ chỗ không được chồng lấn đêm trên cùng một phòng. Khoảng ở là \[ngày nhận, ngày trả): ngày trả của đặt phòng này trùng ngày nhận của đặt phòng kia là hợp lệ | Cố định |
| BR-05 | Các trạng thái giữ chỗ: Chờ cọc, Chờ xác nhận cọc, Đã xác nhận, Đã nhận phòng. Các trạng thái còn lại giải phóng phòng | Cố định |
| BR-06 | Thời gian giữ chỗ khi ở trạng thái Chờ cọc | 30 phút |
| BR-07 | Tỷ lệ tiền cọc trên tổng tiền phòng, làm tròn lên đến 1.000 đồng | 30% |
| BR-08 | Giá một đêm = giá nền + phụ thu cuối tuần. Giá nền là giá theo mùa nếu đêm thuộc một mùa giá, ngược lại là giá cơ bản của loại phòng | Xem 3.2 |
| BR-09 | Phụ thu cuối tuần áp dụng cho đêm Thứ 6 và đêm Thứ 7, tính theo % giá nền (đặt riêng cho từng loại phòng) | 10% |
| BR-10 | Các khoảng mùa giá của cùng một loại phòng không chồng lấn; khoảng mùa tính cả hai ngày đầu và cuối | Cố định |
| BR-11 | Chính sách hủy theo thời điểm hủy so với giờ nhận phòng của ngày nhận | Hoàn cọc nếu hủy trước 48 giờ (xem 3.3) |
| BR-12 | Chỉ cho nhận phòng khi đã có khoản cọc được xác nhận, hoặc nhân viên đã ghi nhận đủ số cọc thu tại quầy | Cố định |
| BR-13 | Chỉ nhận phòng từ ngày nhận trở đi; chỉ trả phòng sau khi đã nhận phòng; trả phòng sớm không hoàn tiền các đêm chưa ở | Cố định |
| BR-14 | Phòng ở trạng thái Bảo trì, Ngừng hoạt động hoặc đang bị khóa theo ngày không được gán cho đặt phòng trong khoảng bị khóa | Cố định |
| BR-15 | Mã đặt phòng là duy nhất, dạng BK-yyMMdd-XXXX (yyMMdd là ngày tạo, XXXX là 4 ký tự chữ hoa hoặc số ngẫu nhiên) | Cố định |
| BR-16 | Giá từng đêm được chốt khi tạo đặt phòng; thay đổi bảng giá sau đó không ảnh hưởng đặt phòng đã tạo | Cố định |
| BR-17 | Sổ thanh toán chỉ thêm, không sửa hoặc xóa; sai sót được xử lý bằng khoản điều chỉnh | Cố định |
| BR-18 | Đặt phòng ở Chờ xác nhận cọc quá thời hạn dưới đây mà chưa được xử lý thì được tô cảnh báo trên bảng việc, không tự hủy | 24 giờ |
| BR-19 | Mật khẩu tối thiểu; khóa tạm sau số lần đăng nhập sai liên tiếp; liên kết đặt lại mật khẩu hết hạn sau | 8 ký tự; 5 lần, 15 phút; 30 phút |
| BR-20 | Khách chỉ xem và hủy đặt phòng của chính mình; nhân viên xử lý mọi đặt phòng; chỉ quản trị viên cấu hình phòng, giá, chính sách, tài khoản nhân viên và xem báo cáo | Cố định |
| BR-21 | Loại phòng và phòng đã có lịch sử đặt không xóa cứng mà chuyển sang Ngừng hoạt động | Cố định |
| BR-22 | Chỉ đánh giá sau khi đặt phòng ở trạng thái Đã trả phòng, mỗi đặt phòng một lần | Cố định |
| BR-23 | Ảnh phòng chỉ nhận JPG/PNG, dung lượng và số ảnh tối đa mỗi loại phòng | 2 MB; 5 ảnh |
| BR-24 | Mỗi tài khoản khách chỉ được có tối đa số đặt phòng đồng thời ở trạng thái Chờ cọc hoặc Chờ xác nhận cọc (chống giữ phòng ảo) | 3 đặt phòng |
| BR-25 | Giá, tiền cọc, số tiền hoàn, trạng thái và quyền luôn được tính lại và kiểm tra ở máy chủ; không dùng giá trị do trình duyệt gửi lên | Cố định |
| BR-26 | Khi nhận phòng nhân viên có thể ghi loại giấy tờ và 4 số cuối; hệ thống không lưu toàn bộ số giấy tờ hay ảnh giấy tờ (giảm rủi ro dữ liệu cá nhân) | Cố định |

### 3.2. Cách tính giá

Giá một đêm = giá nền + phụ thu cuối tuần (BR-08, BR-09). Tổng tiền phòng là tổng giá các đêm. Tiền cọc = tổng tiền phòng × 30%, làm tròn lên đến 1.000 đồng (BR-07).

**Ví dụ 1.** Phòng Deluxe có giá cơ bản 800.000 ₫, phụ thu cuối tuần 10% (80.000 ₫). Khách nhận Thứ 6 06/11/2026, trả Thứ 2 09/11/2026:

| **Đêm** | **Thứ** | **Giá nền** | **Phụ thu** | **Giá đêm** |
| --- | --- | --- | --- | --- |
| 06/11 | Thứ 6 | 800.000 ₫ | 80.000 ₫ | 880.000 ₫ |
| 07/11 | Thứ 7 | 800.000 ₫ | 80.000 ₫ | 880.000 ₫ |
| 08/11 | Chủ nhật | 800.000 ₫ | 0 ₫ | 800.000 ₫ |

Tổng tiền phòng 2.560.000 ₫; tiền cọc 768.000 ₫.

**Ví dụ 2 (có mùa giá).** Mùa “Cao điểm cuối năm” từ 20/12 đến 05/01 có giá nền 1.000.000 ₫. Khách nhận Thứ 7 19/12/2026, trả Thứ 3 22/12/2026: đêm 19/12 (Thứ 7, ngoài mùa) = 800.000 + 80.000 = 880.000 ₫; đêm 20/12 (Chủ nhật, trong mùa) = 1.000.000 ₫; đêm 21/12 (Thứ 2, trong mùa) = 1.000.000 ₫. Tổng 2.880.000 ₫.

### 3.3. Chính sách hủy

“Giờ nhận phòng” là ngày nhận phòng lúc 14:00 (BR-02).

| **Tình huống** | **Xử lý tiền cọc** |
| --- | --- |
| Hủy khi chưa có khoản cọc (Chờ cọc) | Không phát sinh tiền |
| Khách hủy từ 48 giờ trước giờ nhận phòng trở lên | Hoàn 100% tiền cọc |
| Khách hủy trong vòng 48 giờ trước giờ nhận phòng | Không hoàn cọc |
| Khách không đến | Không hoàn cọc; phòng được trả lại cho các đêm còn lại |
| Cơ sở hủy (bảo trì, sự cố) | Hoàn 100% tiền cọc |
| Hết hạn giữ chỗ (không báo chuyển cọc kịp thời) | Không phát sinh tiền |

Việc hoàn tiền thực hiện thủ công bằng chuyển khoản; hệ thống ghi nhận khoản hoàn trong sổ thanh toán và hiển thị số tiền “Cần hoàn” cho đến khi nhân viên ghi nhận đã hoàn.

## 4. Yêu cầu chức năng

Mỗi yêu cầu có nhãn ưu tiên: **Bắt buộc** (phải có để ứng dụng dùng được), **Nên có** (làm sau khi xong nhóm Bắt buộc), **Nếu còn thời gian**.

### 4.1. Tài khoản và phân quyền

**FR-01. Đăng ký tài khoản khách (Bắt buộc).** Người dùng nhập: họ và tên, email, số điện thoại (10 chữ số, bắt đầu bằng 0), mật khẩu và nhập lại mật khẩu (BR-19). Email chưa tồn tại thì tạo tài khoản vai trò Khách ở trạng thái Hoạt động và đăng nhập được ngay. Nếu email đã tồn tại, hiển thị:

*“**Email đã được sử dụng.**”*

**FR-02. Đăng nhập và đăng xuất (Bắt buộc).** Đăng nhập bằng email và mật khẩu; sau khi đăng nhập, khách về trang chủ, nhân viên và quản trị viên vào trang quản lý. Nếu sai, hiển thị *“**Email hoặc mật khẩu không chính xác.**”* Sai liên tiếp theo BR-19 thì khóa tạm với thông báo *“**Tài khoản tạm khóa do đăng nhập sai nhiều lần. Vui lòng thử lại sau 15 phút.**”* Tài khoản bị quản trị viên khóa hiển thị *“**Tài khoản đã bị khóa. Vui lòng liên hệ cơ sở.**”*

**FR-03. Quên/đổi mật khẩu và hồ sơ cá nhân (Nên có).** Đặt lại mật khẩu qua liên kết gửi về email (hết hạn theo BR-19); đổi mật khẩu khi đã đăng nhập (phải nhập mật khẩu cũ); cập nhật họ tên và số điện thoại.

**FR-04. Quản lý tài khoản nhân viên và khách (Nên có).** Quản trị viên tạo tài khoản nhân viên (họ tên, email, số điện thoại, mật khẩu tạm), khóa/mở khóa tài khoản, đặt lại mật khẩu; xem danh sách tài khoản khách, tìm theo tên, email, số điện thoại. Quản trị viên không tự khóa hoặc hạ quyền chính mình.

### 4.2. Loại phòng và phòng

**FR-05. Quản lý loại phòng (Bắt buộc).** Quản trị viên thêm/sửa loại phòng: tên, mô tả, sức chứa tối đa (1 đến 10 khách), diện tích (m²), loại giường, giá cơ bản mỗi đêm (lớn hơn 0), phụ thu cuối tuần (%), tiện nghi (chọn từ danh sách: Wi-Fi, điều hòa, TV, tủ lạnh, bồn tắm, ban công, bữa sáng, chỗ đỗ xe…), ảnh (BR-23), trạng thái Hoạt động/Ngừng hoạt động. Loại phòng Ngừng hoạt động không xuất hiện khi tìm phòng; các đặt phòng đã có vẫn được giữ (BR-21).

**FR-06. Quản lý phòng (Bắt buộc).** Thêm/sửa phòng: số phòng (không trùng), tầng, loại phòng, trạng thái Hoạt động / Bảo trì / Ngừng hoạt động. Không chuyển sang Bảo trì hoặc Ngừng hoạt động khi phòng còn đặt phòng giữ chỗ từ hôm nay trở đi; hệ thống liệt kê các đặt phòng đó để nhân viên đổi phòng hoặc hủy trước.

**FR-07. Khóa phòng theo ngày (Nên có).** Chặn một phòng trong khoảng ngày (từ ngày, đến ngày tính cả hai đầu, lý do) để bảo trì hoặc giữ cho chủ. Không tạo được nếu khoảng khóa chồng lên đặt phòng giữ chỗ; khi đó liệt kê các đặt phòng bị ảnh hưởng. Các khoảng khóa của cùng một phòng không chồng nhau; có thể xóa khoảng khóa.

### 4.3. Giá và chính sách

**FR-08. Giá cơ bản và phụ thu cuối tuần (Bắt buộc).** Hệ thống tính tiền theo mục 3.2 từ giá cơ bản và phụ thu cuối tuần của từng loại phòng. Phần tính giá là một hàm riêng, dùng chung cho tìm phòng, đặt phòng và hóa đơn, có test tự động.

**FR-09. Giá theo mùa (Nên có).** Quản trị viên thêm/sửa/xóa mùa giá cho từng loại phòng: tên mùa, từ ngày, đến ngày, giá nền. Khoảng ngày không được chồng lấn (BR-10), nếu chồng thì hiển thị *“**Khoảng ngày bị trùng với mùa giá đã có.**”* Thay đổi giá không ảnh hưởng đặt phòng cũ (BR-16).

**FR-10. Cấu hình chính sách và thông tin cơ sở (Nên có).** Quản trị viên chỉnh: tỷ lệ cọc (BR-07), thời gian giữ chỗ (BR-06), giờ nhận/trả (BR-02), mốc hủy hoàn cọc (BR-11), tên, địa chỉ và số điện thoại cơ sở, thông tin tài khoản nhận cọc (ngân hàng, số tài khoản, chủ tài khoản). Nếu chưa chỉnh thì dùng giá trị mặc định ở mục 3.1.

### 4.4. Tìm kiếm phòng

**FR-11. Tìm phòng trống (Bắt buộc).** Không cần đăng nhập. Người dùng nhập ngày nhận, ngày trả, số khách. Hệ thống kiểm tra BR-01 và hiển thị lỗi nếu sai: *“**Ngày trả phải sau ngày nhận.**”*, *“**Ngày nhận không được ở quá khứ.**”*, *“**Tối đa 30 đêm cho mỗi lần đặt.**”* Kết quả liệt kê các loại phòng Hoạt động có sức chứa đủ số khách và còn ít nhất một phòng trống trọn khoảng ngày (phòng Hoạt động, không bị khóa, không có đặt phòng giữ chỗ chồng lấn theo BR-04), mỗi loại gồm: ảnh đại diện, tên, sức chứa, tiện nghi chính, số phòng còn lại, giá trung bình mỗi đêm và tổng giá cho cả kỳ ở. Nếu không có kết quả: *“**Không còn phòng phù hợp trong các ngày bạn chọn.**”*

**FR-12. Lọc và sắp xếp kết quả (Nên có).** Lọc theo khoảng giá, tiện nghi, sức chứa tối thiểu; sắp xếp theo giá tăng/giảm hoặc sức chứa.

**FR-13. Trang chi tiết loại phòng (Bắt buộc).** Hiển thị thư viện ảnh, mô tả, tiện nghi, diện tích, loại giường, sức chứa, giá cơ bản và ghi chú phụ thu cuối tuần, chính sách cọc và hủy (lấy từ cấu hình), giờ nhận/trả phòng, đánh giá trung bình và nhận xét (nếu có, FR-17) và nút “Đặt phòng” kèm ngày đã chọn.

### 4.5. Đặt phòng của khách

**FR-14. Tạo đặt phòng và giữ chỗ (Bắt buộc).** Khách phải đăng nhập (nếu chưa thì chuyển tới đăng nhập rồi quay lại và giữ nguyên lựa chọn). Trang xác nhận hiển thị: loại phòng, ngày nhận, ngày trả, số khách, tên/số điện thoại/email liên hệ (lấy từ hồ sơ, sửa được), ghi chú (tối đa 500 ký tự), bảng giá từng đêm, tổng tiền phòng, tiền cọc và chính sách hủy áp dụng cho đặt phòng này (nếu ngày nhận cách thời điểm đặt dưới 48 giờ thì ghi rõ “Đặt phòng này sẽ không được hoàn cọc nếu hủy” và khách phải tích ô xác nhận đã đọc). Khi bấm “Đặt phòng”, hệ thống làm trong **một giao dịch dữ liệu**: (1) kiểm tra lại BR-01, BR-03 và BR-24; (2) khóa bản ghi của loại phòng (để các lần đặt cùng loại được xử lý lần lượt) rồi tìm các phòng trống của loại đó; (3) chọn phòng có số nhỏ nhất; (4) nếu không còn phòng thì không tạo đặt phòng và báo lỗi; (5) lưu đặt phòng ở trạng thái Chờ cọc với mã đặt (BR-15), giá từng đêm (BR-16), tiền cọc (BR-07) và hạn giữ chỗ (BR-06). Số phòng cụ thể chỉ hiển thị cho khách sau khi đặt phòng được xác nhận. Khi hết phòng giữa chừng hiển thị:

*“**Loại phòng này vừa hết phòng trong các ngày bạn chọn. Vui lòng chọn ngày hoặc loại phòng khác.**”*

**FR-15. Chuyển cọc qua VietQR (Bắt buộc).** Sau khi đặt, trang thanh toán cọc hiển thị: mã đặt phòng, số tiền cọc, mã VietQR (số tiền và nội dung chuyển khoản là mã đặt phòng), thông tin tài khoản nhận cọc (FR-10) và đồng hồ đếm ngược giữ chỗ. Nếu không tạo được ảnh QR thì hiển thị số tài khoản và nội dung chuyển khoản bằng chữ. Khách bấm “Tôi đã chuyển khoản” thì đặt phòng sang Chờ xác nhận cọc và vẫn giữ phòng, kèm thông báo *“**Cơ sở sẽ đối chiếu và xác nhận trong thời gian sớm nhất.**”* Nếu quá hạn giữ chỗ trước khi bấm, hiển thị *“**Đặt phòng đã hết hạn giữ chỗ. Vui lòng đặt lại.**”*

**FR-16. Xem và hủy đặt phòng của tôi (Bắt buộc).** Danh sách đặt phòng của khách (mới nhất trước) kèm trạng thái; trang chi tiết gồm các đêm và giá, tiền cọc, các khoản thanh toán, hạn giữ chỗ, số phòng (sau khi xác nhận). Khách hủy được khi đặt phòng ở Chờ cọc, Chờ xác nhận cọc hoặc Đã xác nhận; hệ thống hiển thị trước số tiền được hoàn theo BR-11 và yêu cầu xác nhận. Nếu hủy khi đang Chờ xác nhận cọc, đặt phòng chuyển Đã hủy và được đánh dấu để nhân viên đối chiếu, hoàn tiền thủ công nếu đã nhận tiền. Không hủy được khi đã nhận phòng hoặc đã trả phòng.

**FR-17. Đánh giá sau khi ở (Nếu còn thời gian).** Khách chấm 1 đến 5 sao và viết nhận xét (tối đa 1.000 ký tự) cho đặt phòng ở trạng thái Đã trả phòng (BR-22). Đánh giá hiển thị trên trang chi tiết loại phòng (điểm trung bình và số lượt); quản trị viên có thể ẩn/hiện nhận xét.

### 4.6. Quản lý đặt phòng (nhân viên)

**FR-18. Bảng việc và danh sách đặt phòng (Bắt buộc).** Trang “Hôm nay” gom các nhóm: nhận phòng hôm nay, trả phòng hôm nay, đang ở, chờ xác nhận cọc (tô cảnh báo nếu quá thời hạn BR-18), chờ cọc sắp hết hạn. Danh sách đầy đủ lọc theo trạng thái, ngày nhận, ngày trả, loại phòng; tìm theo mã đặt, tên khách, số điện thoại; phân trang 20 dòng. Trang chi tiết gồm thông tin khách, phòng, các đêm và giá, thanh toán và lịch sử chuyển trạng thái (ai làm, lúc nào, lý do).

**FR-19. Xác nhận hoặc từ chối cọc (Bắt buộc).** Với đặt phòng ở Chờ xác nhận cọc, nhân viên đối chiếu sao kê rồi nhập số tiền thực nhận và hình thức (chuyển khoản/tiền mặt). Nếu số tiền từ mức cọc trở lên thì đặt phòng chuyển Đã xác nhận, hệ thống ghi khoản Cọc vào sổ thanh toán (FR-26) và gửi email xác nhận (FR-29). Nếu thấp hơn thì không cho xác nhận và hiển thị *“**Số tiền nhận chưa đủ tiền cọc.**”* Từ chối thì nhân viên nhập lý do, đặt phòng chuyển Đã hủy; nếu khách đã chuyển tiền, nhân viên hoàn lại và ghi nhận trong sổ thanh toán.

**FR-20. Đặt phòng hộ khách (Nên có).** Nhân viên nhập thông tin khách (tên, số điện thoại, email không bắt buộc), tìm phòng như FR-11 và đặt theo FR-14, có cùng cơ chế chống trùng. Nhân viên chọn thu cọc ngay (nhập số tiền từ mức cọc trở lên và hình thức) thì đặt phòng vào thẳng Đã xác nhận; nếu chưa thu thì ở Chờ cọc theo BR-06. Đặt hộ không cần tài khoản khách.

**FR-21. Nhận phòng (Bắt buộc).** Chỉ áp dụng cho đặt phòng Đã xác nhận (BR-12). Nếu ngày hiện tại chưa đến ngày nhận thì từ chối với *“**Chưa đến ngày nhận phòng.**”* (BR-13). Nếu đặt phòng chưa được xác nhận cọc thì hiển thị *“**Đặt phòng chưa được xác nhận cọc.**”* và nhân viên xử lý qua FR-19 hoặc ghi nhận thu cọc tại quầy trước. Khi nhận phòng, nhân viên có thể ghi loại giấy tờ và 4 số cuối (BR-26); hệ thống ghi thời điểm thực tế và ghi chú, chuyển Đã nhận phòng. Đặt phòng đã bị đánh dấu Không đến thì không nhận phòng lại được; nhân viên tạo đặt phòng mới nếu còn phòng.

**FR-22. Trả phòng, phụ phí và thanh toán còn lại (Bắt buộc).** Áp dụng cho đặt phòng Đã nhận phòng. Nhân viên thêm các khoản phụ phí (tên khoản, số tiền lớn hơn 0). Hệ thống hiển thị: tổng tiền phòng + tổng phụ phí − số đã thu = số còn phải thu. Nhân viên ghi nhận khoản Thanh toán thêm (số tiền, hình thức). Chỉ hoàn tất khi số còn phải thu bằng 0, nếu chưa thì hiển thị *“**Còn phải thu … ₫.**”* Hoàn tất thì đặt phòng chuyển Đã trả phòng và ghi thời điểm. Trả phòng sớm hơn ngày trả dự kiến vẫn được ghi nhận nhưng không hoàn tiền các đêm chưa ở (BR-13). Ví dụ: tiền phòng 2.560.000 ₫, đã cọc 768.000 ₫, phụ phí 150.000 ₫ (nước uống, giặt ủi) thì còn phải thu 1.942.000 ₫.

**FR-23. Hủy và đánh dấu không đến (Bắt buộc).** *Hủy:* với đặt phòng chưa nhận phòng, nhân viên nhập lý do và chọn nguyên nhân “Khách yêu cầu” (hoàn cọc theo BR-11 tính tại thời điểm hủy) hoặc “Cơ sở hủy” (hoàn 100% cọc); hệ thống hiển thị số tiền cần hoàn. *Không đến:* với đặt phòng Đã xác nhận, nhân viên chỉ đánh dấu được từ 00:00 ngày kế tiếp ngày nhận; cọc được giữ lại và các đêm còn lại được trả về cho phòng trống.

**FR-24. Đổi phòng (Nên có).** Với đặt phòng Đã xác nhận hoặc Đã nhận phòng, nhân viên chọn một phòng khác **cùng loại** còn trống trong các đêm còn lại (từ hôm nay đến ngày trả nếu đang ở). Hệ thống kiểm tra BR-04 và BR-14, không đổi giá, ghi lịch sử. Nếu phòng mới đang có đặt phòng chồng lấn thì từ chối.

**FR-25. Sơ đồ phòng theo ngày (Nên có).** Bảng có hàng là từng phòng (nhóm theo loại), cột là 14 ngày kể từ ngày chọn. Mỗi ô tô màu theo tình trạng đêm đó: Trống, Giữ chỗ (Chờ cọc/Chờ xác nhận cọc), Đã xác nhận, Đang ở, Bảo trì/Khóa. Bấm vào ô có đặt phòng thì mở chi tiết đặt phòng. Tình trạng này được suy ra từ dữ liệu đặt phòng và khóa phòng, không lưu riêng.

### 4.7. Thanh toán và hóa đơn

**FR-26. Sổ thanh toán (Bắt buộc).** Mỗi đặt phòng có một sổ ghi các khoản: **Cọc**, **Thanh toán thêm**, **Hoàn tiền**, **Điều chỉnh**. Mỗi khoản có số tiền, hình thức (chuyển khoản/tiền mặt), thời điểm, người ghi, ghi chú. Khoản đã ghi không sửa hoặc xóa (BR-17); sai sót được xử lý bằng khoản Điều chỉnh có ghi chú. Hệ thống tính: *Đã thu thực tế* = Cọc + Thanh toán thêm + Điều chỉnh − Hoàn tiền; *Còn phải thu* = tổng tiền phòng + phụ phí − Đã thu thực tế; *Cần hoàn* (với đặt phòng Đã hủy) = số tiền hoàn theo BR-11 − tổng Hoàn tiền đã ghi, không âm.

**FR-27. Hóa đơn in được (Nên có).** Trang hóa đơn gồm: thông tin cơ sở (FR-10), mã đặt, thông tin khách, ngày nhận/trả, danh sách từng đêm và giá, phụ phí, tổng cộng, đã cọc, thanh toán thêm, còn lại; có nút In (người dùng có thể lưu thành PDF từ trình duyệt). Chỉ tạo được khi đặt phòng đã Đã trả phòng.

### 4.8. Thông báo và tác vụ nền

**FR-28. Tự động hết hạn giữ chỗ (Bắt buộc).** Tiến trình nền chạy mỗi phút, chuyển các đặt phòng Chờ cọc đã quá hạn giữ chỗ sang Hết hạn, nhờ đó phòng được trả lại cho tìm kiếm. Việc chuyển trạng thái kiểm tra trạng thái hiện tại trong cùng giao dịch, nên không sai khi khách bấm “Tôi đã chuyển khoản” đúng lúc hết hạn hoặc khi tiến trình chạy lặp. Có ghi lịch sử với người thực hiện là Hệ thống.

**FR-29. Email thông báo (Nên có).** Gửi email khi: (1) đã nhận đặt phòng (mã, hạn giữ chỗ, hướng dẫn chuyển cọc); (2) cọc được xác nhận (thông tin đặt phòng, chính sách hủy); (3) đặt phòng bị hủy (kèm số tiền hoàn nếu có); (4) hết hạn giữ chỗ; (5) nhắc trước ngày nhận phòng 1 ngày, lúc 09:00, cho đặt phòng Đã xác nhận; (6) cảm ơn sau khi trả phòng, kèm liên kết đánh giá (Nếu còn thời gian). Email lỗi thì thử lại tối đa 3 lần, cách nhau 5 phút; vẫn lỗi thì ghi nhật ký và không làm hỏng nghiệp vụ chính. Khi chưa cấu hình SMTP, email được ghi ra log.

### 4.9. Báo cáo và nhật ký

**FR-30. Báo cáo công suất và doanh thu (Nên có).** Quản trị viên chọn khoảng ngày (tối đa 366 ngày). *Công suất* mỗi ngày = số phòng có khách ở đêm đó (đặt phòng Đã xác nhận, Đã nhận phòng, Đã trả phòng) chia cho số phòng khả dụng đêm đó (Hoạt động và không bị khóa). *Doanh thu* theo ngày và theo tháng tính từ sổ thanh toán: Cọc + Thanh toán thêm + Điều chỉnh − Hoàn tiền. Hiển thị biểu đồ cùng tổng và trung bình. Ví dụ: 10 phòng khả dụng, 4 phòng có khách thì công suất 40%.

**FR-31. Thống kê đặt phòng (Nếu còn thời gian).** Số đặt phòng theo trạng thái; tỉ lệ hủy, không đến, hết hạn; loại phòng được đặt nhiều nhất; tỉ lệ khách tự đặt so với nhân viên đặt hộ.

**FR-32. Nhật ký thao tác (Nếu còn thời gian).** Ghi lại: đăng nhập thất bại, tạo/khóa tài khoản, thay đổi giá và chính sách, xác nhận/từ chối cọc, hủy, đổi phòng, ghi nhận thanh toán, lỗi gửi email. Quản trị viên xem và lọc theo thời gian, người thực hiện, loại thao tác.

## 5. Trạng thái

Tình trạng một đêm của phòng (trống, giữ chỗ, đang ở…) **được suy ra** từ dữ liệu đặt phòng và khóa phòng, không lưu thành trạng thái cố định. Mọi chuyển trạng thái đặt phòng được kiểm tra ở một nơi duy nhất trong mã nguồn và ghi vào lịch sử.

### 5.1. Trạng thái của đặt phòng

| **Trạng thái** | **Ý nghĩa** | **Giữ phòng** | **Chuyển được tới** |
| --- | --- | --- | --- |
| Chờ cọc | Mới đặt, đang giữ chỗ trong thời hạn BR-06, chờ khách chuyển cọc | Có | Chờ xác nhận cọc; Đã xác nhận (nhân viên thu cọc tại quầy); Đã hủy; Hết hạn |
| Chờ xác nhận cọc | Khách báo đã chuyển khoản, chờ nhân viên đối chiếu | Có | Đã xác nhận; Đã hủy |
| Đã xác nhận | Đã có cọc, chờ khách đến | Có | Đã nhận phòng; Đã hủy; Không đến |
| Đã nhận phòng | Khách đang ở | Có | Đã trả phòng |
| Đã trả phòng | Hoàn tất, đã thanh toán đủ | Không | (kết thúc) |
| Đã hủy | Hủy bởi khách hoặc nhân viên | Không | (kết thúc) |
| Hết hạn | Quá hạn giữ chỗ mà khách không báo chuyển cọc | Không | (kết thúc) |
| Không đến | Khách đã xác nhận nhưng không nhận phòng | Không | (kết thúc) |

### 5.2. Trạng thái của phòng

| **Trạng thái** | **Ý nghĩa** |
| --- | --- |
| Hoạt động | Có thể gán cho đặt phòng. |
| Bảo trì | Tạm không bán; chuyển được khi phòng không còn đặt phòng giữ chỗ từ hôm nay trở đi. |
| Ngừng hoạt động | Không bán nữa nhưng giữ lịch sử (BR-21). |

### 5.3. Loại khoản trong sổ thanh toán

| **Loại** | **Ý nghĩa** |
| --- | --- |
| Cọc | Tiền cọc nhận được khi xác nhận đặt phòng. |
| Thanh toán thêm | Tiền thu thêm (thu lúc trả phòng hoặc thu cọc bổ sung). |
| Hoàn tiền | Tiền hoàn lại cho khách (chi ra). |
| Điều chỉnh | Khoản sửa sai có ghi chú, có thể dương hoặc âm. |

### 5.4. Trạng thái của tài khoản

| **Trạng thái** | **Ý nghĩa** |
| --- | --- |
| Hoạt động | Sử dụng bình thường. |
| Bị khóa tạm thời | Đăng nhập sai quá số lần (BR-19), tự mở sau thời gian khóa. |
| Bị khóa | Do quản trị viên khóa, chỉ quản trị viên mới mở được. |

## 6. Dữ liệu chính

Danh sách thực thể để thiết kế CSDL (ERD) và giao cho agent dựng entity JPA. Quan hệ: 1 loại phòng – nhiều phòng, nhiều ảnh, nhiều mùa giá; 1 phòng – nhiều khoảng khóa; 1 đặt phòng – 1 phòng, nhiều đêm, nhiều phụ phí, nhiều khoản thanh toán, nhiều sự kiện trạng thái; 1 đặt phòng – 0 hoặc 1 đánh giá. Nên đánh chỉ mục cho (room\_id, ngày nhận, ngày trả, trạng thái) để kiểm tra chồng lấn nhanh.

| **Bảng** | **Trường chính** |
| --- | --- |
| users | id, họ tên, email, số điện thoại, mật khẩu băm, vai trò, trạng thái, số lần đăng nhập sai, khóa đến, tạo lúc |
| room\_types | id, tên, mô tả, sức chứa tối đa, diện tích, loại giường, giá cơ bản, phụ thu cuối tuần (%), trạng thái |
| amenities / room\_type\_amenities | danh sách tiện nghi và bảng nối với loại phòng |
| room\_type\_images | id, room\_type\_id, tên tệp, thứ tự |
| rooms | id, số phòng (duy nhất), tầng, room\_type\_id, trạng thái |
| room\_blocks | id, room\_id, từ ngày, đến ngày, lý do, người tạo |
| season\_prices | id, room\_type\_id, tên mùa, từ ngày, đến ngày, giá nền |
| bookings | id, mã đặt (duy nhất), user\_id (rỗng nếu đặt hộ), tên khách, số điện thoại, email, room\_type\_id, room\_id, ngày nhận, ngày trả, số khách, tổng tiền phòng, tiền cọc, trạng thái, hạn giữ chỗ, ghi chú, lý do và nguyên nhân hủy, nhận phòng lúc, trả phòng lúc, nguồn đặt (khách/nhân viên), giấy tờ (loại, 4 số cuối), tạo lúc |
| booking\_nights | id, booking\_id, ngày của đêm, giá đêm (đã chốt) |
| booking\_events | id, booking\_id, từ trạng thái, đến trạng thái, người thực hiện, lý do, thời điểm |
| surcharges | id, booking\_id, tên khoản, số tiền, người ghi, thời điểm |
| payments | id, booking\_id, loại, số tiền, hình thức, ghi chú, người ghi, thời điểm |
| reviews | id, booking\_id (duy nhất), user\_id, room\_type\_id, số sao, nhận xét, ẩn, tạo lúc |
| settings | khóa, giá trị (tỷ lệ cọc, thời gian giữ chỗ, giờ nhận/trả, thông tin cơ sở, tài khoản nhận cọc) |
| audit\_logs | id, user\_id, hành động, đối tượng, chi tiết, thời gian |

## 7. Yêu cầu phi chức năng

- **Bảo mật:** mật khẩu băm BCrypt; Spring Security phân quyền theo vai trò ở từng đường dẫn; mọi truy cập đặt phòng của khách phải kiểm tra chủ sở hữu (BR-20); chống CSRF trên biểu mẫu; dùng JPA và truy vấn tham số hóa; tải ảnh phải kiểm tra loại và dung lượng, đổi tên tệp, không cho chạy tệp tải lên; thông tin cá nhân của khách (tên, số điện thoại, email) chỉ nhân viên và quản trị viên xem được; không lưu thông tin thẻ; mật khẩu email và cấu hình nhạy cảm lưu trong biến môi trường, không để trong mã nguồn. Máy chủ luôn tự tính lại giá, tiền cọc, số tiền hoàn, trạng thái và quyền (BR-25), không tin giá trị do trình duyệt gửi lên; nội dung người dùng nhập (nhận xét, ghi chú) luôn được mã hóa khi hiển thị để chống XSS; sau đăng nhập chỉ chuyển hướng tới đường dẫn nội bộ; mã đặt lại mật khẩu chỉ lưu dạng băm, dùng một lần và có hạn; cookie phiên đặt cờ HttpOnly và SameSite; ảnh tải lên được kiểm tra cả nội dung tệp chứ không chỉ đuôi tệp; giới hạn số lần đăng nhập sai và số đặt phòng đang giữ chỗ (BR-19, BR-24); khi triển khai thật phải dùng HTTPS.
- **Đúng đắn và đồng thời:** tạo đặt phòng, đổi phòng, xác nhận cọc và hết hạn giữ chỗ chạy trong giao dịch dữ liệu; khóa bản ghi loại phòng khi gán phòng; có test hai luồng cùng đặt phòng cuối cùng thì chỉ một luồng thành công; tiền lưu bằng số nguyên (đồng) hoặc BigDecimal, không dùng số thực dấu phẩy động.
- **Hiệu năng:** tìm phòng trống trong vòng 2 giây với 100 phòng và 5.000 đặt phòng (có chỉ mục); hỗ trợ tối thiểu 50 người dùng đồng thời.
- **Tự động hóa:** quét hết hạn giữ chỗ mỗi phút; gửi email nhắc lúc 09:00 mỗi ngày.
- **Thông báo:** gửi qua email (Gmail); có chế độ giả lập ghi email ra log khi chưa cấu hình SMTP.
- **Giao diện:** ứng dụng web tiếng Việt, hiển thị tốt trên điện thoại (khách chủ yếu đặt bằng điện thoại) và trên máy tính; tiền định dạng 1.250.000 ₫; ngày định dạng dd/MM/yyyy; trạng thái và màu luôn có nhãn chữ đi kèm (không chỉ dùng màu). Nguyên tắc UX: (1) thiết kế ưu tiên điện thoại; (2) luồng đặt phòng chia 4 bước rõ ràng (Chọn phòng, Thông tin, Chuyển cọc, Hoàn tất) có thanh tiến trình; (3) luôn thấy tổng tiền, số đêm và chính sách hủy trước khi bấm đặt; (4) ngày nhận mặc định là hôm nay, ngày trả mặc định là hôm sau, ô chọn ngày không cho chọn ngày không hợp lệ; (5) lỗi hiển thị ngay cạnh ô nhập bằng tiếng Việt dễ hiểu, không hiện lỗi kỹ thuật; (6) thao tác khó hoàn tác (hủy, đánh dấu không đến, trả phòng) có hộp thoại xác nhận nêu rõ hậu quả; (7) tên và màu trạng thái đặt phòng thống nhất toàn hệ thống; (8) trang trống hoặc không có kết quả có thông điệp chỉ việc nên làm tiếp; (9) trang lễ tân ưu tiên bảng việc “Hôm nay”, nút lớn, ít bước; (10) ô nhập có nhãn và đủ độ tương phản.
- **Thời gian:** toàn hệ thống dùng múi giờ GMT+7; “ngày” nhận/trả tính theo múi giờ này.
- **Nhật ký:** ghi lại thao tác quan trọng (FR-32) và lịch sử chuyển trạng thái đặt phòng.
- **Bảo trì và kiểm thử:** có README hướng dẫn cài đặt và chạy bằng một lệnh; có dữ liệu mẫu (seed) để demo gồm 1 quản trị viên, 2 nhân viên, 2 khách, 4 loại phòng, 20 phòng, một mùa giá và khoảng 15 đặt phòng ở đủ các trạng thái; có test tự động tối thiểu cho: tính giá (cuối tuần, mùa, qua tháng), kiểm tra chồng lấn (kể cả đặt liền kề), hoàn cọc (đúng mốc 48 giờ, trước và sau mốc), chuyển trạng thái hợp lệ và không hợp lệ, đặt phòng đồng thời, hết hạn giữ chỗ.

## 8. Luồng hoạt động tổng quát

- Quản trị viên cấu hình: loại phòng, phòng, giá cơ bản, phụ thu cuối tuần, mùa giá, chính sách cọc/hủy, tài khoản nhân viên
- Khách vào website, nhập ngày nhận, ngày trả, số khách → xem các loại phòng còn trống và tổng giá
- Khách chọn loại phòng → đăng nhập hoặc đăng ký → kiểm tra thông tin → đặt phòng
- Hệ thống tự gán phòng, chốt giá từng đêm, tạo đặt phòng Chờ cọc và giữ chỗ 30 phút
- Khách chuyển cọc theo mã VietQR → bấm “Tôi đã chuyển khoản” → Chờ xác nhận cọc
- Nhân viên đối chiếu → xác nhận cọc → Đã xác nhận → email xác nhận gửi cho khách
- Quá hạn giữ chỗ mà khách chưa báo chuyển cọc → hệ thống chuyển Hết hạn và trả phòng về trạng thái trống
- Khách hủy (theo chính sách hoàn cọc) hoặc nhân viên hủy / đánh dấu không đến
- Ngày nhận: nhân viên làm thủ tục nhận phòng → Đã nhận phòng
- Ngày trả: nhân viên ghi phụ phí, thu phần còn lại, in hóa đơn → Đã trả phòng
- Khách đánh giá; quản trị viên xem báo cáo công suất, doanh thu

## 9. Tiêu chí nghiệm thu

Bảng dưới đây là kịch bản kiểm thử, dùng để nghiệm thu từng giai đoạn và đưa vào báo cáo kiểm thử. Các ngày trong ví dụ là ngày minh họa; khi kiểm thử có thể đổi sang ngày tương lai tương ứng.

| **Mã** | **Chức năng** | **Kịch bản** | **Kết quả mong đợi** |
| --- | --- | --- | --- |
| AC-01 | FR-01, FR-02 | Đăng ký bằng email đã tồn tại; sau đó đăng nhập sai mật khẩu 5 lần liên tiếp | Báo “Email đã được sử dụng”; sau lần sai thứ 5 tài khoản bị khóa 15 phút |
| AC-02 | FR-11 | Tìm phòng với ngày trả bằng ngày nhận; với ngày nhận ở quá khứ; với 31 đêm | Hiển thị đúng ba thông báo lỗi tương ứng (BR-01) |
| AC-03 | FR-08 | Phòng Deluxe giá 800.000 ₫, phụ thu cuối tuần 10%, nhận 06/11, trả 09/11 | Các đêm 880.000 ₫, 880.000 ₫, 800.000 ₫; tổng 2.560.000 ₫; cọc 768.000 ₫ |
| AC-04 | FR-09 | Mùa 20/12 đến 05/01 giá nền 1.000.000 ₫; đặt 19/12 đến 22/12; thử tạo thêm mùa chồng lấn | Tổng 2.880.000 ₫; mùa chồng lấn bị từ chối |
| AC-05 | BR-04, FR-11 | Phòng 101 là phòng duy nhất của loại, đã có đặt 06/11 đến 08/11. Thử đặt 08/11 đến 10/11, rồi 07/11 đến 09/11 | 08/11 đến 10/11 đặt được vào phòng 101 (liền kề); 07/11 đến 09/11 báo hết phòng |
| AC-06 | FR-14 | Hai yêu cầu cùng lúc đặt phòng cuối cùng của một loại (test hai luồng) | Chỉ một thành công; yêu cầu còn lại nhận thông báo hết phòng; không có hai đặt phòng chồng lấn trên cùng phòng |
| AC-07 | FR-28, BR-06 | Đặt phòng lúc 10:00 và không chuyển cọc | Lúc 10:31 đặt phòng ở Hết hạn; phòng xuất hiện lại khi tìm kiếm |
| AC-08 | FR-15, FR-19 | Khách bấm “Tôi đã chuyển khoản”; nhân viên xác nhận với số tiền thấp hơn cọc, rồi với số tiền đủ | Chờ xác nhận cọc; lần thiếu bị từ chối “Số tiền nhận chưa đủ tiền cọc”; lần đủ chuyển Đã xác nhận, sổ có khoản Cọc, khách nhận email |
| AC-09 | FR-16, BR-11 | Đặt phòng Đã xác nhận, nhận 06/11 14:00. Một khách hủy lúc 03/11 10:00; một đặt phòng khác hủy lúc 05/11 10:00 | Trường hợp đầu hiển thị cần hoàn 100% cọc (768.000 ₫); trường hợp sau không hoàn |
| AC-10 | FR-21 | Nhận phòng với đặt phòng chưa xác nhận cọc; nhận phòng trước ngày nhận | Báo “Đặt phòng chưa được xác nhận cọc” và “Chưa đến ngày nhận phòng”; sau khi xác nhận cọc và đến ngày thì nhận được |
| AC-11 | FR-22, FR-26 | Trả phòng: tiền phòng 2.560.000 ₫, cọc 768.000 ₫, phụ phí 150.000 ₫ | Còn phải thu 1.942.000 ₫; chưa ghi nhận đủ thì không hoàn tất; ghi đủ thì chuyển Đã trả phòng |
| AC-12 | FR-23 | Đánh dấu Không đến cho đặt phòng Đã xác nhận 06/11 đến 09/11 vào ngày 07/11 | Trạng thái Không đến; cọc được giữ; các đêm 07/11 và 08/11 trở lại trống |
| AC-13 | FR-24 | Đổi sang phòng cùng loại đang trống; thử đổi sang phòng có đặt phòng chồng lấn | Lần đầu đổi được và phòng cũ trống lại; lần sau bị từ chối |
| AC-14 | BR-16 | Đổi giá cơ bản của loại phòng sau khi đã có đặt phòng | Đặt phòng cũ giữ nguyên giá từng đêm; đặt phòng mới dùng giá mới |
| AC-15 | BR-20 | Khách A mở đường dẫn đặt phòng của khách B; khách mở trang quản lý; nhân viên mở trang cấu hình giá | Bị từ chối (403/404), không lộ dữ liệu |
| AC-16 | FR-30 | 10 phòng khả dụng, ngày X có 4 phòng có khách | Công suất ngày X là 40%; doanh thu khớp tổng các khoản trong sổ thanh toán |
| AC-17 | FR-05, BR-23 | Tải lên tệp .exe, ảnh 3 MB và ảnh thứ 6 cho một loại phòng | Cả ba bị từ chối kèm lý do |
| AC-18 | FR-06 | Chuyển sang Bảo trì một phòng đang có đặt phòng giữ chỗ trong tương lai | Bị từ chối và liệt kê các đặt phòng bị ảnh hưởng |
| AC-19 | BR-24 | Khách đang có 3 đặt phòng chờ cọc rồi đặt thêm đặt phòng thứ 4 | Bị từ chối với thông báo cần thanh toán hoặc hủy bớt đặt phòng đang chờ |
| AC-20 | BR-25 | Dùng công cụ trình duyệt sửa giá hoặc tiền cọc trong biểu mẫu rồi gửi đặt phòng | Máy chủ bỏ qua giá do trình duyệt gửi, dùng giá tự tính; đặt phòng lưu đúng giá và cọc theo quy tắc |
| AC-21 | FR-14, BR-11 | Đặt phòng có ngày nhận cách thời điểm đặt dưới 48 giờ | Trang xác nhận cảnh báo không hoàn cọc nếu hủy và bắt buộc tích ô xác nhận mới đặt được |

## 10. Đối chiếu với đặc tả vấn đề ban đầu

| **Yêu cầu gốc** | **Được đáp ứng ở** |
| --- | --- |
| Khách xem phòng trống theo ngày và số khách, thấy tổng giá | FR-11 đến FR-13 |
| Đăng ký, đăng nhập, phân quyền khách / nhân viên / quản trị viên | FR-01 đến FR-04; mục 2 |
| Đặt phòng, tự gán phòng, giữ chỗ 30 phút | FR-14; BR-04, BR-06 |
| Cọc 30% bằng VietQR, báo đã chuyển, nhân viên xác nhận | FR-15, FR-19; BR-07 |
| Tự động hết hạn giữ chỗ | FR-28 |
| Xem và hủy đặt phòng, hoàn cọc theo chính sách | FR-16; mục 3.3 |
| Nhận phòng, trả phòng, phụ phí, thu phần còn lại | FR-21, FR-22, FR-26 |
| Đặt hộ khách, đổi phòng, không đến | FR-20, FR-24, FR-23 |
| Sơ đồ phòng theo ngày | FR-25 |
| Hóa đơn | FR-27 |
| Giá cơ bản, phụ thu cuối tuần, giá theo mùa, cấu hình chính sách | FR-08 đến FR-10 |
| Quản lý loại phòng, phòng, khóa phòng | FR-05 đến FR-07 |
| Email xác nhận và nhắc | FR-29 |
| Báo cáo công suất, doanh thu, thống kê, nhật ký | FR-30 đến FR-32 |
| Đánh giá sau khi ở | FR-17 |

**Ghi chú giả định:** hệ thống phục vụ một cơ sở; một đặt phòng ứng với một phòng (đoàn nhiều phòng thì đặt nhiều lần); số khách tính chung người lớn và trẻ em; giờ nhận/trả là thông tin chuẩn, không tính tiền theo giờ; hoàn tiền thực hiện thủ công, hệ thống chỉ ghi nhận; đồng hồ máy chủ dùng múi giờ GMT+7.
