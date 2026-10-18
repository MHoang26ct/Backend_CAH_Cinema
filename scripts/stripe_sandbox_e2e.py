#!/usr/bin/env python3
"""Opt-in API-level Stripe Sandbox E2E on the Cinema test server (not Checkout UI).

Uses real signed Stripe webhooks, test funds only. Creates bookings and cancels
successful ones via the normal cancellation API. --seed adds two sandbox showtimes.
Never logs API keys, JWTs, raw Stripe bodies, Checkout URLs or webhook signatures.
Requires the test server's postgres_db container for read-only assertions/fixtures.
"""
import argparse
import decimal
import json
import os
from pathlib import Path
import subprocess
import time
import urllib.error
import urllib.parse
import urllib.request


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--env-file', default='.env')
    parser.add_argument('--token-file', required=True)
    parser.add_argument('--full-showtime', type=int, required=True)
    parser.add_argument('--room', type=int, required=True)
    parser.add_argument('--movie', type=int, required=True)
    parser.add_argument('--seed', action='store_true', help='Explicitly allow sandbox fixtures')
    parser.add_argument('--partial80-showtime', type=int)
    parser.add_argument('--partial50-showtime', type=int)
    parser.add_argument('--base-url', default='http://127.0.0.1:8080')
    parser.add_argument('--report', default='stripe-sandbox-results.json')
    args = parser.parse_args()
    os.umask(0o077)
    cfg = {}
    for line in Path(args.env_file).read_text().splitlines():
        k, sep, v = line.partition('=')
        if sep: cfg[k.strip()] = v.strip().strip('\"\x27')
    assert cfg.get('STRIPE_SECRET_KEY', '').startswith('sk_test_'), 'Test key required'
    assert cfg.get('STRIPE_TEST_MODE', '').lower() == 'true', 'Test mode required'
    assert cfg.get('PAYMENT_TEST_AUTOMATION_ENABLED', '').lower() == 'true', 'Automation flag required'
    token = Path(args.token_file).read_text().strip()
    headers = {'Authorization': 'Bearer ' + token, 'Content-Type': 'application/json',
               'X-Sandbox-Token': cfg['STRIPE_SANDBOX_TOKEN']}
    results = []

    def request(url, headers, body=None, expected=200):
        req = urllib.request.Request(url, data=None if body is None else body, headers=headers)
        try:
            with urllib.request.urlopen(req, timeout=45) as response:
                data = response.read()
                assert response.status == expected, 'Unexpected HTTP status'
                return json.loads(data) if data else {}
        except urllib.error.HTTPError as error:
            # Deliberately don't output provider response bodies or signed payment URLs.
            if error.code == expected: return {}
            raise RuntimeError('HTTP ' + str(error.code) + ' at ' + urllib.parse.urlsplit(url).path) from None

    def api(path, body=None, expected=200, override=None):
        data = request(args.base_url + '/api/v1/' + path, headers if override is None else override,
                       None if body is None else json.dumps(body).encode(), expected)
        return data.get('data', data)

    def stripe(path, body=None):
        return request('https://api.stripe.com/v1/' + path, {'Authorization': 'Bearer ' + cfg['STRIPE_SECRET_KEY']},
                       None if body is None else urllib.parse.urlencode(body).encode())

    def sql(query):
        result = subprocess.run(['docker', 'exec', '-i', 'postgres_db', 'sh', '-c',
                                 'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Atq'],
                                input=query, text=True, capture_output=True)
        if result.returncode: raise RuntimeError('Fixture/assertion SQL failed (details suppressed)')
        return result.stdout.strip()

    def wait_for(check, timeout=120):
        deadline = time.monotonic() + timeout
        while time.monotonic() < deadline:
            value = check()
            if value: return value
            time.sleep(1)
        raise AssertionError('Timed out awaiting gateway/webhook evidence')

    def record(name, **details):
        entry = {'case': name, 'status': 'PASS', **details}
        results.append(entry)
        Path(args.report).write_text(json.dumps(results, indent=2) + '\n')
        print(json.dumps(entry), flush=True)

    assert stripe('balance')['livemode'] is False
    api('users/me')  # Fail early if the test customer's access token expired.
    assert sql(f"SELECT start_time > timezone('Asia/Ho_Chi_Minh', now()) + interval '24 hours' FROM showtimes WHERE showtime_id={args.full_showtime}") == 't'
    seats = [int(x) for x in sql(f"SELECT seat_id FROM seats WHERE room_id={args.room} AND status='ACTIVE' AND coalesce(is_deleted,false)=false ORDER BY seat_id LIMIT 30").splitlines()]
    used = set()

    def booking(showtime):
        for seat in seats:
            if seat in used: continue
            try:
                api('seats/pre-lock', {'showtimeId': showtime, 'seatIds': [seat]})
                data = api('bookings', {'showtimeId': showtime, 'seatIds': [seat], 'paymentMethod': 'STRIPE', 'foodItems': []})
                used.add(seat)
                return int(data['bookingId'])
            except RuntimeError as e:
                if 'HTTP 409' not in str(e) and 'HTTP 400' not in str(e): raise
        raise RuntimeError('No available sandbox seats')

    # Seed independent showtimes rather than rewriting existing customer showtimes.
    def seed(hours):
        assert sql(f"SELECT count(*) FROM showtimes WHERE room_id={args.room} AND coalesce(is_deleted,false)=false AND status<>'CANCELLED' AND start_time < timezone('Asia/Ho_Chi_Minh', now())+interval '{hours+3} hours' AND end_time > timezone('Asia/Ho_Chi_Minh', now())+interval '{hours} hours'") == '0', 'Fixture overlaps a real showtime'
        value = sql(f"INSERT INTO showtimes(room_id,movie_id,format,start_time,end_time,base_price,status,is_deleted,original_duration_micros) VALUES ({args.room},{args.movie},'2D',timezone('Asia/Ho_Chi_Minh', now())+interval '{hours} hours',timezone('Asia/Ho_Chi_Minh', now())+interval '{hours+3} hours',70000,'AVAILABLE',false,10800000000) RETURNING showtime_id")
        return int(value)

    if args.seed:
        partial80, partial50 = seed(12), seed(4)
    else:
        assert args.partial80_showtime and args.partial50_showtime, 'Supply partial fixture IDs or --seed'
        partial80, partial50 = args.partial80_showtime, args.partial50_showtime
    record('fixtures', full_showtime=args.full_showtime, partial80_showtime=partial80, partial50_showtime=partial50)
    for percent, showtime in [(100,args.full_showtime),(80,partial80),(50,partial50)]:
        bid = booking(showtime)
        driver = f'bookings/{bid}/stripe/sandbox/payment-intent'
        if percent == 100:
            bad = dict(headers); bad.pop('X-Sandbox-Token')
            api(driver, {'scenario': 'SUCCESS'}, expected=403, override=bad)
            record('automation_token_required', booking=bid)
        response = api(driver, {'scenario': 'SUCCESS'})
        intent = response['paymentIntentId']
        wait_for(lambda: api(f'bookings/{bid}')['status'] == 'PAID')
        assert stripe('payment_intents/' + intent)['status'] == 'succeeded'
        # Prove a signed Stripe event was actually consumed; DB PAID alone could come from reconciliation.
        events = stripe('events?type=payment_intent.succeeded&limit=100')['data']
        event = next(e for e in events if e['data']['object']['id'] == intent)
        eventid = event['id']; assert eventid.startswith('evt_') and eventid.replace('_','').isalnum()
        wait_for(lambda: sql(f"SELECT count(*) FROM stripe_webhook_inbox WHERE event_id='{eventid}' AND processed_at IS NOT NULL") == '1')
        assert sql(f"SELECT count(*) FROM payment_receipts WHERE booking_id={bid}") == '1'
        assert sql(f"SELECT count(*) FROM tickets WHERE booking_id={bid} AND status='VALID'") == '1'
        quote = api(f'bookings/{bid}/cancellation-quote')
        paid = decimal.Decimal(sql(f'SELECT amount FROM payment_receipts WHERE booking_id={bid}'))
        expected = (paid * percent / 100).quantize(decimal.Decimal('1'), rounding=decimal.ROUND_HALF_UP)
        assert decimal.Decimal(str(quote['totalRefundAmount'])) == expected
        cancel = {'quoteToken': quote['quoteToken'], 'expectedRefundAmount': int(expected), 'reason': 'Stripe Sandbox automated verification'}
        api(f'bookings/{bid}/cancel', cancel)
        wait_for(lambda: sql(f"SELECT status FROM refunds WHERE booking_id={bid}") == 'SUCCEEDED', timeout=150)
        rid = sql(f'SELECT gateway_transaction_no FROM refunds WHERE booking_id={bid}')
        provider = stripe('refunds/' + rid)
        assert provider['status'] == 'succeeded' and provider['amount'] == int(expected) and provider['payment_intent'] == intent
        assert api(f'bookings/{bid}')['status'] == 'CANCELLED'
        assert sql(f"SELECT count(*) FROM tickets WHERE booking_id={bid} AND status='VALID'") == '0'
        assert len(stripe('refunds?payment_intent=' + intent)['data']) == 1
        record('payment_and_refund_' + str(percent), booking=bid, payment_intent=intent, refund_id=rid, paid_vnd=int(paid), refunded_vnd=int(expected), signed_event=eventid)

    for scenario in ['CARD_DECLINED', 'INSUFFICIENT_FUNDS', 'THREE_D_SECURE']:
        bid=booking(args.full_showtime)
        api(f'bookings/{bid}/stripe/sandbox/payment-intent', {'scenario':scenario}, expected=200 if scenario=='THREE_D_SECURE' else 400)
        intent=sql(f'SELECT intent_id FROM stripe_payments WHERE booking_id={bid}')
        provider=stripe('payment_intents/'+intent)
        assert provider['status'] == ('requires_action' if scenario=='THREE_D_SECURE' else 'requires_payment_method')
        assert api(f'bookings/{bid}')['status']=='PENDING'
        assert sql(f'SELECT count(*) FROM payment_receipts WHERE booking_id={bid}')=='0'
        assert sql(f"SELECT count(*) FROM tickets WHERE booking_id={bid} AND status='VALID'")=='0'
        # Test fixture cleanup; no money captured, and normal booking expiry releases seats.
        stripe('payment_intents/'+intent+'/cancel', {})
        sql(f"UPDATE bookings SET payment_deadline=timezone('Asia/Ho_Chi_Minh', now())-interval '1 minute',reconciliation_deadline=timezone('Asia/Ho_Chi_Minh', now())-interval '30 seconds',expires_at=timezone('Asia/Ho_Chi_Minh', now())-interval '1 second' WHERE booking_id={bid} AND status='PENDING'; UPDATE stripe_payments SET deadline=timezone('Asia/Ho_Chi_Minh', now())-interval '1 minute' WHERE booking_id={bid}")
        record(scenario.lower(), booking=bid, payment_intent=intent, provider_status=provider['status'])
    print('Stripe API-level Sandbox E2E complete. Checkout UI, 3DS challenge completion and load tests are not covered.', flush=True)

if __name__ == '__main__':
    main()
