# SOFTWARE REQUIREMENTS SPECIFICATION (SRS)

## Hệ thống quản lý rạp chiếu phim và đặt vé Cinema

**Phiên bản:** 0.2 — Dự thảo chờ phê duyệt

**Ngày cập nhật:** 18/10/2026

**Mục đích dự án:** Đồ án

**Ngôn ngữ:** Tiếng Việt

---

## Kiểm soát tài liệu

| Phiên bản | Ngày | Nội dung | Trạng thái |
|---|---:|---|---|
| 0.1 | 14/10/2026 | Đặc tả ban đầu dựa trên mã nguồn và API hiện có | Lưu để tham chiếu |
| 0.2 | 18/10/2026 | Chuẩn hóa phạm vi đồ án; bổ sung Stripe Sandbox, payment/refund, quyết định voucher/điểm/đồ ăn và bằng chứng kiểm thử | Chờ phê duyệt baseline |

### Quy ước trạng thái

- **Đã kiểm chứng:** có bằng chứng test hoặc nghiệm thu được nêu trong tài liệu dự án.
- **Hiện trạng:** tìm thấy trong source/API; không tự đồng nghĩa đã nghiệm thu toàn bộ.
- **Yêu cầu đã chốt:** quyết định nghiệp vụ đã được chủ dự án xác nhận và là mục tiêu bắt buộc của baseline.
- **Gap cần triển khai/đối chiếu:** yêu cầu đã chốt nhưng chưa được xác nhận đầy đủ trong mã nguồn hoặc kiểm thử.
- **Ngoài phạm vi:** không phải cam kết của đồ án phiên bản này.

> SRS mô tả yêu cầu và tiêu chí nghiệm thu. SRS không chứa API key, webhook secret, JWT, sandbox token, số thẻ, CVC hoặc URL tunnel ngắn hạn.

---

# 1. Giới thiệu

## 1.1. Mục đích

Cinema là hệ thống hỗ trợ khách tra cứu phim và lịch chiếu, chọn/giữ ghế, đặt vé, thanh toán, nhận vé điện tử, hủy booking và hoàn tiền. Hệ thống cũng hỗ trợ nhân viên bán vé/check-in và quản trị viên vận hành dữ liệu rạp, lịch chiếu, giá, khuyến mãi và báo cáo.

Tài liệu này là căn cứ để:

1. Thống nhất phạm vi đồ án giữa thành viên thực hiện và người đánh giá.
2. Thiết kế/đối chiếu backend, frontend và các tích hợp ngoài.
3. Viết kiểm thử, nghiệm thu luồng nghiệp vụ quan trọng.
4. Phân biệt rõ chức năng đã được kiểm chứng trong Sandbox với khả năng vận hành thanh toán thật.

## 1.2. Phạm vi

### Thuộc phạm vi

- Tài khoản, xác thực, OTP, Google OAuth và hồ sơ người dùng.
- Tra cứu phim, rạp, phòng, ghế và suất chiếu.
- Giữ ghế, tạo booking trực tuyến hoặc tại quầy, tính giá, voucher và đồ ăn.
- Thanh toán theo các phương thức hệ thống hỗ trợ; **Stripe Sandbox là gateway chính cho demo và kiểm thử tự động**.
- Receipt, vé QR, check-in và trạng thái giao đồ ăn.
- Hủy booking, báo giá hoàn tiền, refund toàn phần/một phần.
- Quản trị phim, rạp, phòng, sơ đồ ghế, suất chiếu, giá, ngày lễ, voucher, đồ ăn và nội dung.
- Báo cáo nghiệp vụ và thông báo email/outbox.

### Ngoài phạm vi baseline đồ án

- Stripe live, thu tiền thật, settlement/hoàn tiền ngân hàng thật hoặc cam kết vận hành thương mại tại Việt Nam.
- Browser automation hoàn tất hosted Checkout hoặc thử thách 3DS.
- Load/performance test trực tiếp lên Stripe Sandbox.
- Quản lý kho đồ ăn và logistics ngoài trạng thái giao đồ ăn của booking.
- Quy trình kế toán, hóa đơn điện tử, đối soát ngân hàng thực tế.

## 1.3. Đối tượng đọc

- Thành viên thực hiện đồ án.
- Giảng viên/hội đồng đánh giá.
- Người kiểm thử.
- Người triển khai môi trường demo.

## 1.4. Thuật ngữ

| Thuật ngữ | Diễn giải |
|---|---|
| Booking | Đơn đặt vé, có thể gồm vé và đồ ăn. |
| Showtime | Suất chiếu của một phim trong một phòng tại một thời điểm. |
| Seat lock | Quyền giữ ghế tạm thời trước và trong quá trình checkout. |
| Receipt | Bản ghi tiền đã được xác minh cho booking. |
| Refund | Nghĩa vụ và kết quả hoàn tiền gắn với receipt/giao dịch gốc. |
| Stripe Sandbox | Môi trường Stripe test; không tạo giao dịch tiền thật. |
| Webhook | Thông báo server-to-server từ Stripe về payment/refund. |
| 3DS | 3D Secure; trạng thái yêu cầu khách xác thực thêm, chưa phải thanh toán thành công. |
| Idempotency | Xử lý retry/sự kiện lặp mà không tạo thêm hiệu ứng tài chính hoặc vé. |
| Quote | Báo giá hủy/refund được tính ở thời điểm hiện tại trước khi xác nhận hủy. |

## 1.5. Tài liệu tham khảo

- `README.md` và `docs/api/` của repository.
- `docs/stripe-sandbox.md` — phạm vi, cấu hình và bằng chứng Stripe Sandbox.
- `migrations/20261017_03_stripe_sandbox.sql` và `migrations/20261018_04_stripe_legacy_constraints.sql`.
- Stripe Documentation: Testing, Webhooks, Checkout Sessions, Refunds, Currencies.

---

# 2. Mô tả tổng quan

## 2.1. Bối cảnh sản phẩm

Cinema cung cấp backend cho ứng dụng khách và màn hình vận hành rạp. Khách có thể tra cứu, chọn ghế và tạo booking. Backend là nguồn tin cậy duy nhất cho giá, quyền sở hữu booking, trạng thái ghế, trạng thái thanh toán, receipt và vé. Client không được tự quyết định số tiền, trạng thái `PAID` hay kết quả refund.

## 2.2. Tác nhân và vai trò

| Tác nhân | Nhu cầu/chức năng chính |
|---|---|
| Khách truy cập | Xem phim, rạp, lịch chiếu; đăng ký/đăng nhập; xem nội dung công khai. |
| Khách hàng (`USER`) | Giữ ghế, tạo booking, thanh toán, xem vé, hủy booking thuộc sở hữu. |
| Nhân viên (`STAFF`) | Bán vé tại quầy, tra cứu khách, xác nhận nghiệp vụ được cấp quyền, check-in vé. |
| Quản trị viên (`ADMIN`) | Có quyền staff và quản lý dữ liệu vận hành, cấu hình, nội dung, báo cáo. |
| Stripe Sandbox | Tạo Checkout Session/PaymentIntent test, gửi webhook, trả trạng thái refund. |
| Google OAuth | Cung cấp bằng chứng đăng nhập Google. |
| Dịch vụ email | Gửi OTP và thông báo nghiệp vụ. |

## 2.3. Các phân hệ

1. Xác thực và tài khoản.
2. Danh mục phim/rạp/phòng/suất chiếu.
3. Ghế, giữ ghế và booking.
4. Giá, hạng thành viên, voucher và đồ ăn.
5. Thanh toán, receipt, refund và Stripe Sandbox.
6. Vé QR, check-in và fulfillment đồ ăn.
7. Quản trị, nội dung và báo cáo.
8. Outbox/email và tác vụ theo lịch.

## 2.4. Môi trường và phụ thuộc

| Hạng mục | Ràng buộc hiện tại |
|---|---|
| Backend | Java 17, Spring Boot, REST API, Spring Security/JWT. |
| Dữ liệu | PostgreSQL; schema được quản lý bằng SQL migration. |
| Khóa ghế/OTP | Redis. |
| Gửi email | SMTP/Gmail theo cấu hình môi trường. |
| Payment demo | Stripe Sandbox, test API key, HTTPS webhook endpoint công khai. |
| Tài liệu API | Swagger/OpenAPI khi ứng dụng chạy. |

## 2.5. Ràng buộc và giả định

- Thời gian nghiệp vụ dùng quy ước `Asia/Ho_Chi_Minh`; việc triển khai phải kiểm thử nhất quán timezone.
- Endpoint Stripe webhook phải public HTTPS và signing secret phải đúng endpoint/môi trường.
- Nếu domain tunnel của môi trường demo thay đổi, Stripe webhook destination và Stripe success/cancel URL phải được cập nhật cùng lúc.
- Stripe Sandbox không thay thế cổng thanh toán Việt Nam thực tế.
- Tất cả secrets chỉ nằm trong cấu hình môi trường an toàn; không commit vào Git.

---

# 3. Yêu cầu chức năng và quy tắc nghiệp vụ

## 3.1. Danh mục yêu cầu chức năng

| Mã | Yêu cầu | Trạng thái |
|---|---|---|
| FR-AUTH-01 | Đăng ký bằng email/OTP và đăng nhập bằng email/mật khẩu hoặc Google OAuth. | Hiện trạng |
| FR-AUTH-02 | Quản lý access token, refresh token, đổi và khôi phục mật khẩu. | Hiện trạng |
| FR-USER-01 | Người dùng xem/cập nhật hồ sơ thuộc sở hữu. | Hiện trạng |
| FR-CATALOG-01 | Tra cứu phim, thể loại, rạp, phòng và suất chiếu công khai. | Hiện trạng |
| FR-SEAT-01 | Xem trạng thái ghế, giữ/nhả ghế và chống xung đột chọn ghế. | Hiện trạng |
| FR-BOOKING-01 | Tạo booking online với giá do server tính từ ghế, đồ ăn, hạng và voucher. | Hiện trạng |
| FR-BOOKING-02 | Xem booking/vé theo quyền sở hữu và xử lý booking hết hạn. | Hiện trạng |
| FR-PAY-01 | Tạo thanh toán Stripe Checkout cho booking Stripe Sandbox hợp lệ. | Đã kiểm chứng Sandbox |
| FR-PAY-02 | Nhận/xác minh webhook và chỉ ghi nhận thanh toán từ bằng chứng provider hợp lệ. | Đã kiểm chứng Sandbox |
| FR-PAY-03 | Cung cấp driver PaymentIntent chỉ cho regression test Sandbox an toàn. | Đã kiểm chứng Sandbox |
| FR-REFUND-01 | Tạo quote hủy và hủy booking đã thanh toán theo chính sách refund. | Hiện trạng; chính sách đã chốt |
| FR-REFUND-02 | Hoàn Stripe toàn phần/một phần, idempotent và có đối soát trạng thái provider. | Đã kiểm chứng Sandbox |
| FR-TICKET-01 | Phát hành vé chỉ sau payment được xác minh; tạo QR và không phát hành trùng. | Đã kiểm chứng cùng Stripe |
| FR-TICKET-02 | Staff/Admin kiểm tra QR, check-in một lần và cập nhật fulfillment đồ ăn. | Hiện trạng |
| FR-COUNTER-01 | Staff/Admin tạo booking tại quầy cho khách thành viên hoặc khách vãng lai. | Hiện trạng |
| FR-ADMIN-01 | Admin quản lý phim, rạp, phòng, ghế, suất chiếu, giá, ngày lễ, voucher và đồ ăn. | Hiện trạng |
| FR-CONTENT-01 | Quản lý/xem bình luận và bài viết khuyến mãi theo quyền. | Hiện trạng |
| FR-REPORT-01 | Admin xem báo cáo kinh doanh theo khoảng lọc API hỗ trợ. | Hiện trạng |
| FR-NOTIFY-01 | Gửi OTP/thông báo qua email, tách trạng thái gửi khỏi trạng thái tiền/vé. | Hiện trạng |
| FR-MEMBER-01 | Điều chỉnh tổng chi tiêu/điểm/hạng theo refund đã thành công. | **Yêu cầu đã chốt; cần đối chiếu triển khai** |

## 3.2. Use case tổng quan

| Mã UC | Use case | Tác nhân chính | FR liên quan |
|---|---|---|---|
| UC-01 | Đăng ký, đăng nhập và quản lý phiên | Khách truy cập/Khách hàng | FR-AUTH-* |
| UC-02 | Tra cứu phim, rạp và suất chiếu | Khách truy cập/Khách hàng | FR-CATALOG-01 |
| UC-03 | Giữ ghế và tạo booking | Khách hàng | FR-SEAT-01, FR-BOOKING-01 |
| UC-04 | Thanh toán bằng Stripe Checkout | Khách hàng, Stripe Sandbox | FR-PAY-01, FR-PAY-02, FR-TICKET-01 |
| UC-05 | Regression payment Sandbox | Test runner, Stripe Sandbox | FR-PAY-03 |
| UC-06 | Xem booking/vé | Khách hàng | FR-BOOKING-02, FR-TICKET-01 |
| UC-07 | Quote hủy và refund | Khách hàng/Staff/Admin, Stripe Sandbox | FR-REFUND-* , FR-MEMBER-01 |
| UC-08 | Bán vé tại quầy | Staff/Admin | FR-COUNTER-01 |
| UC-09 | Check-in vé và giao đồ ăn | Staff/Admin | FR-TICKET-02 |
| UC-10 | Quản lý vận hành rạp | Admin | FR-ADMIN-01 |
| UC-11 | Nội dung, báo cáo và thông báo | User/Admin/Hệ thống | FR-CONTENT-01, FR-REPORT-01, FR-NOTIFY-01 |

## 3.3. Đặc tả use case trọng tâm

### UC-03 — Giữ ghế và tạo booking

| Thành phần | Đặc tả |
|---|---|
| Tác nhân | Khách hàng đã xác thực; Staff/Admin ở luồng quầy. |
| Tiền điều kiện | Showtime còn bán; ghế hợp lệ thuộc phòng của showtime; người dùng có quyền tương ứng. |
| Luồng chính | Chọn ghế → hệ thống kiểm tra/xác lập lock → khách gửi ghế, đồ ăn, voucher, payment method → server kiểm tra và tính giá snapshot → tạo booking `PENDING` với deadline → tạo draft ticket/food. |
| Ngoại lệ | Ghế đã bán/đang giữ bởi người khác; ghế không thuộc phòng; suất không bán; voucher/món không hợp lệ; client gửi số tiền sai. |
| Hậu điều kiện | Thành công: booking và dữ liệu snapshot nhất quán. Thất bại: không có booking/ghế/voucher thành công một phần. |
| Tiêu chí chấp nhận | Tổng tiền do server xác định; booking mới `PENDING`; deadline thanh toán là 15 phút từ thời điểm tạo. |

### UC-04 — Thanh toán Stripe Checkout

| Thành phần | Đặc tả |
|---|---|
| Tác nhân | Chủ booking; Stripe Sandbox. |
| Tiền điều kiện | Booking dùng `STRIPE`, thuộc người dùng, ở `PENDING`, còn payment deadline; Stripe Sandbox được bật và cấu hình hợp lệ. |
| Luồng chính | Client yêu cầu tạo payment → backend lấy snapshot booking → tạo/reuse Stripe Checkout Session → trả `payUrl`, `sessionId`, deadline → khách thanh toán trên trang Stripe → Stripe gửi signed webhook → backend verify, retrieve/đối chiếu provider state → ghi receipt → booking `PAID` → phát vé. |
| Ngoại lệ | Khách rời Checkout; amount/currency/owner không khớp; webhook sai chữ ký; event lặp; 3DS `requires_action`; provider lỗi; booking đã hết hạn. |
| Hậu điều kiện | Chỉ payment đã xác thực đầy đủ mới có receipt/vé. Return URL chỉ hướng dẫn người dùng xem lại booking, không xác nhận payment. |
| Tiêu chí chấp nhận | Với payment test thành công: có PaymentIntent Sandbox `succeeded`, webhook inbox đã xử lý, một receipt và đúng số vé. Với lỗi/3DS: không có receipt/vé. |

### UC-05 — Regression PaymentIntent Sandbox

| Thành phần | Đặc tả |
|---|---|
| Tác nhân | Test runner; chủ booking; Stripe Sandbox. |
| Tiền điều kiện | Profile `stripe-sandbox`, Stripe enabled, sandbox driver enabled, test key; JWT đúng chủ booking; `X-Sandbox-Token` mạnh và hợp lệ. |
| Luồng chính | Test runner tạo booking hợp lệ → gọi driver với scenario allow-list → Stripe xử lý PaymentIntent → webhook đi qua luồng nghiệp vụ thông thường → runner kiểm tra provider, inbox, receipt, ticket/refund. |
| Scenario | `SUCCESS`, `CARD_DECLINED`, `INSUFFICIENT_FUNDS`, `THREE_D_SECURE`. |
| Ngoại lệ | Token thiếu/sai, profile/flag không đúng, scenario không hợp lệ, booking khác chủ. |
| Hậu điều kiện | Driver không nhận PAN/CVC, arbitrary PaymentMethod/amount/owner và không trực tiếp đặt booking `PAID`. |
| Tiêu chí chấp nhận | Thiếu token bị từ chối; decline/insufficient funds không tạo receipt/vé; 3DS ở `requires_action` không cấp vé. |

### UC-07 — Quote hủy và refund

| Thành phần | Đặc tả |
|---|---|
| Tác nhân | Chủ booking; Staff/Admin theo quyền; Stripe Sandbox. |
| Tiền điều kiện | Booking `PAID`, chưa có vé check-in, đủ thời gian hủy; requester là chủ booking hoặc được ủy quyền. |
| Luồng chính | Yêu cầu quote → server tính lại quote theo thời điểm hiện tại → client xác nhận quote token/số tiền kỳ vọng → server khóa booking, chuyển `CANCELLED`, hủy vé, lưu snapshot chính sách, tạo refund obligation → worker/gateway refund giao dịch gốc → trạng thái refund được xác nhận qua API/webhook. |
| Ngoại lệ | Quote thay đổi, quá hạn hủy, booking không thuộc requester, vé đã check-in, refund provider pending/failed/unknown, replay/refund cạnh tranh. |
| Hậu điều kiện | `CANCELLED` không có nghĩa là refund đã thành công. Refund chỉ thành công khi có bằng chứng provider `succeeded`. |
| Tiêu chí chấp nhận | Không hoàn vượt số tiền đã thu; refund 100%/80%/50% dùng amount VND chính xác; retry không tạo thêm refund; failed/unknown được giữ để đối soát. |

### UC-09 — Check-in vé và fulfillment đồ ăn

| Thành phần | Đặc tả |
|---|---|
| Tác nhân | Staff/Admin. |
| Tiền điều kiện | Có quyền staff/admin; QR thuộc vé hợp lệ, booking đã thanh toán, thời gian check-in hợp lệ. |
| Luồng chính | Quét/nhập QR → backend xác minh purpose/ticket/booking/time/status → đánh dấu ticket checked-in → cập nhật booking theo quy tắc → lần check-in đầu của booking giao đồ ăn một lần nếu có. |
| Ngoại lệ | QR không tồn tại/sai mục đích, vé đã dùng, ngoài thời gian, request đồng thời. |
| Hậu điều kiện | Vé không được check-in hai lần; đồ ăn đã `FULFILLED` không đủ điều kiện refund. |

## 3.4. Yêu cầu chức năng chi tiết

### FR-PAY-01 — Khởi tạo thanh toán Stripe Checkout

- **Mô tả:** Chủ booking tạo hosted Checkout Sandbox cho booking Stripe hợp lệ.
- **Quy tắc:** Số tiền/currency lấy từ backend snapshot; VND là zero-decimal (`98.000 VND` gửi Stripe là `98000`); không tin amount từ client.
- **Tiêu chí chấp nhận:** Một retry an toàn reuse giao dịch phù hợp thay vì tạo charge mới; session thuộc Stripe test account; không trả/ghi secret hoặc dữ liệu thẻ.

### FR-PAY-02 — Xác nhận payment qua bằng chứng tin cậy

- **Mô tả:** Backend xác nhận payment chỉ khi webhook Stripe có chữ ký hợp lệ hoặc reconciliation bằng Stripe API đã xác thực.
- **Quy tắc:** Đối chiếu booking/request/provider ID, amount, currency, account, `livemode=false`; durable inbox deduplicate event ID; event cũ/lặp không làm lùi trạng thái.
- **Tiêu chí chấp nhận:** Browser return, client response hoặc HTTP 200 từ client không thể tự tạo `PAID`, receipt hay ticket.

### FR-PAY-03 — Xử lý hết hạn và tiền về muộn

- **Mô tả:** Cinema giữ deadline nghiệp vụ 15 phút dù Stripe Checkout có expiry tối thiểu 30 phút.
- **Quy tắc:** Không kéo dài booking để khớp Stripe; worker đóng/expire provider payment khi đến hạn. Tiền được xác nhận sau deadline, booking `EXPIRED`, hết ghế hoặc không thể phát vé phải được bù hoàn 100%.
- **Tiêu chí chấp nhận:** Payment muộn không hồi sinh booking/vé; receipt và obligation refund vẫn lưu để truy vết nếu provider đã thu tiền.

### FR-REFUND-01 — Quote và điều kiện hủy

- **Mô tả:** Hệ thống trả quote chứa phần vé, phần đồ ăn, tổng hoàn, phần giữ lại, tỷ lệ hoàn và lý do không thể hủy.
- **Quy tắc:** Chỉ hủy booking `PAID`, chưa có ticket checked-in, trước giờ chiếu ít nhất 2 giờ. Quote phải được tính lại lúc xác nhận; client gửi số tiền kỳ vọng sai phải bị từ chối.
- **Tiêu chí chấp nhận:** Không thể dùng quote cũ để nhận số tiền lớn hơn quote hiện tại; cancellation policy snapshot được lưu cùng booking/refund.

### FR-REFUND-02 — Hoàn tiền Stripe

- **Mô tả:** Hệ thống tạo refund trên PaymentIntent Stripe gốc.
- **Quy tắc:** Lưu refund obligation trước/đồng thời với dispatch; idempotency key ổn định; query provider trước retry khi timeout/unknown; `pending` không phải `succeeded`.
- **Tiêu chí chấp nhận:** Mỗi refund đối chiếu đúng payment intent, currency và amount; một refund không bị gửi nhiều lần khi worker/webhook retry; failed sau success ban đầu phải được ghi nhận.

### FR-MEMBER-01 — Điều chỉnh quyền lợi sau refund

- **Mô tả:** Điều chỉnh quyền lợi khách hàng sau khi có refund thành công.
- **Quy tắc đã chốt:**
  1. Voucher của booking đã thanh toán **không được hoàn lại** khi khách hủy/refund.
  2. Chỉ khi refund `SUCCEEDED`, hệ thống mới giảm tổng chi tiêu/điểm tương ứng số tiền đã hoàn.
  3. Hạng thành viên phải được tính lại từ tổng chi tiêu ròng, tránh sai số do làm tròn riêng theo từng refund một phần.
  4. Refund `FAILED`, `UNKNOWN` hoặc `REVIEW_REQUIRED` không được trừ quyền lợi trước khi có bằng chứng thành công.
- **Tiêu chí chấp nhận:** Full/partial refund làm thay đổi tổng chi tiêu/hạng đúng theo tổng ròng; thực hiện lại cùng event không trừ lần hai.
- **Trạng thái:** Gap cần đối chiếu và bổ sung nếu service membership hiện chưa đảo biến động sau refund thành công.

### FR-TICKET-01 — Phát hành vé

- **Mô tả:** Hệ thống phát hành một ticket hợp lệ cho mỗi ghế sau payment đã xác thực.
- **Quy tắc:** Vé liên kết booking, seat, showtime; QR có purpose riêng; retry webhook không phát hành thêm ticket.
- **Tiêu chí chấp nhận:** Payment failure, `requires_action`, unsigned webhook hoặc booking expired không tạo ticket hợp lệ.

### FR-ADMIN-01 — Vận hành dữ liệu rạp

- **Mô tả:** Admin quản lý phim, thể loại, cinema, room, seat, showtime, giá/ngày lễ, voucher và food.
- **Quy tắc:** Không cho thay đổi/xóa làm mất liên kết lịch sử booking/ticket; showtime không được chồng lấn phòng và tuân thủ điều kiện thay đổi/hủy.
- **Tiêu chí chấp nhận:** Dữ liệu không hợp lệ hoặc trái quyền bị từ chối; thay đổi giá mới không làm thay đổi snapshot booking cũ.

## 3.5. Quy tắc nghiệp vụ

| Mã | Quy tắc đã chốt |
|---|---|
| BR-SEAT-01 | Pre-lock ghế tối đa 5 phút; booking checkout/payment deadline 15 phút. Chỉ người giữ hợp lệ mới promote/nhả lock. |
| BR-SEAT-02 | Ghế phải thuộc phòng của showtime, không trùng, không phải lối đi và chưa sold/locked trái chủ. |
| BR-BKG-01 | Booking mới ở `PENDING`; giá và các thành phần tiền là snapshot do server tính khi tạo booking. |
| BR-BKG-02 | Đúng mốc deadline không nhận payment mới; task expiry lặp không được hủy booking đã thanh toán. |
| BR-PRICE-01 | Giá bao gồm giá seat/showtime, loại ghế, thời gian, định dạng, hạng thành viên, giảm giá muộn và voucher theo cấu hình. Client không quyết định giá. |
| BR-VOUCHER-01 | Voucher phải active, trong hiệu lực, còn lượt và đạt điều kiện. Một booking dùng tối đa một voucher. |
| BR-VOUCHER-02 | Booking `PENDING` hết hạn giải phóng lượt voucher theo luồng expiry hiện có. Booking đã thanh toán rồi refund **không hoàn lại voucher**. |
| BR-PAY-01 | Redirect URL không là bằng chứng payment; chỉ webhook chữ ký hợp lệ hoặc provider retrieval đã xác thực có thể settle payment. |
| BR-PAY-02 | Payment/event lặp, đảo thứ tự hoặc retry không được tạo receipt/ticket/refund trùng hay lùi trạng thái thành công. |
| BR-PAY-03 | Stripe của đồ án bắt buộc là test/sandbox; từ chối live key/mode. Không nhận/lưu PAN/CVC. |
| BR-REFUND-01 | Tỷ lệ phần vé: 100% khi còn ít nhất 24 giờ; 80% khi còn từ 6 đến dưới 24 giờ; 50% khi còn từ 2 đến dưới 6 giờ; dưới 2 giờ không hủy theo luồng khách. |
| BR-REFUND-02 | Phần đồ ăn thực trả được hoàn 100% khi booking được phép hủy và đồ ăn chưa `FULFILLED`; không hoàn phần đồ ăn khi đã giao/check-in. |
| BR-REFUND-03 | Refund không vượt tiền đã thu; giữ/refund pending cũng phải tính trong kiểm tra vượt mức. |
| BR-REFUND-04 | Booking `CANCELLED` và refund `SUCCEEDED` là hai trạng thái riêng. Refund failed/unknown phải vào xử lý đối soát, không retry mù quáng. |
| BR-MEMBER-01 | Refund thành công làm giảm tổng chi tiêu/điểm tương ứng và tính lại hạng từ tổng ròng; event lặp không trừ lặp. |
| BR-TICKET-01 | Một ticket chỉ check-in thành công một lần; check-in hợp lệ cập nhật trạng thái theo nghiệp vụ. |
| BR-SHOW-01 | Showtime mới phải hợp lệ về thời điểm, phòng, phim, thời lượng/khoảng đệm và không xung đột với lịch hiện có. |

---

# 4. Yêu cầu giao diện và tích hợp

## 4.1. Giao diện người dùng

Frontend nằm ngoài repository backend này, nhưng giao diện đồ án phải biểu đạt được các trạng thái sau:

| Khu vực | Yêu cầu giao diện tối thiểu |
|---|---|
| Tra cứu/đặt vé | Danh sách phim/suất, sơ đồ ghế, thông tin giá và thời hạn thanh toán rõ ràng. |
| Checkout | Hiển thị trạng thái đang chuyển hướng Stripe; không thông báo thành công chỉ vì quay về return URL. |
| Booking/vé | Phân biệt `PENDING`, `PAID`, `EXPIRED`, `CANCELLED`, trạng thái refund và vé có thể sử dụng. |
| Hủy/refund | Hiển thị quote gồm tiền vé/đồ ăn/tổng hoàn/giữ lại và cảnh báo xác nhận. |
| Staff check-in | Hiển thị kết quả quét, lý do từ chối và trạng thái giao đồ ăn. |
| Admin | Form kiểm tra lỗi đầu vào; cảnh báo khi thay đổi dữ liệu ảnh hưởng booking/showtime. |

## 4.2. Giao tiếp client–backend

- Các endpoint private cần JWT hợp lệ; authorization dựa trên role và ownership tại server.
- Phần lớn API trả `ApiResponse`; API promotion admin có contract riêng theo tài liệu API.
- UI phải xử lý loading, empty state, validation error, quyền bị từ chối và lỗi network.
- UI không được tự tính/truyền amount có tính quyết định cho payment/refund.

## 4.3. Tích hợp Stripe Sandbox

| Thành phần | Yêu cầu |
|---|---|
| Hosted Checkout | Backend tạo Checkout Session và trả URL cho demo thủ công. |
| PaymentIntent driver | Chỉ dùng cho API-level test automation ở profile sandbox riêng. |
| Webhook | `POST /api/v1/public/stripe/webhook`; verify raw body và `Stripe-Signature`. |
| Return URL | Chỉ là trang thông báo/quay về app; không confirm payment. |
| Event tối thiểu | `payment_intent.succeeded`, `payment_intent.payment_failed`, `refund.created`, `refund.updated`, `refund.failed`. |
| Bảo vệ driver | JWT chủ booking + `X-Sandbox-Token` + sandbox profile/flag + scenario allow-list. |

## 4.4. Email, OAuth và outbox

- Google ID token phải được backend kiểm tra trước khi cấp phiên.
- OTP/reset token không được dùng thay access token.
- Lỗi gửi email không được đảo ngược payment/receipt/ticket hợp lệ.
- Notification bất đồng bộ phải idempotent và truy vết được bằng outbox khi áp dụng.

---

# 5. Yêu cầu dữ liệu

## 5.1. Thực thể nghiệp vụ chính

| Nhóm | Dữ liệu chính |
|---|---|
| Danh tính | User, role, account type, rank, refresh token/OTP theo luồng. |
| Rạp/phim | Movie, genre, cinema, room, seat, seat type, showtime. |
| Bán hàng | Booking, draft/final ticket, food order/draft item, voucher, price snapshot. |
| Thanh toán | Payment request, payment confirmation, payment receipt, refund. |
| Stripe Sandbox | Stripe payment (session/intent/state/idempotency/deadline), durable webhook inbox. |
| Vận hành | Promotion article, comment, holiday/price config, outbox event. |

## 5.2. Vòng đời trạng thái

### Booking

```text
PENDING → PAID → CHECKED_IN
PENDING → EXPIRED
PAID → CANCELLED

`REFUNDED` là trạng thái legacy có thể tồn tại trong dữ liệu lịch sử; kết quả tiền hiện hành luôn phải đọc từ `Refund.status`, không suy ra từ `Booking.status`.
```

- Không được chuyển `EXPIRED` về `PAID` chỉ vì payment đến muộn.
- Không được coi `CANCELLED` là refund đã `SUCCEEDED`.

### Payment Stripe

```text
CREATED / awaiting provider
→ succeeded → settlement receipt/ticket
→ failed / requires_payment_method
→ requires_action (3DS; chưa thanh toán)
→ expired/cancelled
```

### Refund

```text
REQUESTED → PROCESSING → SUCCEEDED
                     ├→ FAILED
                     ├→ UNKNOWN
                     └→ REVIEW_REQUIRED
```

`SUCCEEDED` chỉ dùng khi provider API/webhook đã cung cấp bằng chứng hợp lệ.

## 5.3. Ràng buộc toàn vẹn

1. Một ghế/showtime không được bán cho hai booking thành công.
2. Payment reference/PaymentIntent được liên kết duy nhất với giao dịch hợp lệ theo gateway.
3. Một payment thành công có tối đa một receipt thành công cho cùng transaction reference.
4. Một event webhook được durable inbox xử lý idempotent theo event ID.
5. Tổng refund thành công/có nghĩa vụ không vượt số tiền receipt đã thu.
6. Ticket chỉ tồn tại/hợp lệ sau settlement và chỉ được check-in một lần.
7. Snapshot booking/quote được giữ để truy vết giá và policy tại thời điểm nghiệp vụ.

## 5.4. Dữ liệu nhạy cảm

- Password chỉ lưu dạng hash; không trả hash trong response.
- Không lưu raw card PAN, CVC hoặc Stripe client secret trong database/log/report.
- Không commit `sk_test_*`, `whsec_*`, JWT, OAuth secret, SMTP password hoặc sandbox token.
- API/log phải tránh raw webhook body hoặc provider exception chứa secret/dữ liệu không cần thiết.

---

# 6. Yêu cầu phi chức năng và nghiệm thu

## 6.1. Bảo mật và phân quyền

| Mã | Yêu cầu | Cách kiểm chứng |
|---|---|---|
| NFR-SEC-01 | Server xác minh JWT, role và ownership cho thao tác private. | Test User A không xem/hủy/trả tiền booking của User B; staff/admin theo quyền. |
| NFR-SEC-02 | Stripe webhook phải verify chữ ký raw body; event sai bị từ chối. | Gửi request thiếu/sai `Stripe-Signature`; nhận 400 và DB không đổi. |
| NFR-SEC-03 | Test driver Sandbox không xuất hiện ngoài profile/flag bắt buộc. | Test context/profile production không có route hoặc bị chặn. |
| NFR-SEC-04 | Không nhận/lưu raw card data và secrets. | Review request DTO, DB, log và repository. |

## 6.2. Tính nhất quán và độ tin cậy

| Mã | Yêu cầu | Cách kiểm chứng |
|---|---|---|
| NFR-CON-01 | Không bán trùng ghế và không tạo ticket/receipt trùng khi request cạnh tranh. | Test đồng thời seat lock, booking, webhook replay. |
| NFR-CON-02 | Payment/refund retry và webhook out-of-order an toàn. | Gửi event lặp; mô phỏng provider state final khác event cũ. |
| NFR-REL-01 | Lỗi gateway/email/Redis không tạo kết quả thành công giả. | Mô phỏng lỗi; kiểm tra trạng thái nghiệp vụ, backlog/đối soát. |
| NFR-REL-02 | Late payment được bù hoàn thay vì hồi sinh booking hết hạn. | Test deadline/worker/reconciliation và refund obligation. |

## 6.3. Khả năng sử dụng

- Người dùng phải thấy rõ deadline, số tiền cuối, trạng thái payment/refund và lý do từ chối.
- Không dùng màu sắc là dấu hiệu duy nhất cho trạng thái lỗi/thành công.
- Chức năng quan trọng phải hiển thị loading/error để tránh thao tác gửi lặp.

## 6.4. Hiệu năng

- Chưa đặt ngưỡng p95 hoặc tải đồng thời chính thức vì chưa benchmark trên môi trường đại diện.
- Trước khi tuyên bố sẵn sàng phát hành thật phải chốt workload, dữ liệu, endpoint, p95 và error rate mục tiêu.
- Không dùng Stripe Sandbox làm hạ tầng load test.

## 6.5. Vận hành

- Build/test phải chạy trước deployment.
- Backup JAR, `.env` an toàn và PostgreSQL trước migration/deploy.
- Migration chỉ đi tiếp; không sửa lịch sử migration đã áp dụng.
- Có khả năng xem lỗi nghiệp vụ qua booking/payment/refund reference nhưng không lộ secrets.
- Rollback JAR không thay thế backup/khôi phục database.

## 6.6. Ma trận nghiệm thu Stripe Sandbox

| Case | Kỳ vọng | Trạng thái/bằng chứng |
|---|---|---|
| Payment success | PaymentIntent Sandbox thành công, webhook signed được xử lý, có receipt/ticket | Đã kiểm chứng 18/10/2026 |
| Full refund | Refund đúng full amount, provider `succeeded` | Đã kiểm chứng |
| Partial refund 80% | Quote/refund đúng amount 80% phần vé | Đã kiểm chứng |
| Partial refund 50% | Quote/refund đúng amount 50% phần vé | Đã kiểm chứng |
| Thiếu sandbox token | Driver bị từ chối; không gọi provider | Đã kiểm chứng |
| Card declined | Không receipt/ticket | Đã kiểm chứng |
| Insufficient funds | Không receipt/ticket | Đã kiểm chứng |
| 3DS required | `requires_action`; không receipt/ticket | Đã kiểm chứng backend API-level |
| Sai webhook signature | HTTP 400, DB không đổi | Đã kiểm chứng |
| Hosted Checkout | Session test VND đúng amount, `livemode=false`, expire được | Đã kiểm chứng; browser completion chưa tự động hóa |
| Payment/refund live | Tiền và settlement ngân hàng thật | Ngoài phạm vi |

Kết quả local sau bổ sung Stripe được ghi nhận: **332 tests, 0 failures, 0 errors, 1 skipped**. Bằng chứng này không thay thế browser E2E, performance test hay payment live.

## 6.7. Truy vết yêu cầu

| Yêu cầu | Use case | Quy tắc/NFR chính | Bằng chứng kiểm thử cần có |
|---|---|---|---|
| FR-SEAT-01, FR-BOOKING-01 | UC-03 | BR-SEAT-*, BR-BKG-*; NFR-CON-01 | Ghế cạnh tranh, giá snapshot, booking expiry |
| FR-PAY-01, FR-PAY-02 | UC-04 | BR-PAY-*; NFR-SEC-02, NFR-CON-02 | Signed webhook, amount mismatch, replay, provider retrieval |
| FR-PAY-03 | UC-05 | BR-PAY-03; NFR-SEC-03 | Token/profile/scenario/ownership reject |
| FR-REFUND-* | UC-07 | BR-REFUND-*; NFR-CON-02 | Full/partial/pending/failed/duplicate refund |
| FR-MEMBER-01 | UC-07 | BR-MEMBER-01 | Full/partial refund, idempotent point/rank recalculation |
| FR-TICKET-* | UC-04, UC-09 | BR-TICKET-01; NFR-CON-01 | Issue once, QR invalid/used/concurrent check-in |
| FR-ADMIN-01 | UC-10 | BR-SHOW-01 | Role, invalid input, dependency/history protection |

---

# 7. Quyết định, gap và quản lý thay đổi

## 7.1. Quyết định nghiệp vụ đã phê duyệt

1. Cinema là **đồ án**, không cam kết hệ thống thanh toán thương mại thật.
2. **Stripe Sandbox** là payment gateway chính cho demo và regression test tự động.
3. Voucher của booking đã thanh toán sẽ **không được hoàn lại** khi khách hủy/refund.
4. Refund thành công phải giảm tổng chi tiêu/điểm và tính lại hạng thành viên từ tổng ròng.
5. Đồ ăn được hoàn 100% nếu booking đủ điều kiện hủy và chưa fulfillment; không hoàn khi đã giao/check-in.
6. Khi refund gateway failed/unknown, booking vẫn giữ kết quả hủy nhưng refund phải được đối soát; không retry mù quáng hoặc cấp vé lại.

## 7.2. Hạng mục đã xử lý và việc còn theo dõi

| Mã | Nội dung | Trạng thái |
|---|---|---|
| RES-01 | Membership `totalPaid`/point/rank sau refund thành công | Đã bổ sung reconciliation idempotent theo từng refund, có marker audit và unit test success/replay/provider failure. Cần áp dụng migration `20261018_05_refund_membership_adjustment.sql` trước khi deploy môi trường dùng DB hiện hữu. |
| RES-02 | Hủy showtime từng giải phóng voucher của booking đã thanh toán | Đã sửa: chỉ booking `PENDING` hết hạn mới trả voucher; booking đã thanh toán bị hủy showtime giữ nguyên voucher đã dùng. |
| RES-03 | Admin/email có thể ngụ ý refund đã chi trả | Đã sửa: booking bị hủy và refund obligation được tạo; API/email không tuyên bố gateway payout đã thành công. Kết quả tiền đọc từ `Refund.status`. |
| GAP-03 | Browser E2E hosted Checkout và hoàn tất 3DS | Không chặn Sandbox API-level/demo; bổ sung khi cần kiểm thử giao diện toàn diện. |
| GAP-04 | Mục tiêu hiệu năng/RPO/RTO | Chốt số liệu và kế hoạch đo nếu đồ án yêu cầu phần vận hành/performance. |

## 7.3. Điều kiện chốt SRS baseline 1.0

SRS được chốt khi:

1. Người phụ trách xác nhận các quyết định tại mục 7.1.
2. Migration và các hạng mục `RES-*` được áp dụng/kiểm chứng trên môi trường demo; mọi phần chưa hoàn thành được ghi rõ.
3. Mỗi requirement MVP có ít nhất một tiêu chí nghiệm thu và đường truy vết kiểm thử.
4. Không có secret, dữ liệu thẻ hoặc URL tạm thời trong tài liệu.
5. Bất kỳ thay đổi nghiệp vụ nào sau khi chốt được ghi phiên bản, lý do, ảnh hưởng FR/BR/test và người phê duyệt.

---

## Phụ lục A — API/điểm vào tham khảo

Các đường dẫn sau là tham khảo contract backend, không thay thế Swagger/OpenAPI đang chạy:

```text
POST /api/v1/bookings
GET  /api/v1/bookings/{id}/cancellation-quote
POST /api/v1/bookings/{id}/cancel
POST /api/v1/bookings/{bookingId}/stripe/pay
POST /api/v1/public/stripe/webhook
GET  /api/v1/public/stripe/return
POST /api/v1/bookings/{bookingId}/stripe/sandbox/payment-intent
POST /api/v1/staff/tickets/check-in
```

Automation endpoint cuối chỉ được hiện diện trong profile Stripe Sandbox có kiểm soát; không coi đó là API production.

## Phụ lục B — Bằng chứng và giới hạn Sandbox

- Payment/refund Stripe đã được chạy trên **test account** với webhook ký hợp lệ.
- Không công bố key, signing secret, JWT, raw payment URL, raw webhook payload hoặc thông tin thẻ trong SRS/báo cáo.
- Sandbox pass chứng minh luồng backend trong môi trường test; không chứng minh tiền đã về ngân hàng hay Stripe có thể vận hành thương mại tại Việt Nam.
