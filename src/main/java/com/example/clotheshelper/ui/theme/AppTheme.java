package com.example.clotheshelper.ui.theme;

public enum AppTheme {
    LIGHT(
            "Light",
            "#f9fafb",
            "#ffffff",
            "#f3f4f6",
            "#111827",
            "#4b5563",
            "#d1d5db",
            "#2563eb",
            "#ffffff",
            "#16a34a",
            "#dc2626",
            "#dbeafe",
            "#1d4ed8",
            "#e5e7eb",
            "#111827",
            "#fee2e2",
            "#991b1b"
    ),
    DARK(
            "Dark",
            "#090909",
            "#151515",
            "#202020",
            "#f8fafc",
            "#d4d4d8",
            "#3f3f46",
            "#f59e0b",
            "#111827",
            "#f97316",
            "#f87171",
            "#292524",
            "#fbbf24",
            "#27272a",
            "#fef3c7",
            "#451a1a",
            "#fecaca"
    );

    private final String label;
    private final String background;
    private final String surface;
    private final String mutedSurface;
    private final String text;
    private final String mutedText;
    private final String border;
    private final String primary;
    private final String onPrimary;
    private final String success;
    private final String error;
    private final String infoBackground;
    private final String infoText;
    private final String secondaryBackground;
    private final String secondaryText;
    private final String dangerBackground;
    private final String dangerText;

    AppTheme(
            String label,
            String background,
            String surface,
            String mutedSurface,
            String text,
            String mutedText,
            String border,
            String primary,
            String onPrimary,
            String success,
            String error,
            String infoBackground,
            String infoText,
            String secondaryBackground,
            String secondaryText,
            String dangerBackground,
            String dangerText
    ) {
        this.label = label;
        this.background = background;
        this.surface = surface;
        this.mutedSurface = mutedSurface;
        this.text = text;
        this.mutedText = mutedText;
        this.border = border;
        this.primary = primary;
        this.onPrimary = onPrimary;
        this.success = success;
        this.error = error;
        this.infoBackground = infoBackground;
        this.infoText = infoText;
        this.secondaryBackground = secondaryBackground;
        this.secondaryText = secondaryText;
        this.dangerBackground = dangerBackground;
        this.dangerText = dangerText;
    }

    public String createRootStyle() {
        return "-app-background: " + background + ";"
                + "-app-surface: " + surface + ";"
                + "-app-muted-surface: " + mutedSurface + ";"
                + "-app-text: " + text + ";"
                + "-app-muted-text: " + mutedText + ";"
                + "-app-border: " + border + ";"
                + "-app-primary: " + primary + ";"
                + "-app-on-primary: " + onPrimary + ";"
                + "-app-success: " + success + ";"
                + "-app-error: " + error + ";"
                + "-app-info-background: " + infoBackground + ";"
                + "-app-info-text: " + infoText + ";"
                + "-app-secondary-background: " + secondaryBackground + ";"
                + "-app-secondary-text: " + secondaryText + ";"
                + "-app-danger-background: " + dangerBackground + ";"
                + "-app-danger-text: " + dangerText + ";"
                + "-fx-background-color: -app-background;";
    }

    @Override
    public String toString() {
        return label;
    }
}
