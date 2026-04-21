# Mobile Integration Guide: Ultra-Elite Security & Reliability

This document provides the technical specifications for the mobile team to synchronize the Smart Attendance Mobile App with the newly hardened backend security and reliability layers.

## 🛡️ Security: HMAC-SHA256 Signing

To prevent heartbeat spoofing, every heartbeat request MUST be signed with a student-specific `secretKey`.

### 1. Retrieve the Secret Key
The `secretKey` is returned by the backend during the **Student Setup** or **Login** response within the `user` object.
```json
{
  "accessToken": "...",
  "user": {
    "id": "...",
    "secretKey": "BASE64_OR_HEX_STRING"
  }
}
```

### 2. Construct the Payload String
Create a pipe-delimited string of the heartbeat fields in the **EXACT** following order. Latitude and longitude should be formatted to 6 decimal places.

**Format:**
`studentId|sessionId|latitude|longitude|stepCount|batteryLevel`

**Example:**
`550e8400-e29b-41d4-a716-446655440000|660f9511-f39c-52e5-b817-557766551111|12.971600|77.594600|150|85`

### 3. Generate the Signature
Generate an HMAC-SHA256 hash of the payload string using the `secretKey`.

**React Native (Standard HMAC Implementation with crypto-js):**
```typescript
import CryptoJS from 'crypto-js';

const signature = CryptoJS.HmacSHA256(payload, secretKey).toString(CryptoJS.enc.Hex);
```

**Using Web Crypto API (Web Students):**
```typescript
// Used in the Student Web Dashboard
const signHeartbeat = async (payload, secretKey) => {
  const encoder = new TextEncoder();
  const keyBuffer = await crypto.subtle.importKey(
    'raw', encoder.encode(secretKey),
    { name: 'HMAC', hash: 'SHA-256' },
    false, ['sign']
  );
  const signature = await crypto.subtle.sign('HMAC', keyBuffer, encoder.encode(payload));
  return Array.from(new Uint8Array(signature)).map(b => b.toString(16).padStart(2, '0')).join('');
}
```

## 📈 Reliability: Sequence Tracking

To ensure packets are processed in order and prevent replay attacks, a `sequenceId` must be included.

1.  Maintain a local `sequenceId` counter per session, starting at `1`.
2.  Increment the counter for every heartbeat sent.
3.  Reset the counter when a new session begins.
4.  Include the `sequenceId` (integer) in the JSON body of the heartbeat request.

## 📦 Final Heartbeat DTO

The heartbeat request body should now look like this:

```json
{
  "studentId": "...",
  "sessionId": "...",
  "latitude": 12.9716,
  "longitude": 77.5946,
  "stepCount": 150,
  "batteryLevel": 85,
  "requestSignature": "ae34f...21bc",
  "sequenceId": 42
}
```

## 🚨 Backend Rejection Errors
- **401 Unauthorized**: Missing or invalid `secretKey` / JWT.
- **403 Forbidden**: Invalid `requestSignature` (Signature mismatch).
- **Security Alert**: Repeated signature failures will trigger a security anomaly on the admin dashboard.
