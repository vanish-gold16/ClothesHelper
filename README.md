# 👕 ClothesHelper

A JavaFX desktop application that manages your wardrobe and recommends weather-appropriate outfits using real-time weather data.

## Features

### 🌤️ Weather-Based Outfit Generation
- Fetches live weather from the [Open-Meteo API](https://open-meteo.com/) (no API key required)
- Displays current temperature and "feels like" temperature for Prague
- Generates layered outfit plans adapted to seven temperature ranges — from _light one-layer_ (≥ 22 °C) to _deep winter_ (< −6 °C)
- Takes rain and wind conditions into account when selecting layers
- Press **Regenerate** for a new combination; each slot rotates through your wardrobe using weighted randomisation

### 🎨 Outfit Styling Options
- **Pattern** — choose between _Random_ (weather-optimal picks) and _Sandwich_ (shoes echo the colour of the top)
- **Style** — filter by _Formal_, _Sporty_, _Streetwear_, _Shine_, or leave it on _Any_
- Outfits you like can be **saved** and viewed later on the Profile page

### 📚 Clothing Library
- Add items with detailed metadata: type, brand, size, seasons, main colour, wear occasions, vibe, and free-text notes
- Attach a photo to any item (supports PNG, JPG, WebP via TwelveMonkeys ImageIO)
- Browse all items in a visual card grid with photo previews or colour-coded placeholders
- **Sort** by newest, oldest, brand, name, colour, or vibe
- **Filter** by free-text search, brand, clothing type, colour, season, occasion, or vibe
- Edit or delete items in place

### 👤 Profile & Stats
- Shows your display name (pulled from the OS user)
- Wardrobe stats: total items, favourite colour (with swatch), favourite brand
- Lists all saved outfits with piece chips; rename or delete outfits

### ⚙️ Settings
- Switch between **Light** and **Dark** themes
- Theme preference is persisted across sessions via `java.util.prefs`

## Supported Clothing Types

T-shirt · Shirt · Blouse · Top · Sweater · Hoodie · Sweatshirt · Jacket · Coat · Blazer · Dress · Skirt · Jeans · Pants · Shorts · Leggings · Suit · Underwear · Socks · Shoes · Sneakers · Boots · Sandals · Hat · Scarf · Gloves · Bag · Belt · Accessory

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| UI framework | JavaFX 21 |
| Build tool | Maven (with Maven Wrapper) |
| Weather API | Open-Meteo Forecast API |
| Image format support | TwelveMonkeys ImageIO (WebP) |
| Testing | JUnit 5 |
| Persistence | Local JSON files (`wardrobe-memory/`) |

## Project Structure

```
ClothesHelper/
├── src/main/java/com/example/clotheshelper/
│   ├── ClothesHelperApplication.java      # Application entry point
│   ├── enums/                             # ClothingType, Seasons, Vibe, etc.
│   ├── outfit/                            # Outfit recommendation engine
│   ├── storage/                           # JSON-based persistence layer
│   ├── ui/
│   │   ├── AppRoot.java                   # Root layout with bottom navigation
│   │   ├── components/                    # Reusable UI components
│   │   ├── pages/                         # Home, Library, Add, Edit, Settings, Profile
│   │   ├── styles/                        # Shared style constants
│   │   └── theme/                         # Light/Dark theme definitions
│   └── weather/                           # Open-Meteo client & WeatherSnapshot
├── src/main/resources/                    # CSS stylesheets, assets
├── src/test/java/                         # Unit tests
├── wardrobe-memory/                       # Local wardrobe data (git-ignored)
│   ├── catalog.json
│   ├── outfits.json
│   └── items/                             # Per-item JSON + photos
└── pom.xml
```

## Prerequisites

- **Java 21** or later
- **Maven 3.9+** (or use the included Maven Wrapper)

## Getting Started

```bash
# Clone the repository
git clone https://github.com/vanish-gold16/ClothesHelper.git
cd ClothesHelper

# Build the project
./mvnw clean compile

# Run the application
./mvnw javafx:run
```

On Windows, use `mvnw.cmd` instead of `./mvnw`.

## Running Tests

```bash
./mvnw test
```

## How the Outfit Engine Works

1. The app fetches the current "feels like" temperature and weather conditions for Prague.
2. Based on the temperature, it selects a **layering plan** (e.g. _base layer → middle layer → outerwear → bottom → shoes_).
3. Each slot in the plan defines preferred and fallback clothing types.
4. Every item in your library is **scored** against each slot using:
   - Season compatibility (hot / warm / rainy / cozy / freezing)
   - Warmth suitability for the clothing type
   - Weather condition bonuses (rain-resistant layers, wind-blocking outerwear)
   - Outfit pattern bonuses (sandwich colour matching via Euclidean RGB distance)
   - Style affinity and vibe scoring
5. The highest-scoring item fills each slot. On **Regenerate**, weighted randomisation rotates through alternatives while keeping the best picks most likely.

## Data Storage

All wardrobe data is stored locally in the `wardrobe-memory/` directory as plain JSON files. No external database is required. This directory is git-ignored by default.
