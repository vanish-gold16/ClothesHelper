package com.example.clotheshelper.outfit;

import com.example.clotheshelper.enums.OutfitPattern;
import com.example.clotheshelper.enums.OutfitStyle;
import com.example.clotheshelper.storage.SavedClothingItem;
import com.example.clotheshelper.weather.WeatherSnapshot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public final class OutfitRecommendationService {
    private static final int MINIMUM_ACCEPTABLE_SCORE = 35;
    // Only items scoring within this margin of the best are treated as interchangeable
    // and rotated through on repeated "Generate" presses. If everything else is further
    // behind than this, the single best item is returned every time (no forced change).
    // The margin is wide enough to keep adjacent-style items in the mix, while the
    // style-clash penalty (see styleScore) pushes clashing items past it.
    private static final int VARIETY_MARGIN = 40;

    public Recommendation generate(List<SavedClothingItem> items, WeatherSnapshot weather) {
        return generate(items, weather, 0, OutfitPattern.RANDOM, OutfitStyle.ANY);
    }

    public Recommendation generate(List<SavedClothingItem> items, WeatherSnapshot weather, int variant) {
        return generate(items, weather, variant, OutfitPattern.RANDOM, OutfitStyle.ANY);
    }

    public Recommendation generate(
            List<SavedClothingItem> items,
            WeatherSnapshot weather,
            int variant,
            OutfitPattern pattern
    ) {
        return generate(items, weather, variant, pattern, OutfitStyle.ANY);
    }

    public Recommendation generate(
            List<SavedClothingItem> items,
            WeatherSnapshot weather,
            int variant,
            OutfitPattern pattern,
            OutfitStyle style
    ) {
        OutfitPattern safePattern = pattern == null ? OutfitPattern.RANDOM : pattern;
        OutfitStyle safeStyle = style == null ? OutfitStyle.ANY : style;
        int feelsLike = (int) Math.floor(weather.apparentTemperatureCelsius());
        Conditions conditions = new Conditions(weather.isRaining(), weather.isWindy());
        Plan plan = createPlan(feelsLike, conditions);
        List<Pick> picks = new ArrayList<>();
        List<MissingSlot> missingRequired = new ArrayList<>();
        List<MissingSlot> missingOptional = new ArrayList<>();
        Set<String> usedItemIds = new HashSet<>();
        boolean onePieceSelected = false;
        int safeVariant = Math.max(0, variant);
        // For the sandwich pattern we remember the colour of the top once it is chosen,
        // then steer the shoes toward it. The top is always scored before the shoes.
        String topColorHex = null;

        for (Slot slot : plan.slots()) {
            if (slot.role() == Role.BOTTOM && onePieceSelected) {
                continue;
            }

            Optional<ScoredItem> candidate = findBestCandidate(
                    items, slot, feelsLike, conditions, usedItemIds, safeVariant, safePattern, safeStyle, topColorHex);
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
            if (slot.role() == Role.BASE_TOP && topColorHex == null) {
                topColorHex = cleanText(item.mainColorHex());
            }
            onePieceSelected = onePieceSelected || (feelsLike >= 12 && slot.role() == Role.BASE_TOP && isOnePiece(item));
        }

        return new Recommendation(
                plan.title(),
                guidanceFor(plan, conditions, safePattern, safeStyle),
                feelsLike,
                picks,
                missingRequired,
                missingOptional
        );
    }

    private String guidanceFor(Plan plan, Conditions conditions, OutfitPattern pattern, OutfitStyle style) {
        String guidance = guidanceFor(plan, conditions);
        if (pattern == OutfitPattern.SANDWICH) {
            guidance += " Sandwich look: the shoes echo the colour of the top.";
        }
        if (style != null && style != OutfitStyle.ANY) {
            guidance += " Styled for a " + style.toString().toLowerCase(Locale.ROOT) + " look"
                    + adjacentStyleHint(style) + ".";
        }
        return guidance;
    }

    private String adjacentStyleHint(OutfitStyle style) {
        return switch (style) {
            case FORMAL -> ", with room for shiny pieces";
            case SHINE -> ", with room for formal pieces";
            case STREETWEAR -> ", mixing in sporty pieces";
            case SPORTY -> ", mixing in streetwear pieces";
            default -> "";
        };
    }

    private String guidanceFor(Plan plan, Conditions conditions) {
        if (conditions.raining() && conditions.windy()) {
            return plan.guidance() + " Rain and wind expected, so favour a water-resistant jacket or coat.";
        }
        if (conditions.raining()) {
            return plan.guidance() + " Rain expected, so bring a water-resistant layer and skip open shoes.";
        }
        if (conditions.windy()) {
            return plan.guidance() + " It is windy, so a jacket will help cut the chill.";
        }
        return plan.guidance();
    }

    private Optional<ScoredItem> findBestCandidate(
            List<SavedClothingItem> items,
            Slot slot,
            int feelsLike,
            Conditions conditions,
            Set<String> usedItemIds,
            int variant,
            OutfitPattern pattern,
            OutfitStyle style,
            String topColorHex
    ) {
        List<ScoredItem> candidates = new ArrayList<>();
        for (SavedClothingItem item : items) {
            if (item == null || usedItemIds.contains(item.id())) {
                continue;
            }

            int score = score(item, slot, feelsLike, conditions, pattern, style, topColorHex);
            if (score == Integer.MIN_VALUE) {
                continue;
            }
            // Required slots accept the best type-matching item even below the comfort
            // threshold, so a usable base layer is never reported as missing just because
            // its season tag is penalised. Optional slots stay conservative.
            if (!slot.required() && score < MINIMUM_ACCEPTABLE_SCORE) {
                continue;
            }

            candidates.add(new ScoredItem(item, score));
        }

        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        // Keep only the items that score within the margin of the best one. If nothing
        // else comes close, this leaves a single item that is returned on every press,
        // so a clearly best-fitting piece is not swapped out for a worse one.
        candidates.sort(Comparator.comparingInt(ScoredItem::score).reversed());
        int bestScore = candidates.get(0).score();
        List<ScoredItem> pool = new ArrayList<>();
        for (ScoredItem candidate : candidates) {
            if (candidate.score() >= bestScore - VARIETY_MARGIN) {
                pool.add(candidate);
            }
        }

        // Cycle straight through the suitable pool so every press advances to the next
        // item (variant 0 is always the best). The pool only holds items within the
        // margin, so each one is a sensible choice; if there is just one, it is returned
        // every time and nothing changes - which is the desired behaviour when no other
        // item is close enough.
        return Optional.of(pool.get(variant % pool.size()));
    }

    private int score(
            SavedClothingItem item,
            Slot slot,
            int feelsLike,
            Conditions conditions,
            OutfitPattern pattern,
            OutfitStyle style,
            String topColorHex
    ) {
        String type = normalize(item.clothingType());
        int score;
        if (slot.preferredTypes().contains(type)) {
            score = 100;
        } else if (slot.fallbackTypes().contains(type)) {
            score = 62;
        } else {
            return Integer.MIN_VALUE;
        }

        List<String> seasons = normalizeAll(item.seasons());
        score += bestSeasonScore(seasons, feelsLike);
        score += warmthScore(type, feelsLike);
        score += bestOccasionScore(normalizeAll(item.wearOccasions()));
        score += vibeScore(normalize(item.vibe()), feelsLike);
        score += conditionScore(type, seasons, conditions);
        score += patternScore(item, slot, pattern, topColorHex);
        score += styleScore(normalize(item.vibe()), style);

        if (item.hasPhoto()) {
            score += 2;
        }
        if (cleanText(item.name()) != null) {
            score += 1;
        }

        return score;
    }

    // Sandwich styling: reward shoes whose colour is close to the top's colour. The
    // bonus fades smoothly with colour distance so an exact match wins, similar shades
    // still get a nudge, and clashing colours get nothing.
    private int patternScore(SavedClothingItem item, Slot slot, OutfitPattern pattern, String topColorHex) {
        if (pattern != OutfitPattern.SANDWICH || slot.role() != Role.SHOES || topColorHex == null) {
            return 0;
        }

        double distance = colorDistance(topColorHex, item.mainColorHex());
        if (distance < 0) {
            return 0;
        }

        double maxDistance = Math.sqrt(3 * 255.0 * 255.0);
        double closeness = 1.0 - Math.min(distance, maxDistance) / maxDistance;
        return (int) Math.round(closeness * 70);
    }

    private double colorDistance(String hexA, String hexB) {
        int[] rgbA = parseHexColor(hexA);
        int[] rgbB = parseHexColor(hexB);
        if (rgbA == null || rgbB == null) {
            return -1;
        }

        int deltaR = rgbA[0] - rgbB[0];
        int deltaG = rgbA[1] - rgbB[1];
        int deltaB = rgbA[2] - rgbB[2];
        return Math.sqrt((double) deltaR * deltaR + (double) deltaG * deltaG + (double) deltaB * deltaB);
    }

    private int[] parseHexColor(String hex) {
        if (hex == null) {
            return null;
        }

        String value = hex.trim();
        if (value.startsWith("#")) {
            value = value.substring(1);
        }
        if (value.length() != 6) {
            return null;
        }

        try {
            return new int[]{
                    Integer.parseInt(value.substring(0, 2), 16),
                    Integer.parseInt(value.substring(2, 4), 16),
                    Integer.parseInt(value.substring(4, 6), 16)
            };
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private int conditionScore(String type, List<String> seasons, Conditions conditions) {
        int score = 0;
        if (conditions.raining()) {
            if (seasons.contains("rainy")) {
                score += 25;
            }
            if (isAny(type, "jacket", "coat")) {
                score += 15;
            }
            if (isAny(type, "boots")) {
                score += 8;
            }
            if (isAny(type, "sandals")) {
                score -= 30;
            }
        }
        if (conditions.windy() && isAny(type, "jacket", "coat", "blazer")) {
            score += 12;
        }
        return score;
    }

    private Plan createPlan(int feelsLike, Conditions conditions) {
        if (feelsLike >= 22) {
            List<Slot> slots = new ArrayList<>(List.of(
                    baseTop(true),
                    hotBottom(),
                    lightShoes()
            ));
            if (conditions.raining()) {
                slots.add(rainLayer());
            }
            slots.add(optionalHat());
            return new Plan(
                    "Light one-layer outfit",
                    "It feels warm in Prague, so breathable pieces should do the job.",
                    List.copyOf(slots)
            );
        }

        if (feelsLike >= 18) {
            return new Plan(
                    "Easy one-layer outfit",
                    "Comfortable weather: start light, keep an extra layer optional.",
                    List.of(
                            baseTop(true),
                            mildBottom(),
                            lightShoes(),
                            conditions.raining() ? rainLayer() : lightExtraLayer()
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

    private Slot rainLayer() {
        return new Slot(
                "Rain layer",
                Role.OUTERWEAR,
                false,
                types("jacket", "coat"),
                types("blazer"),
                "Add a water-resistant jacket or coat for the rain."
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

    private Slot mildBottom() {
        return new Slot(
                "Bottom",
                Role.BOTTOM,
                true,
                types("pants", "jeans", "shorts", "skirt"),
                types("leggings"),
                "Add pants, jeans, shorts, or a skirt."
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

    private List<String> normalizeAll(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }

        List<String> normalized = new ArrayList<>();
        for (String value : values) {
            String normalizedValue = normalize(value);
            if (normalizedValue != null) {
                normalized.add(normalizedValue);
            }
        }
        return normalized;
    }

    // An item can be tagged for several seasons; reward it for whichever fits the
    // weather best so a "Hot, Warm" piece is not dragged down by its colder tag.
    private int bestSeasonScore(List<String> seasons, int feelsLike) {
        if (seasons.isEmpty()) {
            return 0;
        }

        int best = Integer.MIN_VALUE;
        for (String season : seasons) {
            best = Math.max(best, seasonScore(season, feelsLike));
        }
        return best;
    }

    private int bestOccasionScore(List<String> occasions) {
        int best = 0;
        for (String occasion : occasions) {
            best = Math.max(best, occasionScore(occasion));
        }
        return best;
    }

    private int seasonScore(String season, int feelsLike) {
        if (season == null) {
            return 0;
        }
        if ("any".equals(season)) {
            return 12;
        }

        if (feelsLike >= 22) {
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
        if (feelsLike >= 22) {
            if (isAny(type, "shorts", "sandals")) {
                return 40;
            }
            if (isAny(type, "tshirt", "top", "shirt", "blouse", "dress", "skirt")) {
                return 20;
            }
            // Long bottoms are uncomfortable in real heat, so push them below shorts
            // even when their season tag matches.
            if (isAny(type, "jeans", "pants")) {
                return -10;
            }
            if (isAny(type, "leggings")) {
                return -20;
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

    // Steers the whole outfit toward a chosen style by rewarding matching vibes.
    // Styles sit on a spectrum (formal - shine - streetwear - sporty); a piece is scored
    // by how far its vibe sits from the chosen style on that spectrum. The swing is wide
    // on purpose so a clear clash (e.g. sporty sneakers under "Formal") loses to an
    // on-style item even when the off-style one is a slightly better weather/type fit.
    // Casual and cozy pieces are neutral fillers that work across looks.
    private int styleScore(String vibe, OutfitStyle style) {
        if (style == null || style == OutfitStyle.ANY || vibe == null) {
            return 0;
        }

        if ("casual".equals(vibe)) {
            // Plain casual pieces blend into anything except the dressiest looks.
            return style == OutfitStyle.FORMAL ? -4 : 12;
        }
        if ("cozy".equals(vibe)) {
            return switch (style) {
                case STREETWEAR, SPORTY -> 8;
                default -> -10;
            };
        }

        Integer styleRank = stylePosition(style);
        Integer vibeRank = vibePosition(vibe);
        if (styleRank == null || vibeRank == null) {
            return 0;
        }

        // Distance 0/1 stay within VARIETY_MARGIN of each other so the chosen style and
        // its neighbour can be mixed and rotated. Distance 2/3 are pushed well past the
        // margin so a clashing piece only appears when nothing on-style is available.
        return switch (Math.abs(styleRank - vibeRank)) {
            case 0 -> 70;
            case 1 -> 35;
            case 2 -> -40;
            default -> -85;
        };
    }

    // Position of each style on the formal - shine - streetwear - sporty spectrum.
    private Integer stylePosition(OutfitStyle style) {
        return switch (style) {
            case FORMAL -> 0;
            case SHINE -> 1;
            case STREETWEAR -> 2;
            case SPORTY -> 3;
            default -> null;
        };
    }

    // Position of a clothing vibe on the same spectrum, so it can be compared to a style.
    private Integer vibePosition(String vibe) {
        return switch (vibe) {
            case "elegant" -> 0;
            case "shine" -> 1;
            case "streetwear" -> 2;
            case "sporty" -> 3;
            default -> null;
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

    private record Conditions(boolean raining, boolean windy) {
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
