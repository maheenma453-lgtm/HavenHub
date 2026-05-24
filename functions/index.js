const functions = require("firebase-functions");
const admin     = require("firebase-admin");
admin.initializeApp();

const db = admin.firestore();

// Helper: Send notification to a single user by userId
async function sendToUser(userId, title, body, data = {}) {
    if (!userId) return;
    try {
        const doc = await db.collection("users").doc(userId).get();
        if (!doc.exists) return;

        const token = doc.data()?.fcmToken;
        if (!token) return;

        await admin.messaging().send({
            token,
            notification: { title, body },
            data,
            android: {
                priority: "high",
                notification: {
                    channelId:             "channel_bookings",
                    priority:              "high",
                    defaultSound:          true,
                    defaultVibrateTimings: true,
                    visibility:            "public",
                },
            },
        });

        functions.logger.log(`Notification sent to userId=${userId}`);
    } catch (e) {
        functions.logger.error(`sendToUser failed userId=${userId}`, e);
    }
}

// Helper: Send notification to a topic (all admins/landlords/tenants)
async function sendToTopic(topic, title, body, data = {}) {
    try {
        await admin.messaging().send({
            topic,
            notification: { title, body },
            data,
            android: {
                priority: "high",
                notification: {
                    channelId:    "channel_system",
                    priority:     "high",
                    defaultSound: true,
                    visibility:   "public",
                },
            },
        });
        functions.logger.log(`Notification sent to topic=${topic}`);
    } catch (e) {
        functions.logger.error(`sendToTopic failed topic=${topic}`, e);
    }
}

// TRIGGER 1 — New Booking Created
exports.onBookingCreated = functions.firestore
    .document("bookings/{bookingId}")
    .onCreate(async (snap) => {
        const booking   = snap.data();
        const bookingId = snap.id;

        await sendToUser(
            booking.tenantId,
            "Booking Confirmed ✓",
            `Your booking for "${booking.propertyTitle}" has been confirmed.`,
            { type: "booking", subType: "CONFIRMED", referenceId: bookingId }
        );

        await sendToUser(
            booking.landlordId,
            "New Booking Request 📋",
            `${booking.tenantName} has requested to book "${booking.propertyTitle}".`,
            { type: "booking", subType: "REQUESTED", referenceId: bookingId }
        );
    });

// TRIGGER 2 — Booking Status Changed
exports.onBookingUpdated = functions.firestore
    .document("bookings/{bookingId}")
    .onUpdate(async (change) => {
        const before    = change.before.data();
        const after     = change.after.data();
        const bookingId = change.after.id;

        if (before.status === after.status) return;

        const property = after.propertyTitle || "your property";

        if (after.status === "Confirmed") {
            await sendToUser(
                after.tenantId,
                "Booking Confirmed ✓",
                `Your booking for "${property}" has been confirmed.`,
                { type: "booking", subType: "CONFIRMED", referenceId: bookingId }
            );
        } else if (after.status === "Cancelled") {
            await sendToUser(
                after.tenantId,
                "Booking Cancelled",
                `Your booking for "${property}" has been cancelled.`,
                { type: "booking", subType: "CANCELLED", referenceId: bookingId }
            );
        } else if (after.status === "Completed") {
            await sendToUser(
                after.tenantId,
                "Stay Completed 🌟",
                `Your stay at "${property}" is complete. Please leave a review!`,
                { type: "booking", subType: "CONFIRMED", referenceId: bookingId }
            );
        }
    });

// TRIGGER 3 — Payment Created
exports.onPaymentCreated = functions.firestore
    .document("payments/{paymentId}")
    .onCreate(async (snap) => {
        const payment   = snap.data();
        const paymentId = snap.id;

        await sendToUser(
            payment.tenantId,
            "Payment Successful 💚",
            `Payment of Rs. ${payment.amount} has been confirmed. Transaction ID: ${paymentId}`,
            { type: "payment", subType: "SUCCESS", referenceId: paymentId }
        );

        await sendToUser(
            payment.landlordId,
            "Payment Received 💰",
            `Rs. ${payment.amount} received from ${payment.tenantName}.`,
            { type: "payment", subType: "SUCCESS", referenceId: paymentId }
        );
    });

// TRIGGER 4 — New Message Sent
exports.onMessageSent = functions.firestore
    .document("conversations/{convId}/messages/{messageId}")
    .onCreate(async (snap) => {
        const message        = snap.data();
        const conversationId = snap.ref.parent.parent.id;

        await sendToUser(
            message.receiverId,
            `New Message from ${message.senderName} 💬`,
            message.content?.substring(0, 100) || "",
            { type: "message", subType: "NEW", referenceId: conversationId }
        );
    });

// TRIGGER 5 — Property Verification Updated
exports.onPropertyVerified = functions.firestore
    .document("properties/{propertyId}")
    .onUpdate(async (change) => {
        const before     = change.before.data();
        const after      = change.after.data();
        const propertyId = change.after.id;

        if (before.verificationStatus === after.verificationStatus) return;

        const title = after.title || "your property";

        if (after.verificationStatus === "Approved") {
            await sendToUser(
                after.landlordId,
                "Property Approved ✓",
                `Congratulations! Your property "${title}" has been approved.`,
                { type: "property", subType: "APPROVED", referenceId: propertyId }
            );
        } else if (after.verificationStatus === "Rejected") {
            await sendToUser(
                after.landlordId,
                "Property Rejected ✗",
                `Your property "${title}" was not approved. Please check admin notes.`,
                { type: "property", subType: "REJECTED", referenceId: propertyId }
            );
        }
    });

// TRIGGER 6 — User Verification Updated
exports.onUserVerified = functions.firestore
    .document("users/{userId}")
    .onUpdate(async (change) => {
        const before = change.before.data();
        const after  = change.after.data();
        const userId = change.after.id;

        if (before.verificationStatus === after.verificationStatus) return;

        const name = after.fullName || "User";

        if (after.verificationStatus === "Approved") {
            await sendToUser(
                userId,
                "Account Verified ✓",
                `Congratulations ${name}! Your account has been successfully verified.`,
                { type: "system", subType: "USER_VERIFIED", referenceId: userId }
            );
        } else if (after.verificationStatus === "Rejected") {
            await sendToUser(
                userId,
                "Verification Rejected",
                `Your account verification was rejected. Please contact support.`,
                { type: "system", subType: "USER_REJECTED", referenceId: userId }
            );
        }
    });

// TRIGGER 7 — New Property Submitted
exports.onNewPropertySubmitted = functions.firestore
    .document("properties/{propertyId}")
    .onCreate(async (snap) => {
        const property   = snap.data();
        const propertyId = snap.id;

        await sendToTopic(
            "role_admin",
            "New Property Pending Review ⏳",
            `${property.landlordName || "A landlord"} submitted "${property.title}" for review.`,
            { type: "property", subType: "PENDING", referenceId: propertyId }
        );
    });

// TRIGGER 8 — Seasonal Alert Created
exports.onSeasonalAlertCreated = functions.firestore
    .document("seasonal_alerts/{alertId}")
    .onCreate(async (snap) => {
        const alert   = snap.data();
        const alertId = snap.id;

        if (!alert.isActive) return;

        const topic = alert.targetRole === "LANDLORD" ? "role_landlord"
                    : alert.targetRole === "TENANT"   ? "role_tenant"
                    : "all_users";

        await sendToTopic(
            topic,
            alert.title   || "Special Offer 🎉",
            alert.message || "",
            { type: "seasonal", subType: "ALERT", referenceId: alertId }
        );
    });s