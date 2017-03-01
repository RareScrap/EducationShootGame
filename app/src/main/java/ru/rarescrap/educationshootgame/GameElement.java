// GameElement.java
// Представляет прямоугольный игровой элемент
package ru.rarescrap.educationshootgame;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import ru.rarescrap.educationshootgame.CannonView;

public class GameElement {
    protected CannonView view; // Представление, содержащее GameElement
    protected Paint paint = new Paint(); // Объект Paint для рисования
    protected Rect shape; // Ограничивающий прямоугольник GameElement
    private float velocityY; // Вертикальная скорость GameElement
    private int soundId; // Звук, связанный с GameElement

    // Открытый конструктор
    public GameElement(CannonView view, int color, int soundId, int x, int y, int width, int length, float velocityY) {
        this.view = view;
        paint.setColor(color);
        shape = new Rect(x, y, x + width, y + length); // Определение границ
        this.soundId = soundId;
        this.velocityY = velocityY;
    }

    // Обновление позиции GameElement и проверка столкновений со стенами
    public void update(double interval) {
        // Обновление вертикальной позиции, путем добавления новых координат к старым координатам
        shape.offset(0, (int) (velocityY * interval));

        // Если GameElement сталкивается со стеной, изменить направление
        if ( shape.top < 0 && velocityY < 0 || shape.bottom > view.getScreenHeight() && velocityY > 0)
            velocityY *= -1; // Изменить скорость на противоположную
    }

    // Прорисовка GameElement на объекте Canvas
    public void draw(Canvas canvas) {
        canvas.drawRect(shape, paint);
    }

    // Воспроизведение звука, соответствующего типу GameElement
    public void playSound() {
        view.playSound(soundId);
    }
}