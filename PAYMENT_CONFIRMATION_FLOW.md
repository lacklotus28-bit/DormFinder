# Payment Confirmation Flow in DormFinder

## Overview
The DormFinder app has a complete payment workflow where students submit payments and landlords verify/confirm them before bookings are finalized.

---

## 📱 Student Side: How Payment Works

### Step 1: Booking Approval
1. Student submits a booking request
2. **Status**: `pending` → Landlord reviews and approves
3. **Status**: `approved` → Student can now make payment

### Step 2: Student Makes Payment
When booking status is `approved`, student sees **"Pay Now"** button

**Payment Methods Available:**
- **GCash** (online - simulated in test mode)
- **PayMaya** (online - simulated in test mode)
- **Cash** (requires proof of payment upload)

#### For Online Payment (GCash/PayMaya):
```
Location: PaymentActivity.java
1. Student selects payment method (GCash or PayMaya)
2. Clicks "Proceed to Payment"
3. In test mode: Simulates payment success
4. Payment status: "completed"
5. Booking status: "paid"
6. Booking paymentStatus: "paid"
```

#### For Cash Payment:
```
Location: PaymentActivity.java
1. Student selects "Cash" payment method
2. Upload section appears
3. Student uploads payment proof (photo/screenshot)
4. Clicks "Submit Payment Proof"
5. Image uploads to Firebase Storage
6. Payment status: "pending" (waiting for landlord verification)
7. Booking status: "paid"
8. Booking paymentStatus: "pending"
9. Notification sent to landlord
```

### Step 3: Waiting for Confirmation (Cash Payments Only)
**Location**: `StudentBookingAdapter.java` (line 147-155)

When `status = "approved"` AND `paymentStatus = "pending"`:
```java
holder.tvStatusDescription.setText(
    "⏳ Payment verification in progress. Please wait for landlord confirmation."
);
```

Student sees:
- 🔵 **Status**: APPROVED
- 🟡 **Payment Status**: PAYMENT PENDING
- **Message**: "⏳ Payment verification in progress. Please wait for landlord confirmation."
- **Button**: "View Payment" (to see payment details)

---

## 🏠 Landlord Side: How to Confirm Payments

### Where Landlords Manage Payments
**Activity**: `LandlordPaymentManagementActivity.java`
**Access**: Landlord Home → "Payment Management" or "Manage Payments"

### Payment List View
Landlords see all payments with:
- Student name and email
- Dormitory name
- Amount
- Payment method
- Status (Pending/Completed/Rejected)

### Confirming Cash Payments

#### Step 1: View Pending Payment
When landlord clicks on a **pending cash payment**:
```
Location: LandlordPaymentManagementActivity.java → showCashPaymentVerification()
```

Dialog shows:
```
Title: "Verify Cash Payment"
Content:
  Student: [Student Name]
  Dormitory: [Dorm Name]
  Amount: ₱[Amount]
  Payment Method: Cash
  Reference: [Reference Number]

Buttons:
  [View Proof] - Opens full-size payment proof image
  [✓ Approve]  - Confirm payment received
  [✗ Reject]   - Reject payment (requires reason)
```

#### Step 2: View Payment Proof
**Button**: "View Proof"
- Opens `ImageViewActivity` showing the uploaded payment proof
- Landlord can verify if payment matches the amount

#### Step 3: Approve Payment
**Location**: `LandlordPaymentManagementActivity.java → approvePayment()`

When landlord clicks **"✓ Approve"**:

```java
1. Update Payment:
   - paymentStatus: "pending" → "completed"
   
2. Update Booking:
   - status: "paid" → "confirmed"  // ✅ Key change!
   - paymentStatus: "pending" → "paid"
   - paymentId: [linked]
   - paymentDate: [timestamp]
   - confirmedDate: [timestamp]

3. Update Dormitory:
   - availableRooms: decremented by 1

4. Send Notification to Student:
   - Type: "payment_approved"
   - Title: "Payment Approved!"
   - Message: "Your payment for [Dorm] has been verified and approved."
```

#### Step 4: Reject Payment
**Location**: `LandlordPaymentManagementActivity.java → rejectPayment()`

When landlord clicks **"✗ Reject"**:

```java
1. Landlord enters rejection reason (required)

2. Update Payment:
   - status: "pending" → "failed"
   - failureReason: [landlord's reason]

3. Send Notification to Student:
   - Type: "payment_rejected"
   - Title: "Payment Verification Failed"
   - Message: "Your payment was rejected. Reason: [reason]"
```

Student can then:
- View the rejection reason
- Submit payment again with correct proof
- Contact landlord for clarification

---

## 📊 Status Flow Diagram

### Complete Payment Confirmation Flow
```
BOOKING REQUEST
    ↓
PENDING (Landlord reviews)
    ↓
APPROVED (Student can pay)
    ↓
┌─────────────────────────────────┬──────────────────────────────┐
│    ONLINE PAYMENT               │     CASH PAYMENT             │
│    (GCash/PayMaya)              │                              │
├─────────────────────────────────┼──────────────────────────────┤
│ 1. Auto-confirmed (test mode)   │ 1. Upload payment proof      │
│ 2. Payment: "completed"         │ 2. Payment: "pending"        │
│ 3. Booking: "paid"               │ 3. Booking: "paid"           │
│ 4. PaymentStatus: "paid"        │ 4. PaymentStatus: "pending"  │
│                                  │ 5. Landlord reviews proof    │
│                                  │    ↓                         │
│                                  │ ┌─────────┬─────────┐       │
│                                  │ │ APPROVE │ REJECT  │       │
│                                  │ └────┬────┴────┬────┘       │
│                                  │      ↓         ↓            │
└─────────────────┬────────────────┴──────┘         │           │
                  ↓                                  ↓           │
            CONFIRMED                          FAILED           │
    (Payment verified by landlord)      (Student resubmits)     │
    - Booking: "confirmed"                                      │
    - PaymentStatus: "paid"                                     │
    - Rooms: -1                                                 │
```

---

## 🔔 Notifications

### To Student
1. **Payment Submitted** (automatic after upload)
   - Shows in notification center
   - Can track in Payment History

2. **Payment Approved** (when landlord approves)
   ```
   Title: "Payment Approved!"
   Message: "Your payment for [Dormitory] has been verified and approved."
   Action: Opens booking details (status now "confirmed")
   ```

3. **Payment Rejected** (when landlord rejects)
   ```
   Title: "Payment Verification Failed"
   Message: "Your payment was rejected. Reason: [landlord's reason]"
   Action: Student can resubmit payment
   ```

### To Landlord
1. **Payment Received** (when student submits)
   ```
   Title: "Payment Received"
   Message: "[Student Name] has submitted payment for [Dormitory]"
   Action: Opens Payment Management to verify
   ```

---

## 💾 Database Structure

### Payment Document (Firestore)
```json
{
  "paymentId": "auto-generated",
  "bookingId": "linked-booking-id",
  "studentId": "student-user-id",
  "landlordId": "landlord-user-id",
  "dormitoryId": "dorm-id",
  "dormitoryName": "Dorm Name",
  "amount": 5000.00,
  "paymentMethod": "cash|gcash|paymaya",
  "status": "pending|completed|failed",
  "timestamp": 1699999999999,
  "referenceNumber": "REF-XXXXXX",
  "studentName": "John Doe",
  "studentEmail": "john@email.com",
  "description": "Monthly rent",
  "paymentProof": "firebase-storage-url",
  "completedDate": 1699999999999,
  "failureReason": "Rejection reason (if rejected)"
}
```

### Booking Document (Firestore)
```json
{
  "bookingId": "auto-generated",
  "status": "pending|approved|confirmed|declined|cancelled",
  "paymentStatus": "unpaid|pending|paid",
  "paymentId": "linked-payment-id",
  "paymentDate": 1699999999999,
  "confirmedDate": 1699999999999,
  // ... other booking fields
}
```

---

## 🎯 Key Points for Landlords

### To Confirm a Cash Payment:
1. **Go to**: Landlord Home → "Payment Management"
2. **Look for**: Payments with status "PENDING VERIFICATION"
3. **Click**: On the pending payment
4. **Review**: Payment proof image
5. **Action**: 
   - **Approve** if payment proof is valid → Booking confirmed ✅
   - **Reject** if proof is invalid → Student notified to resubmit ❌

### Important Notes:
- ✅ Only **cash payments** require manual verification
- ✅ Online payments (GCash/PayMaya) are auto-confirmed in test mode
- ✅ Always check the payment proof carefully before approving
- ✅ Provide clear rejection reasons to help students
- ✅ Confirming payment automatically reduces available rooms
- ✅ Students can resubmit if payment is rejected

---

## 🐛 Troubleshooting

### Student says "Already paid" but status is pending:
- Check Payment Management for the pending payment
- Review the payment proof they uploaded
- Approve or reject based on verification

### Payment proof is unclear:
- Reject the payment with reason "Payment proof not clear"
- Student will be notified to upload clearer proof
- They can resubmit with better image

### Student paid to wrong account:
- Reject payment with specific reason
- Guide them to correct payment method via chat
- They can resubmit with correct proof

---

## 📱 UI Locations Reference

### Student Side:
- **My Bookings**: Shows all booking statuses
- **Payment History**: Shows all payments made
- **Notifications**: Payment approval/rejection alerts

### Landlord Side:
- **Payment Management**: Main hub for all payment verification
- **Booking Requests**: Shows bookings with payment status
- **Notifications**: New payment submission alerts

---

## Summary

The payment confirmation flow ensures secure transactions by:
1. ✅ Requiring proof of payment for cash transactions
2. ✅ Giving landlords control to verify payments
3. ✅ Preventing fraudulent bookings
4. ✅ Automatically updating room availability only after verification
5. ✅ Keeping both parties informed via notifications

**Bottom Line**: For cash payments, students upload proof → landlords verify → booking confirmed!
