# Stripe Sandbox — phạm vi, cấu hình và nghiệm thu

## Phạm vi đã chọn

- Stripe Sandbox là gateway phục vụ phát triển, kiểm thử và demo Cinema; không bật thanh toán thật.
- Giữ VNPay/MoMo và lịch sử giao dịch. Không chuyển giao dịch cũ sang Stripe để hoàn tiền.
- Demo bằng Stripe Checkout (trang nhập thẻ do Stripe cung cấp).
- Automation backend bằng Stripe test PaymentMethod; không nhận/lưu số thẻ hoặc CVC.
- Cả hai đường thanh toán phải đi qua cùng nghiệp vụ receipt, vé, hủy và hoàn tiền.
- Automation PaymentIntent không thay thế kiểm thử giao diện Checkout. Nếu cần nghiệm thu toàn bộ UI tự động, bổ sung browser E2E riêng.
- Stripe sandbox không chuyển tiền thật. Pass sandbox không chứng minh đã hoàn tiền vào ngân hàng thật hay đủ điều kiện vận hành thương mại tại Việt Nam.

## Trạng thái kiểm chứng

- Đã kiểm tra trên server: secret key có tiền tố test; gọi API `GET /v1/balance` xác thực thành công và trả `livemode=false`.
- Webhook endpoint Sandbox đã bật tại public URL của môi trường test, dùng Snapshot events và đúng 5 event payment/refund đã liệt kê.
- Nghiệm thu Sandbox API-level ngày 2026-10-18:
  - payment thành công + Stripe-signed webhook + receipt/ticket: đạt;
  - refund toàn phần: đạt;
  - refund một phần theo policy Cinema 80% và 50%: đạt;
  - sandbox token thiếu: bị chặn;
  - card decline và insufficient funds: không tạo receipt/vé;
  - 3DS required: còn `requires_action`, không cấp vé;
  - hosted Checkout: tạo Session VND đúng tiền, Sandbox/livemode=false và đã kiểm tra expire Session.
- Local suite sau bổ sung Stripe: `332 tests, 0 failures, 0 errors, 1 skipped` (test Spring context legacy bị skip).
- Không đánh đồng các kết quả này với payment live/Vietnam settlement, browser Checkout end-to-end hay hoàn tiền ngân hàng thật.

## Thiết lập một lần trên Stripe

1. Mở đúng Sandbox chứa API key đã cấu hình trên server.
2. Vào Workbench → Webhooks → Add destination, chọn sự kiện từ **Your account**.
3. Với handler Event của Stripe Java SDK, chọn **Snapshot events** và API version tương thích SDK mà backend sử dụng.
4. Khai báo endpoint HTTPS public:

   ```text
   https://<public-host>/api/v1/public/stripe/webhook
   ```

5. Đăng ký:

   ```text
   payment_intent.succeeded
   payment_intent.payment_failed
   refund.created
   refund.updated
   refund.failed
   ```

6. Lưu signing secret `whsec_...` trực tiếp vào `.env` trên server. Không gửi qua chat, log, Git hoặc ảnh chụp.
7. Có thể đăng ký trước khi endpoint được deploy; delivery trước thời điểm deploy có thể thất bại. Sau khi deploy phải kiểm tra lại delivery.

### Biến môi trường

```dotenv
STRIPE_ENABLED=true
STRIPE_TEST_MODE=true
STRIPE_SECRET_KEY=sk_test_<set-on-server>
STRIPE_WEBHOOK_SECRET=whsec_<set-on-server>
PAYMENT_TEST_AUTOMATION_ENABLED=false
```

`STRIPE_PUBLISHABLE_KEY=pk_test_...` không bắt buộc cho hosted Checkout chỉ trả URL từ backend. Không nhầm public key, secret API key và webhook signing secret.

Automation phải được bật riêng theo profile sandbox được triển khai; một flag không đủ để mở API test trong production. Không đổi profile đang chạy một cách mù quáng: cần kiểm tra datasource, Redis và authentication của profile đích.

Với Stripe CLI:

```bash
stripe login
stripe listen --forward-to http://localhost:8080/api/v1/public/stripe/webhook
```

Dùng signing secret do listener in ra cho môi trường đó. Secret của listener khác secret endpoint trong Dashboard. CLI phải tiếp tục chạy để forward; đây là công cụ phát triển, không phải thay thế webhook public khi phát hành.

## Yêu cầu nghiệp vụ và an toàn

### STRIPE-01 — Khởi tạo thanh toán

- Chỉ chủ booking hợp lệ được khởi tạo, trong hạn thanh toán và khi Stripe được bật.
- Số tiền lấy từ snapshot backend; không tin số tiền, currency hoặc booking owner do client khai.
- VND là tiền tệ không có phần thập phân trong Stripe: `98.000 VND` tương ứng `amount=98000`, không nhân 100 như VNPay.
- Chuyển đổi tiền phải chính xác; từ chối phần lẻ không hợp lệ, không cắt phần thập phân âm thầm.
- Lưu request và khóa idempotency bền vững để retry không tạo thêm lần thu ngoài ý muốn.

### STRIPE-02 — Xác nhận thanh toán

- Verify chữ ký trên raw body bằng SDK và signing secret đúng môi trường.
- Đối chiếu gateway ID, request, booking, amount, currency và `livemode=false` trước khi ghi nhận.
- Metadata là thông tin liên kết, không phải bằng chứng duy nhất cho phép thu/hoàn tiền.
- Không xác nhận tiền từ browser return, client response hay API test nội bộ tự sửa trạng thái.
- Webhook trùng/cạnh tranh không tạo thêm receipt hoặc vé. Một latest-event-id không đủ để chống mọi webhook phát lại; cần lưu lịch sử xử lý/dedup bền vững.
- Chỉ ACK sau khi đã lưu sự kiện hoặc hoàn tất transaction cần thiết; lỗi phải cho phép Stripe retry.
- Callback không bảo đảm đúng thứ tự; event cũ không được làm trạng thái tài chính lùi về pending/failed.

### STRIPE-03 — Hạn booking và tiền đến muộn

- Giữ nguyên hạn nghiệp vụ Cinema; thử lại không kéo dài giữ ghế.
- Stripe Checkout có giới hạn riêng: `expires_at` tối thiểu 30 phút từ khi tạo session, trong khi Cinema cho thanh toán 15 phút.
- Không kéo dài booking lên 30 phút để khớp Stripe. Cần expire Checkout Session bằng API tại hạn nghiệp vụ và xử lý race/thời gian scheduler trễ.
- Việc đóng trang thanh toán không tuyệt đối ngăn tiền về muộn. Khi booking hết hạn hoặc không thể cấp ghế, ghi receipt và nghĩa vụ hoàn 100%; không phục hồi vé đã mất quyền giữ ghế.
- Đối soát gateway khi thiếu webhook phải xác thực qua API Stripe, không lấy redirect làm bằng chứng.

### STRIPE-04 — Hoàn tiền

- Dùng giao dịch Stripe gốc; không trả qua gateway khác.
- Giữ chính sách Cinema: tỷ lệ 100%/80%/50% áp lên phần vé thực trả theo mốc thời gian; đồ ăn đủ điều kiện hoàn theo chính sách riêng. Không mặc định tỷ lệ áp lên toàn bộ đơn có đồ ăn.
- Hủy qua cancellation quote và API nghiệp vụ hiện có; không thêm API test bỏ qua điều kiện hủy.
- Ghi nghĩa vụ hoàn bền vững trước khi gọi gateway, dùng idempotency key ổn định.
- HTTP 200 không đủ. Refund object có trạng thái `succeeded` từ API xác thực hoặc webhook được xác minh mới là bằng chứng trạng thái gateway thành công; không bắt buộc chờ webhook nếu API đã trả bằng chứng hợp lệ.
- Pending/timeout không được chuyển thành succeeded. Query/đối soát trước khi gửi lại khi kết quả chưa rõ; không tạo idempotency key mới chỉ vì timeout.
- Tổng khoản hoàn thành công và khoản đang giữ cho yêu cầu hoàn không vượt tiền thực thu.
- Event refund thất bại về sau phải được lưu và xử lý; trạng thái thành công tại gateway không phải lời cam kết ngân hàng thật đã ghi có trong sandbox.

### STRIPE-05 — Automation

- Driver dùng danh sách scenario cho phép (`pm_card_*`); không cho nhận arbitrary Stripe ID/amount/owner từ client.
- Chỉ bật ở môi trường sandbox chuyên biệt với quyền truy cập phù hợp, feature flag riêng và test API key. Production không đăng ký route test.
- Driver chỉ gọi Stripe; không trực tiếp cấp vé hay tự gán `PAID`.
- Test runner chờ webhook có timeout hữu hạn, đọc kết quả qua API nghiệp vụ và kiểm tra dữ liệu gateway tương ứng. Hết thời gian phải báo chưa đạt, không giả lập success.
- Case 3DS chờ thao tác khách hàng cần browser automation nếu muốn hoàn tất challenge; backend-only test chỉ chứng minh `requires_action` không cấp vé.
- Không dùng Stripe sandbox cho load test; dùng stub/mock riêng và ghi rõ loại kiểm thử.

## Ma trận nghiệm thu

Mỗi case ghi booking/request/PaymentIntent/refund ID, kết quả kỳ vọng, bằng chứng API/webhook và thời điểm chạy. Không ghi secrets, JWT, raw thẻ hoặc client secret vào báo cáo.

| Case | Bằng chứng cần có | Trạng thái ban đầu |
|---|---|---|
| Checkout thủ công | Session URL, payment thật trong Sandbox, webhook, receipt, vé | Chưa chạy |
| API automation thành công | PaymentIntent test + webhook thật, đúng receipt/vé | Chưa chạy |
| Thẻ bị từ chối/thiếu tiền | Provider failure; không có receipt/vé thành công | Chưa chạy |
| Webhook chữ ký sai | Bị từ chối, DB không đổi | Chưa chạy |
| Webhook trùng/khác thứ tự | Không tạo trùng, không lùi trạng thái | Chưa chạy |
| Callback mất/tạm lỗi | Retry/đối soát nhận biết tiền; không thu lần hai | Chưa chạy |
| Session hết hạn nghiệp vụ | Không gia hạn ghế; expire session; tiền về muộn được bù hoàn | Chưa chạy |
| Full refund | Refund ID gốc, đúng amount, gateway status succeeded | Chưa chạy |
| 80% và 50% | Hai booking phù hợp mốc, quote đúng và hai refund riêng | Chưa chạy |
| Refund lặp/cạnh tranh | Không vượt số tiền; cùng nghĩa vụ không tạo refund thứ hai | Chưa chạy |
| Refund pending/failed | Không báo thành công giả; lưu nghĩa vụ và lý do | Chưa chạy |
| Automation trong production | Route không tồn tại hoặc bị chặn; không có charge | Chưa chạy |

## Triển khai server

- Build và chạy test trước, rồi backup JAR/DB theo quy trình hiện có.
- Áp dụng migration mới; không sửa lịch sử migration đã chạy hoặc xóa dữ liệu VNPay.
- Chỉ bật Stripe sau khi config, webhook secret và schema đầy đủ.
- Kiểm tra `/api/v1/public/stripe/webhook` bằng signed event của Stripe; GET/healthcheck 200 không chứng minh webhook xử lý đúng.
- Kiểm tra Event deliveries trên Dashboard và trạng thái nghiệp vụ Cinema sau mỗi giao dịch.
- Không tự push hoặc triển khai production. Nghiệm thu server sandbox phải được báo riêng với kết quả local/mock.

## Tài liệu chính thức

- https://docs.stripe.com/testing
- https://docs.stripe.com/webhooks
- https://docs.stripe.com/refunds
- https://docs.stripe.com/api/checkout/sessions/create
- https://docs.stripe.com/api/checkout/sessions/expire
- https://docs.stripe.com/currencies
- https://stripe.com/global
