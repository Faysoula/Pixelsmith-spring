package com.example.pixelsmith;

public class UserSession {
    private static Integer currentUserId;

    public static void setCurrentUserId(Integer userId) {
        currentUserId = userId;
    }

    public static Integer getCurrentUserId() {
        return currentUserId;
    }

    public static void clear() {
        currentUserId = null;
    }
}

