package com.example.zenpath;

public class GameRecommender {

    public enum GameType { STAR_SWEEP, LANTERN_RELEASE, PLANET }

    public static class Recommendation {
        public final GameType game;
        public final String title;
        public final String message;

        public Recommendation(GameType game, String title, String message) {
            this.game = game;
            this.title = title;
            this.message = message;
        }
    }

    public static Recommendation recommend(int mood, int stress, String lastGame) {
        float s = clamp(stress, 0, 100) / 100f;      // 0..1
        float m = (clamp(mood, 1, 5) - 1) / 4f;       // 0..1

        float star = 0f, lantern = 0f, planet = 0f;

        // ⭐ Star Sweep = grounding, best for high stress
        star += 1.2f * s;
        star += 0.2f * (1f - m);

        // 🏮 Lantern = emotional release, best for low mood + some stress
        lantern += 1.0f * (1f - m);
        lantern += 0.6f * s;

        // 🪐 Planet = creative, best for low stress + okay mood
        planet += 1.0f * (1f - s);
        planet += 0.6f * m;

        // Avoid repeating last played
        if (lastGame != null) {
            if (lastGame.equals("STAR_SWEEP")) star -= 0.25f;
            if (lastGame.equals("LANTERN_RELEASE")) lantern -= 0.25f;
            if (lastGame.equals("PLANET")) planet -= 0.25f;
        }

        GameType best = GameType.STAR_SWEEP;
        float bestScore = star;
        if (lantern > bestScore) { best = GameType.LANTERN_RELEASE; bestScore = lantern; }
        if (planet > bestScore) { best = GameType.PLANET; }

        switch (best) {
            case STAR_SWEEP:
                return new Recommendation(
                        GameType.STAR_SWEEP,
                        "Try Star Sweep ⭐",
                        "Feeling overwhelmed? Let’s slow down. Stargaze a bit and spot constellations—just a calm reset."
                );
            case LANTERN_RELEASE:
                return new Recommendation(
                        GameType.LANTERN_RELEASE,
                        "Lantern Release 🏮",
                        "Got stuff on your chest? Write it out, send it off, and let that weight leave with the lantern."
                );
            default:
                return new Recommendation(
                        GameType.PLANET,
                        "Planet Mode 🪐",
                        "In a creative mood? Build something fun and chill—no pressure, just vibes."
                );
        }
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}
