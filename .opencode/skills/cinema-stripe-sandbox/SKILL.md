---
name: cinema-stripe-sandbox
version: 1.0
---

# Cinema Stripe Sandbox: vận hành và nghiệm thu

Dùng skill này khi thay đổi, triển khai, kiểm thử hoặc xử lý lỗi Stripe Sandbox trong `backend_cinema`. Stripe ở project này là gateway **test/demo**, không được bật Stripe live mode hoặc dùng để nhận tiền thật tại Việt Nam.

## Nguyên tắc không được phá

- Không gửi, in, commit hoặc đưa vào log: `sk_test_`, `sk_live_`, `pk_*`, `whsec_*`, JWT, `X-Sandbox-Token`, Checkout URL/client secret có thể còn dùng được.
- Không nhận/lưu PAN, CVC hay dữ liệu thẻ. Automation chỉ dùng allow-list Stripe test PaymentMethod.
- Return/browser redirect không phải bằng chứng thanh toán. Chỉ webhook đã verify raw body + `Stripe-Signature`, hoặc retrieval đã xác thực từ Stripe, mới được cập nhật nghiệp vụ.
- Không tự gán `booking=PAID`, không bypass cancellation quote để test refund, không coi HTTP `200` của refund request là thành công tài chính.
- Không sửa migration đã chạy. Bổ sung migration theo thứ tự và kiểm tra constraint legacy có tên khác.
- Giữ gateway `VNPAY`, `MOMO`, `CASH` và dữ liệu cũ. Không dùng Stripe để hoàn giao dịch cũ của gateway khác.

## Tài liệu/điểm vào chính

- Contract và kết quả nghiệm thu: `docs/stripe-sandbox.md`.
- Module Stripe: `src/main/java/com/uit/backend_cinema/modules/payment/stripe/`.
- Migration: `migrations/20261017_03_stripe_sandbox.sql`, sau đó `migrations/20261018_04_stripe_legacy_constraints.sql` nếu DB cũ còn `chk_*_payment_method`.
- Test API-level Sandbox: `scripts/stripe_sandbox_e2e.py`.
- Test local: `StripeSandboxTest`, `StripeRefundProcessorTest`, `StripeMigrationTest`.

## Cấu hình an toàn

`.env` ở server phải có, nhưng không hiển thị giá trị:

```dotenv
STRIPE_ENABLED=true
STRIPE_TEST_MODE=true
STRIPE_SECRET_KEY=sk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...
STRIPE_ACCOUNT_ID=acct_...
STRIPE_SUCCESS_URL=https://<public-host>/api/v1/public/stripe/return
STRIPE_CANCEL_URL=https://<public-host>/api/v1/public/stripe/return
PAYMENT_TEST_AUTOMATION_ENABLED=true
STRIPE_SANDBOX_TOKEN=<ít nhất 32 ký tự ngẫu nhiên>
```

Kiểm tra prefix/khả năng dùng key mà không in secret:

```bash
python3 - <<'PY'
from pathlib import Path
v = {}
for line in Path('.env').read_text().splitlines():
    k, sep, value = line.partition('=')
    if sep: v[k.strip()] = value.strip().strip('"\'')
for k, prefix in [('STRIPE_SECRET_KEY', 'sk_test_'), ('STRIPE_WEBHOOK_SECRET', 'whsec_')]:
    print(f'{k}:', 'OK' if v.get(k, '').startswith(prefix) else 'MISSING_OR_INVALID')
PY
```

Sandbox driver chỉ tồn tại khi cả ba điều kiện đúng:

```text
profile stripe-sandbox
stripe.enabled=true
stripe.sandbox-driver-enabled=true
```

Route driver cần JWT của đúng chủ booking **và** `X-Sandbox-Token`. Không đưa profile `stripe-sandbox` hoặc flag driver vào production.

## Stripe Dashboard / Webhook

Dùng endpoint public HTTPS:

```text
https://<public-host>/api/v1/public/stripe/webhook
```

Đăng ký Snapshot events từ **Your account**:

```text
payment_intent.succeeded
payment_intent.payment_failed
refund.created
refund.updated
refund.failed
```

Trước khi kết luận callback hoạt động, kiểm tra cả Stripe Event delivery lẫn `stripe_webhook_inbox.processed_at` trong DB. Event có thể gửi lặp hoặc sai thứ tự; luôn dựa vào durable inbox/event ID và provider retrieval.

## Luồng triển khai

1. Đọc `git status`; bảo toàn thay đổi người dùng, nhất là payment/refund khác.
2. Chạy tests trước:

   ```bash
   ./gradlew test bootJar --console=plain
   ```

3. Backup JAR + `.env` + PostgreSQL trước migration/deploy.
4. Áp dụng migration forward. Liệt kê DB constraints nếu booking `STRIPE` báo lỗi:

   ```sql
   SELECT conrelid::regclass, conname, pg_get_constraintdef(oid)
   FROM pg_constraint
   WHERE conrelid IN ('bookings'::regclass, 'invoices'::regclass) AND contype='c';
   ```

5. Khởi động JAR với `--spring.profiles.active=stripe-sandbox` chỉ trên server test.
6. Kiểm tra `GET /api/v1/public/stripe/return` trả 200. Điều này chỉ chứng minh app lên, **không** chứng minh webhook.
7. Tạo/kiểm tra signed Stripe webhook thật và tra inbox/booking/receipt/refund.

## Payment và deadline

- Booking Cinema giữ ghế 15 phút; Stripe Checkout Session chỉ có `expires_at` tối thiểu 30 phút.
- Không kéo dài booking để khớp Stripe. `StripeExpiryWorker` phải expire Session/cancel PaymentIntent khi qua deadline nghiệp vụ.
- Nếu tiền được xác nhận sau `paymentDeadline`, hoặc booking hết hạn/không còn ghế: lưu receipt nhưng tạo refund 100%, không hồi sinh booking/vé.
- VND là Stripe zero-decimal: `98.000 VND` gửi `amount=98000`, không nhân 100.

## Automation API

```http
POST /api/v1/bookings/{bookingId}/stripe/sandbox/payment-intent
Authorization: Bearer <owner-JWT>
X-Sandbox-Token: <server-secret>
Content-Type: application/json

{"scenario":"SUCCESS"}
```

Scenario cho phép:

| Scenario | Stripe test behavior |
|---|---|
| `SUCCESS` | payment succeeds |
| `CARD_DECLINED` | payment fails/needs another method |
| `INSUFFICIENT_FUNDS` | payment fails/needs another method |
| `THREE_D_SECURE` | `requires_action`; không phát vé |

Automation là PaymentIntent/API-level; không thay thế browser E2E cho form hosted Checkout/3DS challenge.

## Refund

- Refund được tạo qua cancellation quote và cancellation API thông thường.
- Store `PaymentIntent`, `Refund`, idempotency reference, amount VND và gateway status; không lưu dữ liệu thẻ.
- Query/đối soát refund trước retry khi timeout/unknown. Giữ cùng idempotency key trong khoảng Stripe còn đảm bảo; sau đó `REVIEW_REQUIRED`, không retry mù.
- `succeeded` từ authenticated Refund object hoặc verified refund webhook là bằng chứng gateway; `pending` là `PROCESSING`; `failed/canceled` là `FAILED`.
- Khi refund đã success nhưng provider báo failure sau đó, lưu evidence và chuyển trạng thái theo event, không che giấu.

## Nghiệm thu tối thiểu

Chạy `scripts/stripe_sandbox_e2e.py` với fixture sandbox được cho phép. Script tạo booking qua API thường, chờ webhook thật, rồi hủy qua quote. Nó kiểm tra full/80%/50%, driver token, decline, insufficient funds và 3DS.

Không rerun script bằng `--seed` một cách mù quáng vì nó tạo showtime fixture. Ưu tiên truyền ID showtime fixture đã kiểm tra, luôn cleanup PENDING/Session test sau lỗi. Báo cáo chỉ lưu opaque Stripe IDs, booking/refund ID và số tiền; không lưu secret/JWT/URL.

Chỉ đánh dấu pass khi có:

1. PaymentIntent sandbox `succeeded` hoặc trạng thái failure mong đợi;
2. webhook event ID được ghi `stripe_webhook_inbox.processed_at`;
3. `PaymentReceipt`/ticket đúng khi success, không có khi failure;
4. Refund provider status/amount/payment intent đúng ở các ca 100/80/50;
5. local suite không có failures/errors.

## Giới hạn cần báo rõ

Sandbox pass không chứng minh settlement/hoàn tiền ngân hàng thật. Browser Checkout full E2E, hoàn tất 3DS challenge bằng browser và load/performance test là các hạng mục riêng; không giả là đã test khi mới chạy API-level automation.
