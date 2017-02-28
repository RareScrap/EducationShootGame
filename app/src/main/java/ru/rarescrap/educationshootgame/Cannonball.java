// Cannonball.java
// Класс представляет выпущенное ядро
package ru.rarescrap.educationshootgame;

import android.graphics.Canvas;
import android.graphics.Rect;

import ru.rarescrap.educationshootgame.GameElement;

public class Cannonball extends GameElement {
    private float velocityX;
    private boolean onScreen;

    // Конструктор
    public Cannonball(CannonView view, int color, int soundId, int x, int y, int radius, float velocityX, float velocityY) {
        super(view, color, soundId, x, y, 2 * radius, 2 * radius, velocityY);
        this.velocityX = velocityX;
        onScreen = true;
    }

    // Метод вычисляет радиус ядра как половину расстояния между границами shape.right и shape.left
    private int getRadius() {
        return (shape.right - shape.left) / 2;
    }

    // Метод проверяет, столкнулось ли ядро с объектом GameElement
    public boolean collidesWith(GameElement element) {
        return (Rect.intersects(shape, element.shape) && velocityX > 0);
    }

    // Метод возвращает true, если ядро находится на экране
    public boolean isOnScreen() {
        return onScreen;
    }

    // Метод инвертирует горизонтальную скорость ядра
    public void reverseVelocityX() {
        velocityX *= -1;
    }

    // Обновление позиции ядра
    @Override
    public void update(double interval) {
        super.update(interval); // Обновление вертикальной позиции ядра

        // Обновление горизонтальной позиции
        shape.offset((int) (velocityX * interval), 0);

        // Если ядро уходит за пределы экрана
        if (shape.top < 0 || shape.left < 0 ||
                shape.bottom > view.getScreenHeight() || shape.right > view.getScreenWidth())
            onScreen = false; // Убрать с экрана
    }

    // Рисование ядра на объекте Canvas
    @Override
    public void draw(Canvas canvas) {
        canvas.drawCircle(shape.left + getRadius(), shape.top + getRadius(), getRadius(), paint); // TODO: Сделть так, чтобы CannonBall логически больше не отрисоывался как прямоугольник
    }
}