package com.example.clotheshelper.outfit;

import com.example.clotheshelper.storage.SavedClothingItem;
import com.example.clotheshelper.weather.WeatherSnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public final class OutfitRecommendationService {
    private static final int MINIMUM_ACCEPTABLE_SCORE = 35;

    public Recommendation generate(List<SavedClothingItem> items, WeatherSnapshot weather) {
        int feelsLike = (int) Math.floor(weather.apparentTemperatureCelsius());
        Plan plan = createPlan(feelsLike);
        List<Pick> picks = new ArrayList<>();
        List<MissingSlot> missingRequired = new ArrayList<>();
        List<MissingSlot> missingOptional = new ArrayList<>();
        Set<String> usedItemIds = new HashSet<>();
        boolean onePieceSelected = false;

        for (Slot slot : plan.slots()) {
            if (slot.role() == Role.BOTTOM && onePieceSelected) {
                continue;
            }

            Optional<ScoredItem> candidate = findBestCandidate(items, slot, feelsLike, usedItemIds);
            if (candidate.isEmpty()) {
                MissingSlot missingSlot = new MissingSlot(slot.label(), slot.advice());
                if (slot.required()) {
                    missingRequired.add(missingSlot);
                } else {
                    missingOptional.add(missingSlot);
                }
                continue;
            }

            SavedClothingItem item = candidate.get().item();
            picks.add(new Pick(slot.label(), item));
            usedItemIds.add(item.id());
            onePieceSelected = onePieceSelected || (feelsLike >= 12 && slot.role() == Role.BASE_TOP && isOnePiece(item));
        }

        return new Recommendation(
                plan.title(),
                plan.guidance(),
                feelsLike,
                picks,
                missingRequired,
                missingOptional
        );
    }

    private Optional<ScoredItem> findBestCandidate(
            List<SavedClothingItem> items,
            Slot slot,
            int feelsLike,
            Set<String> usedItemIds
    ) {
        ScoredItem bestItem = null;
        for (SavedClothingItem item : items) {
            if (item == null || usedItemIds.contains(item.id())) {
                continue;
            }

            int score = score(item, slot, feelsLike);
            if (score < MINIMUM_ACCEPTABLE_SCORE) {
                continue;
            }

            if (bestItem == null || score > bestItem.score()) {
                bestItem = new ScoredItem(item, score);
            }
        }
        return Optional.ofNullable(bestItem);
    }

    private int score(SavedClothingItem item, Slot slot, int feelsLike) {
        String type = normalize(item.clothingType());
        int score;
        if (slot.preferredTypes().contains(type)) {
            score = 100;
        } else if (slot.fallbackTypes().contains(type)) {
            score = 62;
        } else {
            return Integer.MIN_VALUE;
        }

        score += seasonScore(normalize(item.season()), feelsLike);
        score += warmthScore(type, feelsLike);
        score += occasionScore(normalize(item.wearOccasion()));
        score += vibeScore(normalize(item.vibe()), feelsLike);

        if (item.hasPhoto()) {
            score += 2;
        }
        if (cleanText(item.name()) != null) {
            score += 1;
        }

        return score;
    }

    private Plan createPlan(int feelsLike) {
        if (feelsLike >= 24) {
            return new Plan(
                    "Light one-layer outfit",
                    "It feels warm in Prague, so breathable pieces should do the job.",
                    List.of(
                            baseTop(true),
                            hotBottom(),
                            lightShoes(),
                            optionalHat()
                    )
            );
        }

        if (feelsLike >= 18) {
            return new Plan(
                    "Easy one-layer outfit",
                    "Comfortable weather: start light, keep an extra layer optional.",
                    List.of(
                            baseTop(true),
                            regularBottom(),
                            lightShoes(),
                            lightExtraLayer()
                    )
            );
        }

        if (feelsLike >= 12) {
            return new Plan(
                    "Two-layer outfit",
                    "A base layer plus a hoodie, sweatshirt, or sweater should feel right.",
                    List.of(
                            baseTop(true),
                            midLayer(true),
                            regularBottom(),
                            regularShoes()
                    )
            );
        }

        if (feelsLike >= 6) {
            return new Plan(
                    "Three-layer outfit",
                    "Cool weather: base, middle layer, and light outerwear.",
                    List.of(
                            baseTop(true),
                            midLayer(true),
                            lightOuterwear(true),
                            regularBottom(),
                            regularShoes()
                    )
            );
        }

        if (feelsLike >= 0) {
            return new Plan(
                    "Warm three-layer outfit",
                    "Cold weather: build warmth with a middle layer and proper outerwear.",
                    List.of(
                            baseTop(true),
                            midLayer(true),
                            warmOuterwear(true),
                            regularBottom(),
                            warmShoes(),
                            optionalHat(),
                            optionalScarf()
                    )
            );
        }

        if (feelsLike >= -6) {
            return new Plan(
                    "Winter outfit",
                    "Below zero: prioritize a warm coat, covered shoes, and useful accessories.",
                    List.of(
                            baseTop(true),
                            warmMidLayer(true),
                            warmOuterwear(true),
                            warmBottom(),
                            warmShoes(),
                            optionalHat(),
                            optionalScarf(),
                            optionalGloves()
                    )
            );
        }

        return new Plan(
                "Deep winter outfit",
                "Very cold weather: choose the warmest available layers and add accessories if you have them.",
                List.of(
                        baseTop(true),
                        warmMidLayer(true),
                        heavyOuterwear(true),
                        warmBottom(),
                        warmShoes(),
                        optionalHat(),
                        optionalScarf(),
                        optionalGloves()
                )
        );
    }

    private Slot baseTop(boolean required) {
        return new Slot(
                "Base layer",
                Role.BASE_TOP,
                required,
                types("tshirt", "shirt", "blouse", "top", "dress"),
                types("suit", "underwear"),
                "Add a t-shirt, top, shirt, or another base layer."
        );
    }

    private Slot midLayer(boolean required) {
        return new Slot(
                "Middle layer",
                Role.MID_TOP,
                required,
                types("hoodie", "sweatshirt", "sweater"),
                types("blazer", "shirt"),
                "Add a hoodie, sweatshirt, or sweater."
        );
    }

    private Slot warmMidLayer(boolean required) {
        return new Slot(
                "Warm middle layer",
                Role.MID_TOP,
                required,
                types("sweater", "hoodie", "sweatshirt"),
                types("blazer", "shirt"),
                "Add a warm sweater, hoodie, or sweatshirt."
        );
    }

    private Slot lightExtraLayer() {
        return new Slot(
                "Optional extra layer",
                Role.MID_TOP,
                false,
                types("shirt", "blazer", "hoodie", "sweatshirt"),
                types("sweater", "jacket"),
                "Add a light shirt, blazer, hoodie, or sweatshirt for later."
        );
    }

    private Slot lightOuterwear(boolean required) {
        return new Slot(
                "Outerwear",
                Role.OUTERWEAR,
                required,
                types("jacket", "blazer"),
                types("coat"),
                "Add a jacket, blazer, or light coat."
        );
    }

    private Slot warmOuterwear(boolean required) {
        return new Slot(
                "Outerwear",
                Role.OUTERWEAR,
                required,
                types("coat", "jacket"),
                types("blazer"),
                "Add a jacket, coat, or warmer outer layer."
        );
    }

    private Slot heavyOuterwear(boolean required) {
        return new Slot(
                "Warm outerwear",
                Role.OUTERWEAR,
                required,
                types("coat"),
                types("jacket"),
                "Add a warm coat or the heaviest jacket you have."
        );
    }

    private Slot hotBottom() {
        return new Slot(
                "Bottom",
                Role.BOTTOM,
                true,
                types("shorts", "skirt", "pants"),
                types("jeans", "leggings"),
                "Add shorts, a skirt, pants, or jeans."
        );
    }

    private Slot regularBottom() {
        return new Slot(
                "Bottom",
                Role.BOTTOM,
                true,
                types("pants", "jeans", "leggings", "skirt"),
                types("shorts"),
                "Add pants, jeans, leggings, or a skirt."
        );
    }

    private Slot warmBottom() {
        return new Slot(
                "Warm bottom",
                Role.BOTTOM,
                true,
                types("pants", "jeans", "leggings"),
                types("skirt"),
                "Add pants, jeans, or leggings."
        );
    }

    private Slot lightShoes() {
        return new Slot(
                "Shoes",
                Role.SHOES,
                true,
                types("sandals", "sneakers", "shoes"),
                types("boots"),
                "Add sandals, sneakers, or shoes."
        );
    }

    private Slot regularShoes() {
        return new Slot(
                "Shoes",
                Role.SHOES,
                true,
                types("sneakers", "shoes", "boots"),
                types("sandals"),
                "Add sneakers, shoes, or boots."
        );
    }

    private Slot warmShoes() {
        return new Slot(
                "Warm shoes",
                Role.SHOES,
                true,
                types("boots", "sneakers", "shoes"),
                types("sandals"),
                "Add boots, covered shoes, or sneakers."
        );
    }

    private Slot optionalHat() {
        return accessory("Hat", "hat", "Add a hat for extra warmth.");
    }

    private Slot optionalScarf() {
        return accessory("Scarf", "scarf", "Add a scarf for extra warmth.");
    }

    private Slot optionalGloves() {
        return accessory("Gloves", "gloves", "Add gloves for freezing weather.");
    }

    private Slot accessory(String label, String type, String advice) {
        return new Slot(
                label,
                Role.ACCESSORY,
                false,
                types(type),
                types("accessory"),
                advice
        );
    }

    private int seasonScore(String season, int feelsLike) {
        if (season == null) {
            return 0;
        }
        if ("any".equals(season)) {
            return 12;
        }

        if (feelsLike >= 24) {
            return switch (season) {
                case "hot" -> 35;
                case "warm" -> 18;
                case "cozy" -> -20;
                case "freezing" -> -60;
                default -> 0;
            };
        }

        if (feelsLike >= 18) {
            return switch (season) {
                case "warm" -> 30;
                case "hot" -> 12;
                case "cozy" -> -8;
                case "freezing" -> -45;
                default -> 0;
            };
        }

        if (feelsLike >= 12) {
            return switch (season) {
                case "warm" -> 20;
                case "cozy" -> 15;
                case "rainy" -> 8;
                case "hot" -> -20;
                default -> 0;
            };
        }

        if (feelsLike >= 6) {
            return switch (season) {
                case "cozy" -> 30;
                case "rainy" -> 15;
                case "freezing" -> 10;
                case "hot" -> -40;
                default -> 0;
            };
        }

        if (feelsLike >= 0) {
            return switch (season) {
                case "freezing" -> 30;
                case "cozy" -> 20;
                case "rainy" -> 8;
                case "hot" -> -50;
                default -> 0;
            };
        }

        return switch (season) {
            case "freezing" -> 40;
            case "cozy" -> 10;
            case "hot" -> -70;
            case "warm" -> -20;
            default -> 0;
        };
    }

    private int warmthScore(String type, int feelsLike) {
        if (feelsLike >= 24) {
            if (isAny(type, "shorts", "sandals")) {
                return 30;
            }
            if (isAny(type, "tshirt", "top", "shirt", "blouse", "dress", "skirt")) {
                return 20;
            }
            if (isAny(type, "sweater", "hoodie", "sweatshirt", "boots")) {
                return -35;
            }
            if (isAny(type, "jacket", "coat")) {
                return -70;
            }
            return 0;
        }

        if (feelsLike >= 18) {
            if (isAny(type, "tshirt", "top", "shirt", "blouse", "dress", "sneakers", "shoes")) {
                return 20;
            }
            if (isAny(type, "shorts", "skirt", "sandals")) {
                return 10;
            }
            if (isAny(type, "jacket", "coat")) {
                return -40;
            }
            return 0;
        }

        if (feelsLike >= 12) {
            if (isAny(type, "hoodie", "sweatshirt", "sweater")) {
                return 20;
            }
            if (isAny(type, "jacket", "blazer", "sneakers", "shoes")) {
                return 10;
            }
            if (isAny(type, "shorts", "sandals")) {
                return -40;
            }
            return 0;
        }

        if (feelsLike >= 6) {
            if (isAny(type, "jacket", "coat")) {
                return 25;
            }
            if (isAny(type, "hoodie", "sweatshirt", "sweater")) {
                return 18;
            }
            if (isAny(type, "boots")) {
                return 12;
            }
            if (isAny(type, "shorts", "sandals")) {
                return -80;
            }
            return 0;
        }

        if (feelsLike >= 0) {
            if (isAny(type, "coat")) {
                return 35;
            }
            if (isAny(type, "jacket", "boots")) {
                return 25;
            }
            if (isAny(type, "hoodie", "sweatshirt", "sweater")) {
                return 18;
            }
            if (isAny(type, "shorts", "sandals")) {
                return -100;
            }
            return 0;
        }

        if (isAny(type, "coat")) {
            return 45;
        }
        if (isAny(type, "boots")) {
            return 35;
        }
        if (isAny(type, "hoodie", "sweatshirt", "sweater")) {
            return 25;
        }
        if (isAny(type, "jacket")) {
            return 10;
        }
        if (isAny(type, "shorts", "sandals")) {
            return -120;
        }
        return 0;
    }

    private int occasionScore(String occasion) {
        if (occasion == null) {
            return 0;
        }
        return switch (occasion) {
            case "everyday" -> 8;
            case "any" -> 6;
            case "sport" -> 3;
            default -> 0;
        };
    }

    private int vibeScore(String vibe, int feelsLike) {
        if (vibe == null) {
            return 0;
        }
        if (feelsLike <= 10) {
            return switch (vibe) {
                case "cozy" -> 8;
                case "casual", "streetwear" -> 4;
                default -> 0;
            };
        }

        if (feelsLike >= 22) {
            return switch (vibe) {
                case "casual", "sporty" -> 4;
                default -> 0;
            };
        }

        return switch (vibe) {
            case "casual", "streetwear" -> 3;
            default -> 0;
        };
    }

    private boolean isOnePiece(SavedClothingItem item) {
        return isAny(normalize(item.clothingType()), "dress", "suit");
    }

    private boolean isAny(String value, String... options) {
        if (value == null) {
            return false;
        }
        for (String option : options) {
            if (value.equals(option)) {
                return true;
            }
        }
        return false;
    }

    private Set<String> types(String... values) {
        Set<String> types = new HashSet<>();
        for (String value : values) {
            types.add(normalize(value));
        }
        return Set.copyOf(types);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private String cleanText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private enum Role {
        BASE_TOP,
        MID_TOP,
        OUTERWEAR,
        BOTTOM,
        SHOES,
        ACCESSORY
    }

    public record Recommendation(
            String title,
            String guidance,
            int feelsLike,
            List<Pick> picks,
            List<MissingSlot> missingRequired,
            List<MissingSlot> missingOptional
    ) {
        public boolean hasPicks() {
            return !picks.isEmpty();
        }

        public boolean isComplete() {
            return missingRequired.isEmpty();
        }
    }

    public record Pick(String slotLabel, SavedClothingItem item) {
    }

    public record MissingSlot(String label, String advice) {
    }

    private record Plan(String title, String guidance, List<Slot> slots) {
    }

    private record Slot(
            String label,
            Role role,
            boolean required,
            Set<String> preferredTypes,
            Set<String> fallbackTypes,
            String advice
    ) {
    }

    private record ScoredItem(SavedClothingItem item, int score) {
    }
}
