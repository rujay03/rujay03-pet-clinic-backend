# Vaccination SMS Reminders

This backend now supports vaccination reminder SMS messages for pet owners.

## What is included

- Automatic scheduler: daily job at `app.sms.vaccination-reminder-cron`
- Manual trigger endpoint: `POST /api/notifications/vaccination-reminders/send-now`
- Status endpoint: `GET /api/notifications/vaccination-reminders/status`
- Sender abstraction with safe dev default:
  - `MockSmsSender` (logs only)
  - `TwilioSmsSender` (real SMS)
- Dispatch history table to avoid duplicate sends:
  - `vaccination_reminder_dispatch`

## Reminder selection rules

A reminder is sent when all conditions are true:

- Appointment date = `runDate + vaccination-days-before`
- Appointment status is `PENDING` or `CONFIRMED`
- Appointment type looks like vaccination (`vacc`, `rabies`, `distemper`, `parvo`)
- A successful reminder for the same appointment/date was not already sent

## Configuration

Set in `src/main/resources/application-dev.yml` (or your active profile):

```yaml
app:
  sms:
    enabled: true
    mock-enabled: true
    fail-on-error: false
    vaccination-days-before: 1
    vaccination-reminder-cron: "0 0 9 * * *"
    twilio-account-sid: ""
    twilio-auth-token: ""
    twilio-from-number: ""
```

### Real SMS (Twilio)

Use real SMS by setting:

- `app.sms.enabled=true`
- `app.sms.mock-enabled=false`
- valid Twilio credentials and from number

## Manual run examples

```powershell
# Run for today
curl -X POST "http://localhost:8081/api/notifications/vaccination-reminders/send-now"

# Run for a custom date
curl -X POST "http://localhost:8081/api/notifications/vaccination-reminders/send-now?runDate=2026-04-19"

# Check status
curl "http://localhost:8081/api/notifications/vaccination-reminders/status"
```

## Notes

- Phone numbers are normalized to E.164 when possible.
- Dev mode uses mock logging by default, so no real SMS is sent.

