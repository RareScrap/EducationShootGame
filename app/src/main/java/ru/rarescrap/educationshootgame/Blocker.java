package ru.rarescrap.educationshootgame;

public class Blocker extends GameElement {
    private int missPenalty; // Потеря времени при попадании в блок

    // Конструктор
    public Blocker(CannonView view, int color, int missPenalty, int x, int y, int width, int length, float velocityY) {
        super(view, color, CannonView.BLOCKER_SOUND_ID, x, y, width, length, velocityY);
        this.missPenalty = missPenalty;
    }

    // Возвращает штраф при попадании в блок
    public int getMissPenalty() {
        return missPenalty;
    }
}
