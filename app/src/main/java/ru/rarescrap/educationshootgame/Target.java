package ru.rarescrap.educationshootgame;

public class Target extends GameElement {
    private int hitReward; // Прирост времени при попадании в мишень

    public Target(CannonView view, int color, int hitReward, int x, int y, int width, int length, float velocityY) {
        super(view, color, CannonView.TARGET_SOUND_ID, x, y, width, length, velocityY);
        this.hitReward = hitReward;
    }

    // Возвращает прирост при попадании в мишень
    public int getHitReward() {
        return hitReward;
    }
}
