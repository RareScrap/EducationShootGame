// Cannon.java
// Класс представляет пушку, стреляющую ядрами
package ru.rarescrap.educationshootgame;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;

public class Cannon {
    private int baseRadius; // Радиус основания
    private int barrelLength; // Длина ствола
    private Point barrelEnd = new Point(); // Конечная точка ствола
    private double barrelAngle; // Угол наклона ствола
    private Cannonball cannonball; // Объект Cannonball
    private Paint paint = new Paint(); // Объект Paint для рисования пушки
    private CannonView view; // Представление, в котором находится пушка

    // Конструктор
    public Cannon(CannonView view, int baseRadius, int barrelLength, int barrelWidth) {
        this.view = view;
        this.baseRadius = baseRadius;
        this.barrelLength = barrelLength;
        paint.setStrokeWidth(barrelWidth); // Назначение толщины ствола
        paint.setColor(Color.BLACK); // Пушка окрашена в черный цвет
        align(Math.PI / 2); // Ствол пушки обращен вправо
    }

    // Нацеливание пушки с заданным углом, передаваемым в радианах
    public void align(double barrelAngle) {
        this.barrelAngle = barrelAngle;
        barrelEnd.x = (int) (barrelLength * Math.sin(barrelAngle));
        barrelEnd.y = (int) (-barrelLength * Math.cos(barrelAngle)) + view.getScreenHeight() / 2; // TODO: ХЗ как это считается
    }

        // Метод создает ядро и стреляет в направлении ствола
        public void fireCannonball() {
        // Вычисление горизонтальной составляющей скорости ядра
        int velocityX = (int) (CannonView.CANNONBALL_SPEED_PERCENT *
                view.getScreenWidth() * Math.sin(barrelAngle)); // TODO: ХЗ как это считается

        // Вычисление вертикальной составляющей скорости ядра
        int velocityY = (int) (CannonView.CANNONBALL_SPEED_PERCENT *
                view.getScreenWidth() * -Math.cos(barrelAngle)); // TODO: ХЗ как это считается

        // Вычисление радиуса ядра
        int radius = (int) (view.getScreenHeight() * CannonView.CANNONBALL_RADIUS_PERCENT); // TODO: ХЗ как это считается

        // Построение ядра и размещение его в стволе
        cannonball = new Cannonball(view, Color.BLACK,
                CannonView.CANNON_SOUND_ID, -radius,
                view.getScreenHeight() / 2 - radius, radius, velocityX,
                velocityY);

        cannonball.playSound(); // Воспроизведение звука выстрела
        }

    // Рисование пушки на объекте Canvas
    public void draw(Canvas canvas) {
        // Рисование ствола пушки
        canvas.drawLine(0, view.getScreenHeight() / 2, barrelEnd.x, barrelEnd.y, paint);

        // Рисование основания пушки
        canvas.drawCircle(0, (int) view.getScreenHeight() / 2, (int) baseRadius, paint);
    }
    // Возвращает объект Cannonball, представляющий выпущенное ядро
    public Cannonball getCannonball() {
        return cannonball;
    }

    // Удаляет объект Cannonball из игры
    public void removeCannonball() {
        cannonball = null;
    }
}